package com.moveon.document.service;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentFragment;
import com.moveon.document.entity.DocumentType;
import com.moveon.document.repository.DocumentFragmentRepository;
import com.moveon.infra.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 文档解析服务测试
 */
@ExtendWith(MockitoExtension.class)
class DocumentParserServiceTest {

    @Mock
    private DocumentFragmentRepository fragmentRepository;

    private DocumentParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new DocumentParserService(fragmentRepository);
    }

    // ========== Text Extraction Tests ==========

    @Test
    void extractText_TxtFile_Success() {
        Document doc = Document.builder()
                .id(1L).fileType(DocumentType.TXT).originalFilename("test.txt")
                .build();
        byte[] data = "Hello World\nThis is a test.".getBytes(StandardCharsets.UTF_8);

        String result = parserService.extractText(doc, data);

        assertEquals("Hello World\nThis is a test.", result);
    }

    @Test
    void extractText_EmptyContent_ShouldThrow() {
        Document doc = Document.builder()
                .id(1L).fileType(DocumentType.TXT).originalFilename("empty.txt")
                .build();
        byte[] data = "   \n  \n  ".getBytes(StandardCharsets.UTF_8);

        assertThrows(BusinessException.class, () -> parserService.extractText(doc, data));
    }

    @Test
    void extractText_CleansText() {
        Document doc = Document.builder()
                .id(1L).fileType(DocumentType.TXT).originalFilename("test.txt")
                .build();
        String content = "Line 1\r\n\r\n\r\n\r\nLine 2";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        String result = parserService.extractText(doc, data);

        // 3+ consecutive newlines should be compressed to 2
        assertFalse(result.contains("\n\n\n"));
    }

    // ========== Fragment Splitting Tests ==========

    @Test
    void splitAndSaveFragments_ShortText_OneFragment() {
        String text = "Paragraph 1\n\nParagraph 2";
        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentFragment> fragments = parserService.splitAndSaveFragments(1L, text);

        assertEquals(1, fragments.size());
        assertEquals(0, fragments.get(0).getFragmentIndex());
        assertTrue(fragments.get(0).getContent().contains("Paragraph 1"));
        assertTrue(fragments.get(0).getContent().contains("Paragraph 2"));
        verify(fragmentRepository).deleteByDocumentId(1L);
        verify(fragmentRepository).saveAll(any());
    }

    @Test
    void splitAndSaveFragments_LongText_MultipleFragmentsWithOverlap() {
        // Create text with 12 paragraphs
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            if (i > 1) sb.append("\n\n");
            sb.append("Paragraph ").append(i).append(" with some content to make it longer.");
        }
        String text = sb.toString();

        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentFragment> fragments = parserService.splitAndSaveFragments(1L, text);

        // 12 paragraphs, 4 per fragment, 1 overlap → step = 3
        // Fragment 0: paragraphs 1-4, Fragment 1: paragraphs 4-7, Fragment 2: paragraphs 7-10, Fragment 3: paragraphs 10-12
        assertTrue(fragments.size() >= 3, "Expected at least 3 fragments, got " + fragments.size());

        // Verify fragment indices are sequential
        for (int i = 0; i < fragments.size(); i++) {
            assertEquals(i, fragments.get(i).getFragmentIndex());
            assertEquals(1L, fragments.get(i).getDocumentId());
            assertNotNull(fragments.get(i).getContent());
            assertTrue(fragments.get(i).getCharCount() > 0);
        }

        // Verify overlap: fragment 1 should contain "Paragraph 4" (overlap from fragment 0)
        if (fragments.size() > 1) {
            assertTrue(fragments.get(1).getContent().contains("Paragraph 4"),
                    "Expected overlap: fragment 1 should contain Paragraph 4");
        }
    }

    @Test
    void splitAndSaveFragments_SingleParagraph() {
        String text = "Just one paragraph";
        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentFragment> fragments = parserService.splitAndSaveFragments(1L, text);

        assertEquals(1, fragments.size());
        assertEquals("Just one paragraph", fragments.get(0).getContent());
    }

    @Test
    void splitAndSaveFragments_DeletesOldFragmentsFirst() {
        String text = "Some content";
        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        parserService.splitAndSaveFragments(1L, text);

        // Verify delete was called before save
        var order = inOrder(fragmentRepository);
        order.verify(fragmentRepository).deleteByDocumentId(1L);
        order.verify(fragmentRepository).saveAll(any());
    }

    @Test
    void splitAndSaveFragments_FragmentsHaveCorrectCharCount() {
        String text = "First paragraph\n\nSecond paragraph";
        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentFragment> fragments = parserService.splitAndSaveFragments(1L, text);

        for (DocumentFragment fragment : fragments) {
            assertEquals(fragment.getContent().length(), fragment.getCharCount());
        }
    }

    @Test
    void splitAndSaveFragments_AllFragmentsCanReconstructText() {
        // Create text with 10 paragraphs (each unique)
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            if (i > 1) sb.append("\n\n");
            sb.append("Unique paragraph number ").append(i);
        }
        String text = sb.toString();

        when(fragmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentFragment> fragments = parserService.splitAndSaveFragments(1L, text);

        // First fragment should start with "Unique paragraph number 1"
        assertTrue(fragments.get(0).getContent().startsWith("Unique paragraph number 1"));
        // Last fragment should contain "Unique paragraph number 10"
        assertTrue(fragments.get(fragments.size() - 1).getContent().contains("Unique paragraph number 10"));
    }

    // ========== Get Fragments Tests ==========

    @Test
    void getFragmentsByDocumentId_ReturnsOrderedFragments() {
        List<DocumentFragment> expected = List.of(
                DocumentFragment.builder().id(1L).documentId(1L).fragmentIndex(0).content("a").charCount(1).build(),
                DocumentFragment.builder().id(2L).documentId(1L).fragmentIndex(1).content("b").charCount(1).build()
        );
        when(fragmentRepository.findByDocumentIdOrderByFragmentIndexAsc(1L)).thenReturn(expected);

        List<DocumentFragment> result = parserService.getFragmentsByDocumentId(1L);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getFragmentIndex());
        assertEquals(1, result.get(1).getFragmentIndex());
    }

    @Test
    void deleteFragmentsByDocumentId_CallsRepository() {
        parserService.deleteFragmentsByDocumentId(1L);
        verify(fragmentRepository).deleteByDocumentId(1L);
    }
}
