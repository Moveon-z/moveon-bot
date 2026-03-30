package com.moveon.document.service;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentStatus;
import com.moveon.document.entity.DocumentType;
import com.moveon.document.repository.DocumentRepository;
import com.moveon.infra.exception.BusinessException;
import com.moveon.infra.exception.ResourceNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;
    private final DocumentParsingService documentParsingService;

    @Value("${minio.bucket:moveon-documents}")
    private String bucketName;

    /**
     * 上传文档
     *
     * @param userId   用户 ID
     * @param file     上传的文件
     * @return 文档实体
     */
    @Transactional
    public Document uploadDocument(Long userId, MultipartFile file) {
        try {
            // 确保存储桶存在
            ensureBucketExists();

            // 清理文件名，防止路径穿越
            String safeFilename = sanitizeFilename(file.getOriginalFilename());

            // 生成存储路径：{userId}/{date}/{timestamp}-{safeFilename}
            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String storagePath = userId + "/" + dateStr + "/" + System.currentTimeMillis() + "-" + safeFilename;

            // 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storagePath)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            log.info("File uploaded to MinIO: bucket={}, path={}", bucketName, storagePath);

            // 确定文件类型
            DocumentType documentType = determineDocumentType(safeFilename, file.getContentType());

            // 创建文档元数据
            Document document = Document.builder()
                    .userId(userId)
                    .fileName(safeFilename)
                    .originalFilename(file.getOriginalFilename())
                    .fileType(documentType)
                    .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .status(DocumentStatus.PENDING)
                    .build();

            return documentRepository.save(document);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload document for user {}: {}", userId, e.getMessage(), e);
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传文档并触发异步解析
     */
    @Transactional
    public Document uploadAndParseDocument(Long userId, MultipartFile file) {
        Document document = uploadDocument(userId, file);
        documentParsingService.parseDocumentAsync(document.getId());
        return document;
    }

    /**
     * 清理文件名，防止路径穿越攻击
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        // 只保留文件名部分（去掉路径）
        String name = filename.substring(filename.lastIndexOf('/') + 1);
        name = name.substring(name.lastIndexOf('\\') + 1);
        // 移除路径穿越字符
        name = name.replaceAll("\\.\\.", "");
        // 只保留安全字符：字母、数字、中文、点、短横线、下划线
        name = name.replaceAll("[^\\w\\.\\-\\u4e00-\\u9fff]", "_");
        if (name.isEmpty()) {
            return "unnamed";
        }
        return name;
    }

    /**
     * 根据文件扩展名和 MIME 类型判断文档类型
     */
    private DocumentType determineDocumentType(String filename, String mimeType) {
        if (filename == null) {
            return DocumentType.OTHER;
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".txt")) {
            return DocumentType.TXT;
        } else if (lowerFilename.endsWith(".pdf")) {
            return DocumentType.PDF;
        } else if (lowerFilename.endsWith(".docx")) {
            return DocumentType.DOCX;
        } else if (lowerFilename.endsWith(".xlsx")) {
            return DocumentType.XLSX;
        } else if (lowerFilename.endsWith(".pptx")) {
            return DocumentType.PPTX;
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg") ||
                lowerFilename.endsWith(".png") || lowerFilename.endsWith(".gif")) {
            return DocumentType.IMAGE;
        }

        // 根据 MIME 类型判断
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return DocumentType.IMAGE;
            }
        }

        return DocumentType.OTHER;
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Bucket created: {}", bucketName);
        }
    }

    /**
     * 根据 ID 获取文档
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    /**
     * 根据 ID 和用户 ID 获取文档（权限检查）
     */
    public Document getDocumentByIdAndUserId(Long id, Long userId) {
        Document document = getDocumentById(id);
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "无权访问该文档");
        }
        return document;
    }

    /**
     * 更新文档状态
     */
    public Document updateStatus(Long documentId, DocumentStatus status, String errorMessage) {
        Document document = getDocumentById(documentId);
        document.setStatus(status);
        document.setErrorMessage(errorMessage);

        if (status == DocumentStatus.PARSED) {
            document.setParsedAt(LocalDateTime.now());
        } else if (status == DocumentStatus.COMPLETED) {
            document.setEmbeddedAt(LocalDateTime.now());
        }

        return documentRepository.save(document);
    }

    /**
     * 分页查询用户文档列表
     */
    public Page<Document> getDocumentsByUserId(Long userId, Pageable pageable) {
        return documentRepository.findByUserId(userId, pageable);
    }

    /**
     * 分页查询用户指定状态的文档
     */
    public Page<Document> getDocumentsByUserIdAndStatus(Long userId, String status, Pageable pageable) {
        try {
            DocumentStatus documentStatus = DocumentStatus.valueOf(status.toUpperCase());
            return documentRepository.findByUserIdAndStatus(userId, documentStatus, pageable);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "无效的文档状态：" + status);
        }
    }

    /**
     * 获取文档片段列表
     */
    public java.util.List<com.moveon.document.entity.DocumentFragment> getDocumentFragments(Long documentId, Long userId) {
        // 权限校验
        getDocumentByIdAndUserId(documentId, userId);
        return documentParsingService.getFragmentsByDocumentId(documentId);
    }
}
