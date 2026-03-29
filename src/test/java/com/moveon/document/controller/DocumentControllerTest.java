package com.moveon.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.document.dto.DocumentResponse;
import com.moveon.document.dto.DocumentUploadResponse;
import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentStatus;
import com.moveon.document.entity.DocumentType;
import com.moveon.document.service.DocumentService;
import com.moveon.document.service.DocumentParsingService;
import com.moveon.infra.dto.ApiResponse;
import com.moveon.infra.dto.PageResponse;
import com.moveon.infra.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文档控制器测试
 */
@WebMvcTest(
    controllers = DocumentController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.moveon.infra.config.SecurityConfig.class, com.moveon.auth.config.JwtAuthenticationFilter.class})
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentParsingService documentParsingService;

    @MockBean
    private com.moveon.document.service.DocumentEmbeddingService documentEmbeddingService;

    @MockBean
    private com.moveon.document.service.SemanticSearchService semanticSearchService;

    @Test
    void uploadDocument_Success() throws Exception {
        Document doc = Document.builder()
                .id(1L)
                .userId(1L)
                .fileName("test.txt")
                .originalFilename("test.txt")
                .fileType(DocumentType.TXT)
                .mimeType("text/plain")
                .fileSize(1024L)
                .storagePath("1/2026-03-27/123-test.txt")
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(documentService.uploadAndParseDocument(eq(1L), any())).thenReturn(doc);

        mockMvc.perform(multipart("/documents/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "test.txt", "text/plain", "test content".getBytes()))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                .andExpect(jsonPath("$.data.fileType").value("TXT"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void uploadDocument_UnsupportedFileType() throws Exception {
        mockMvc.perform(multipart("/documents/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "test.exe", "application/octet-stream", "test".getBytes()))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadDocument_EmptyFile() throws Exception {
        mockMvc.perform(multipart("/documents/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "", "text/plain", new byte[0]))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDocumentList_Success() throws Exception {
        Document doc = Document.builder()
                .id(1L).userId(1L).fileName("test.txt")
                .originalFilename("test.txt").fileType(DocumentType.TXT)
                .mimeType("text/plain").fileSize(1024L)
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        Page<Document> page = new PageImpl<>(List.of(doc));
        when(documentService.getDocumentsByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/documents")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].fileName").value("test.txt"));
    }

    @Test
    void getDocumentList_WithStatusFilter() throws Exception {
        Page<Document> page = new PageImpl<>(List.of());
        when(documentService.getDocumentsByUserIdAndStatus(eq(1L), eq("PARSED"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/documents")
                        .param("status", "PARSED")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk());

        verify(documentService).getDocumentsByUserIdAndStatus(eq(1L), eq("PARSED"), any(Pageable.class));
    }

    @Test
    void getDocument_Success() throws Exception {
        Document doc = Document.builder()
                .id(1L).userId(1L).fileName("test.txt")
                .originalFilename("test.txt").fileType(DocumentType.TXT)
                .mimeType("text/plain").fileSize(1024L)
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(documentService.getDocumentByIdAndUserId(eq(1L), eq(1L))).thenReturn(doc);

        mockMvc.perform(get("/documents/1")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"));
    }

    @Test
    void getDocument_NotFound() throws Exception {
        when(documentService.getDocumentByIdAndUserId(eq(999L), eq(1L)))
                .thenThrow(new com.moveon.infra.exception.ResourceNotFoundException("Document", "id", 999L));

        mockMvc.perform(get("/documents/999")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_AccessDenied_WrongUser() throws Exception {
        when(documentService.getDocumentByIdAndUserId(eq(1L), eq(2L)))
                .thenThrow(new BusinessException("ACCESS_DENIED", "无权访问该文档"));

        mockMvc.perform(get("/documents/1")
                        .requestAttr("user", createTestUser(2L)))
                .andExpect(status().isBadRequest());
    }

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }
}
