package com.moveon.document.controller;

import com.moveon.document.dto.DocumentFragmentResponse;
import com.moveon.document.dto.DocumentResponse;
import com.moveon.document.dto.DocumentUploadResponse;
import com.moveon.document.dto.SearchResult;
import com.moveon.document.entity.Document;
import com.moveon.document.service.DocumentParsingService;
import com.moveon.document.service.DocumentService;
import com.moveon.document.service.SemanticSearchService;
import com.moveon.infra.dto.ApiResponse;
import com.moveon.infra.dto.PageResponse;
import com.moveon.infra.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "文档管理", description = "文档上传、查询和管理接口")
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private final DocumentService documentService;
    private final DocumentParsingService documentParsingService;
    private final com.moveon.document.service.DocumentEmbeddingService documentEmbeddingService;

    private final SemanticSearchService semanticSearchService;

    /**
     * 上传单个文档
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传单个文档文件，支持 TXT、PDF、DOCX 格式，最大 50MB")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "上传的文件")
            @RequestParam("file") MultipartFile file) {

        validateFile(file);

        Document document = documentService.uploadAndParseDocument(user.getId(), file);

        DocumentUploadResponse response = DocumentUploadResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType().name())
                .fileSize(document.getFileSize())
                .status(document.getStatus().name())
                .message("文档上传成功，等待解析")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取文档列表
     */
    @GetMapping
    @Operation(summary = "获取文档列表", description = "获取当前用户的文档列表，支持分页和状态筛选")
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> getDocumentList(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "页码（从 0 开始）")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "排序方向")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "文档状态筛选")
            @RequestParam(required = false) String status) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Document> documentPage;
        if (status != null && !status.isEmpty()) {
            documentPage = documentService.getDocumentsByUserIdAndStatus(user.getId(), status, pageable);
        } else {
            documentPage = documentService.getDocumentsByUserId(user.getId(), pageable);
        }

        List<DocumentResponse> content = documentPage.getContent().stream()
                .map(this::toDocumentResponse)
                .toList();

        PageResponse<DocumentResponse> pageResponse = PageResponse.of(
                content,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据文档 ID 获取文档详细信息")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "文档 ID")
            @PathVariable("id") Long id) {

        Document document = documentService.getDocumentByIdAndUserId(id, user.getId());

        DocumentResponse response = toDocumentResponse(document);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 重新解析文档（手动触发，用于解析失败后重试）
     */
    @PostMapping("/{id}/parse")
    @Operation(summary = "重新解析文档", description = "手动触发文档解析，用于解析失败后重试")
    public ResponseEntity<ApiResponse<String>> reparseDocument(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "文档 ID")
            @PathVariable("id") Long id) {

        Document document = documentService.getDocumentByIdAndUserId(id, user.getId());
        documentParsingService.parseDocumentAsync(document.getId());

        return ResponseEntity.ok(ApiResponse.success("文档解析任务已提交"));
    }

    /**
     * 获取文档片段列表
     */
    @GetMapping("/{id}/fragments")
    @Operation(summary = "获取文档片段", description = "获取文档解析后的片段列表")
    public ResponseEntity<ApiResponse<List<DocumentFragmentResponse>>> getDocumentFragments(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "文档 ID")
            @PathVariable("id") Long id) {

        List<com.moveon.document.entity.DocumentFragment> fragments =
                documentService.getDocumentFragments(id, user.getId());

        List<DocumentFragmentResponse> responses = fragments.stream()
                .map(f -> DocumentFragmentResponse.builder()
                        .id(f.getId())
                        .documentId(f.getDocumentId())
                        .fragmentIndex(f.getFragmentIndex())
                        .content(f.getContent())
                        .charCount(f.getCharCount())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 手动触发向量化（用于向量化失败后重试）
     */
    @PostMapping("/{id}/embed")
    @Operation(summary = "手动向量化文档", description = "手动触发文档向量化，用于向量化失败后重试")
    public ResponseEntity<ApiResponse<String>> embedDocument(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "文档 ID")
            @PathVariable("id") Long id) {

        Document document = documentService.getDocumentByIdAndUserId(id, user.getId());
        documentEmbeddingService.embedDocumentAsync(document.getId());

        return ResponseEntity.ok(ApiResponse.success("向量化任务已提交"));
    }

    /**
     * 语义检索文档片段
     */
    @GetMapping("/search")
    @Operation(summary = "语义检索", description = "基于自然语言查询，检索最相关的文档片段")
    public ResponseEntity<ApiResponse<List<SearchResult>>> searchDocuments(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Parameter(description = "查询文本")
            @RequestParam("query") String query,
            @Parameter(description = "返回结果数量")
            @RequestParam(defaultValue = "5") int topK) {

        List<SearchResult> results = semanticSearchService.search(query, user.getId(), topK);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 文件校验
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_SIZE_EXCEEDED", String.format("文件大小超过限制（最大 %dMB）", MAX_FILE_SIZE / 1024 / 1024));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("INVALID_FILE", "无法识别文件类型");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE", String.format("不支持的文件类型：%s，仅支持：%s",
                    extension, String.join(", ", ALLOWED_EXTENSIONS)));
        }

        String contentType = file.getContentType();
        if (contentType != null && !isValidMimeType(contentType)) {
            log.warn("文件 MIME 类型可能不匹配：{}", contentType);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private boolean isValidMimeType(String contentType) {
        String lower = contentType.toLowerCase();
        return lower.contains("text") ||
                lower.contains("pdf") ||
                lower.contains("word") ||
                lower.contains("officedocument") ||
                lower.contains("octet-stream");
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFilename(document.getOriginalFilename())
                .fileType(document.getFileType().name())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .status(document.getStatus().name())
                .summary(document.getSummary())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .parsedAt(document.getParsedAt())
                .embeddedAt(document.getEmbeddedAt())
                .build();
    }
}
