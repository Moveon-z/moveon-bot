package com.moveon.assistant.service;

import com.moveon.assistant.config.RagConfig;
import com.moveon.assistant.dto.AskResponse;
import com.moveon.assistant.dto.Citation;
import com.moveon.assistant.entity.QaLog;
import com.moveon.assistant.repository.QaLogRepository;
import com.moveon.document.dto.SearchResult;
import com.moveon.document.service.SemanticSearchService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private QaLogRepository qaLogRepository;

    @Mock
    private OpenAiChatModel chatModel;

    @Mock
    private RagConfig ragConfig;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        lenient().when(ragConfig.getDefaultTopK()).thenReturn(5);
        lenient().when(ragConfig.getMaxTopK()).thenReturn(20);
        lenient().when(ragConfig.getMaxQuestionLength()).thenReturn(1000);
        lenient().when(ragConfig.getMaxAnswerLength()).thenReturn(4000);
        lenient().when(ragConfig.getMinScoreThreshold()).thenReturn(0.3);

        ragService = new RagService(semanticSearchService, qaLogRepository, ragConfig);
        ReflectionTestUtils.setField(ragService, "chatModel", chatModel);
    }

    @Test
    void ask_withResults_returnsAnswerWithCitations() {
        List<SearchResult> results = List.of(
                SearchResult.builder().fragmentId(1L).documentId(10L).fileName("doc1.pdf")
                        .fragmentIndex(0).content("Test content 1").score(0.85).build(),
                SearchResult.builder().fragmentId(2L).documentId(10L).fileName("doc1.pdf")
                        .fragmentIndex(1).content("Test content 2").score(0.72).build(),
                SearchResult.builder().fragmentId(3L).documentId(11L).fileName("doc2.txt")
                        .fragmentIndex(0).content("Test content 3").score(0.65).build()
        );

        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(results);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("这是基于文档的答案。")));

        AskResponse response = ragService.ask(1L, "测试问题", 5);

        assertNotNull(response);
        assertEquals("这是基于文档的答案。", response.getAnswer());
        assertEquals(3, response.getCitations().size());
        assertEquals(3, response.getHitCount());
        assertNotNull(response.getResponseTimeMs());
        assertTrue(response.getResponseTimeMs() >= 0);

        Citation firstCitation = response.getCitations().get(0);
        assertEquals(10L, firstCitation.getDocumentId());
        assertEquals("doc1.pdf", firstCitation.getFileName());
        assertEquals(0.85, firstCitation.getScore());

        verify(qaLogRepository).save(any(QaLog.class));
    }

    @Test
    void ask_withNoResults_callsModelWithOwnKnowledge() {
        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(List.of());
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("这是模型自身知识的回答。")));

        AskResponse response = ragService.ask(1L, "无关问题", 5);

        assertNotNull(response);
        assertFalse(response.getAnswer().isEmpty());
        assertTrue(response.getCitations().isEmpty());
        assertEquals(0, response.getHitCount());

        verify(chatModel).generate(anyList());
        ArgumentCaptor<QaLog> logCaptor = ArgumentCaptor.forClass(QaLog.class);
        verify(qaLogRepository).save(logCaptor.capture());
        assertEquals("SUCCESS_NO_CONTEXT", logCaptor.getValue().getStatus());
    }

    @Test
    void ask_withLowScoreResults_callsModelWithOwnKnowledge() {
        List<SearchResult> lowScoreResults = List.of(
                SearchResult.builder().fragmentId(1L).documentId(10L).fileName("doc.pdf")
                        .fragmentIndex(0).content("Irrelevant content").score(0.1).build()
        );

        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(lowScoreResults);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("模型自身知识的回答")));

        AskResponse response = ragService.ask(1L, "不相关问题", 5);

        assertNotNull(response);
        assertFalse(response.getAnswer().isEmpty());
        assertTrue(response.getCitations().isEmpty());
        assertEquals(0, response.getHitCount());
        verify(chatModel).generate(anyList());
    }

    @Test
    void ask_chatModelFails_savesFailedLogAndThrows() {
        List<SearchResult> results = List.of(
                SearchResult.builder().fragmentId(1L).documentId(10L).fileName("doc.pdf")
                        .fragmentIndex(0).content("Content").score(0.85).build()
        );

        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(results);
        when(chatModel.generate(anyList())).thenThrow(new RuntimeException("API timeout"));

        assertThrows(com.moveon.infra.exception.BusinessException.class,
                () -> ragService.ask(1L, "测试", 5));

        ArgumentCaptor<QaLog> logCaptor = ArgumentCaptor.forClass(QaLog.class);
        verify(qaLogRepository).save(logCaptor.capture());
        assertEquals("FAILED", logCaptor.getValue().getStatus());
        assertTrue(logCaptor.getValue().getErrorMessage().contains("API timeout"));
    }

    @Test
    void ask_savesAuditLogCorrectly() {
        List<SearchResult> results = List.of(
                SearchResult.builder().fragmentId(1L).documentId(10L).fileName("a.pdf")
                        .fragmentIndex(0).content("content1").score(0.9).build(),
                SearchResult.builder().fragmentId(2L).documentId(11L).fileName("b.txt")
                        .fragmentIndex(1).content("content2").score(0.8).build()
        );

        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(results);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("答案")));

        ragService.ask(42L, "测试问题", null);

        ArgumentCaptor<QaLog> logCaptor = ArgumentCaptor.forClass(QaLog.class);
        verify(qaLogRepository).save(logCaptor.capture());

        QaLog saved = logCaptor.getValue();
        assertEquals(42L, saved.getUserId());
        assertEquals("测试问题", saved.getQuestion());
        assertEquals("答案", saved.getAnswer());
        assertEquals(2, saved.getHitCount());
        assertEquals("10,11", saved.getHitDocumentIds());
        assertEquals("1,2", saved.getHitFragmentIds());
        assertEquals(0.9, saved.getTopScore());
        assertEquals("SUCCESS", saved.getStatus());
        assertFalse(saved.getAnswerTruncated());
    }

    @Test
    void ask_truncatesLongAnswer() {
        String longAnswer = "a".repeat(5000);
        List<SearchResult> results = List.of(
                SearchResult.builder().fragmentId(1L).documentId(10L).fileName("doc.pdf")
                        .fragmentIndex(0).content("content").score(0.9).build()
        );

        when(semanticSearchService.search(anyString(), anyLong(), anyInt())).thenReturn(results);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(longAnswer)));

        AskResponse response = ragService.ask(1L, "question", 5);
        assertEquals(longAnswer, response.getAnswer());

        ArgumentCaptor<QaLog> logCaptor = ArgumentCaptor.forClass(QaLog.class);
        verify(qaLogRepository).save(logCaptor.capture());
        assertTrue(logCaptor.getValue().getAnswerTruncated());
        assertEquals(4000, logCaptor.getValue().getAnswer().length());
    }

    @Test
    void ask_defaultTopK_usedWhenNull() {
        when(semanticSearchService.search(anyString(), anyLong(), eq(5))).thenReturn(List.of());
        when(ragConfig.getDefaultTopK()).thenReturn(5);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("回答")));

        ragService.ask(1L, "question", null);

        verify(semanticSearchService).search(anyString(), anyLong(), eq(5));
    }

    @Test
    void ask_topKCappedAtMax() {
        when(ragConfig.getMaxTopK()).thenReturn(20);
        when(semanticSearchService.search(anyString(), anyLong(), eq(20))).thenReturn(List.of());
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("回答")));

        ragService.ask(1L, "question", 100);

        verify(semanticSearchService).search(anyString(), anyLong(), eq(20));
    }

    @Test
    void ask_chatModelNull_throwsBusinessException() {
        ReflectionTestUtils.setField(ragService, "chatModel", null);

        assertThrows(com.moveon.infra.exception.BusinessException.class,
                () -> ragService.ask(1L, "测试", 5));
    }
}
