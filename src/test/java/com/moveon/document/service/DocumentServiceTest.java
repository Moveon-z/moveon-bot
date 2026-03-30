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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 文档服务测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MinioClient minioClient;

    @Mock
    private DocumentParsingService documentParsingService;

    @Mock
    private MultipartFile multipartFile;

    private DocumentService documentService;

    @BeforeEach
    void setUp() throws Exception {
        documentService = new DocumentService(documentRepository, minioClient, documentParsingService);
        ReflectionTestUtils.setField(documentService, "bucketName", "moveon-documents");

        // 默认 Mock 文件行为
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

        // 存储桶已存在
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    }

    @Test
    void uploadDocument_Success() throws Exception {
        // Mock 数据库保存
        Document savedDoc = Document.builder()
                .id(1L)
                .userId(1L)
                .fileName("test.txt")
                .originalFilename("test.txt")
                .fileType(DocumentType.TXT)
                .mimeType("text/plain")
                .fileSize(1024L)
                .storagePath("1/2026-03-27/1234-test.txt")
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

        Document result = documentService.uploadDocument(1L, multipartFile);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(DocumentType.TXT, result.getFileType());
        assertEquals(DocumentStatus.PENDING, result.getStatus());
        assertEquals(1024L, result.getFileSize());

        // 验证 MinIO 上传被调用
        verify(minioClient).putObject(any(PutObjectArgs.class));
        // 验证数据库保存被调用
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void uploadDocument_BucketNotExists_ShouldCreateBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        documentService.uploadDocument(1L, multipartFile);

        // 验证存储桶被创建
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void uploadDocument_ShouldSanitizeFilename() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("../../etc/passwd.txt");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        documentService.uploadDocument(1L, multipartFile);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());

        Document saved = docCaptor.getValue();
        // 文件名中不应包含路径穿越字符
        assertFalse(saved.getFileName().contains(".."));
        assertFalse(saved.getStoragePath().contains(".."));
        // fileName 应该是清理后的版本
        assertTrue(saved.getFileName().endsWith(".txt"));
    }

    @Test
    void uploadDocument_SpecialCharactersInFilename_ShouldBeSanitized() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("file;rm -rf /;.txt");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        documentService.uploadDocument(1L, multipartFile);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());

        Document saved = docCaptor.getValue();
        // 特殊字符应被替换为下划线
        assertFalse(saved.getFileName().contains(";"));
        assertFalse(saved.getFileName().contains("-"));
        assertTrue(saved.getFileName().endsWith(".txt"));
    }

    @Test
    void uploadDocument_DetermineDocumentType_Pdf() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("report.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        Document result = documentService.uploadDocument(1L, multipartFile);
        assertEquals(DocumentType.PDF, result.getFileType());
    }

    @Test
    void uploadDocument_DetermineDocumentType_Docx() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
        when(multipartFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(3L);
            return d;
        });

        Document result = documentService.uploadDocument(1L, multipartFile);
        assertEquals(DocumentType.DOCX, result.getFileType());
    }

    @Test
    void getDocumentById_Found() {
        Document doc = Document.builder()
                .id(1L).userId(1L).fileName("test.txt")
                .status(DocumentStatus.PENDING).build();
        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(doc));

        Document result = documentService.getDocumentById(1L);
        assertEquals("test.txt", result.getFileName());
    }

    @Test
    void getDocumentById_NotFound_ShouldThrow() {
        when(documentRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentById(999L));
    }

    @Test
    void getDocumentByIdAndUserId_WrongUser_ShouldThrow() {
        Document doc = Document.builder().id(1L).userId(1L).build();
        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(doc));

        assertThrows(BusinessException.class, () -> documentService.getDocumentByIdAndUserId(1L, 2L));
    }

    @Test
    void getDocumentByIdAndUserId_CorrectUser() {
        Document doc = Document.builder().id(1L).userId(1L).build();
        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(doc));

        Document result = documentService.getDocumentByIdAndUserId(1L, 1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void updateStatus_Parsed() {
        Document doc = Document.builder().id(1L).status(DocumentStatus.PENDING).build();
        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.updateStatus(1L, DocumentStatus.PARSED, null);
        assertEquals(DocumentStatus.PARSED, result.getStatus());
        assertNotNull(result.getParsedAt());
    }

    @Test
    void updateStatus_Failed() {
        Document doc = Document.builder().id(1L).status(DocumentStatus.PARSING).build();
        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.updateStatus(1L, DocumentStatus.FAILED, "Parse error");
        assertEquals(DocumentStatus.FAILED, result.getStatus());
        assertEquals("Parse error", result.getErrorMessage());
    }
}
