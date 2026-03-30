package com.moveon.assistant.service;

import com.moveon.assistant.config.RagConfig;
import com.moveon.assistant.dto.AskResponse;
import com.moveon.assistant.dto.Citation;
import com.moveon.assistant.entity.QaLog;
import com.moveon.assistant.repository.QaLogRepository;
import com.moveon.document.dto.SearchResult;
import com.moveon.document.service.SemanticSearchService;
import com.moveon.infra.exception.BusinessException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final SemanticSearchService semanticSearchService;
    private final QaLogRepository qaLogRepository;
    private final RagConfig ragConfig;

    @Autowired(required = false)
    private OpenAiChatModel chatModel;

    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个智能问答助手。请尽力给出准确、有用的回答。

            回答规则：
            1. 如果以下参考资料中有相关内容，优先基于参考资料回答，并注明来源编号（如[来源1]）。
            2. 如果参考资料与问题无关或不充分，你可以结合自身知识回答，但要明确说明"以下回答来自模型自身知识，非基于用户文档"。
            3. 如果参考资料中存在冲突信息，指出冲突并分别引用各来源。
            4. 不要编造不确定的信息。如果确实不知道，请诚实说明。

            参考资料：
            %s""";

    private static final String NO_CONTEXT_ANSWER = "根据现有文档资料，我无法回答这个问题，因为没有找到与您的问题相关的文档内容。";

    public AskResponse ask(Long userId, String question, Integer topK) {
        if (chatModel == null) {
            throw new BusinessException("CHAT_MODEL_UNAVAILABLE", "问答服务暂不可用，请检查 AI 配置");
        }

        int effectiveTopK = resolveTopK(topK);
        long startTime = System.currentTimeMillis();
        List<SearchResult> searchResults = List.of();

        try {
            // 1. Semantic search
            searchResults = semanticSearchService.search(question, userId, effectiveTopK);
            log.info("RAG search: userId={}, results={}", userId, searchResults.size());

            // 2. Filter by score threshold
            List<SearchResult> filteredResults = searchResults.stream()
                    .filter(r -> r.getScore() != null && r.getScore() >= ragConfig.getMinScoreThreshold())
                    .toList();

            if (filteredResults.isEmpty()) {
                // No relevant docs found - let the model answer using its own knowledge
                String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, "（未检索到相关文档资料）");

                Response<AiMessage> llmResponse = chatModel.generate(
                        List.of(SystemMessage.from(systemPrompt), UserMessage.from(question))
                );
                String answer = llmResponse.content().text();
                long elapsed = System.currentTimeMillis() - startTime;

                saveQaLog(userId, question, answer, searchResults, elapsed, "SUCCESS_NO_CONTEXT", null);
                return AskResponse.builder()
                        .answer(answer)
                        .citations(List.of())
                        .hitCount(0)
                        .responseTimeMs(elapsed)
                        .build();
            }

            // 3. Build prompt and call LLM
            String context = formatContext(filteredResults);
            String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);

            Response<AiMessage> llmResponse = chatModel.generate(
                    List.of(SystemMessage.from(systemPrompt), UserMessage.from(question))
            );
            String answer = llmResponse.content().text();
            long elapsed = System.currentTimeMillis() - startTime;

            // 4. Build citations
            List<Citation> citations = filteredResults.stream()
                    .map(r -> Citation.builder()
                            .documentId(r.getDocumentId())
                            .fileName(r.getFileName())
                            .fragmentIndex(r.getFragmentIndex())
                            .score(r.getScore())
                            .build())
                    .toList();

            // 5. Save audit log
            saveQaLog(userId, question, answer, filteredResults, elapsed, "SUCCESS", null);

            return AskResponse.builder()
                    .answer(answer)
                    .citations(citations)
                    .hitCount(filteredResults.size())
                    .responseTimeMs(elapsed)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("RAG ask failed: userId={}, error={}", userId, e.getMessage(), e);
            saveQaLog(userId, question, null, searchResults, elapsed, "FAILED", e.getMessage());
            throw new BusinessException("QA_FAILED", "问答请求处理失败：" + e.getMessage());
        }
    }

    /**
     * 流式问答：逐 token 推送给 SseEmitter
     *
     * @return SseEmitter 供 Controller 直接返回
     */
    public SseEmitter askStream(Long userId, String question, Integer topK) {
        if (streamingChatModel == null) {
            throw new BusinessException("CHAT_MODEL_UNAVAILABLE", "流式问答服务暂不可用，请检查 AI 配置");
        }

        int effectiveTopK = resolveTopK(topK);
        long startTime = System.currentTimeMillis();

        // 1. Semantic search (sync, fast)
        List<SearchResult> searchResults;
        try {
            searchResults = semanticSearchService.search(question, userId, effectiveTopK);
        } catch (Exception e) {
            throw new BusinessException("SEARCH_FAILED", "检索失败：" + e.getMessage());
        }

        // 2. Filter by threshold
        List<SearchResult> filteredResults = searchResults.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= ragConfig.getMinScoreThreshold())
                .toList();

        // 3. Build prompt
        String context = filteredResults.isEmpty()
                ? "（未检索到相关文档资料）"
                : formatContext(filteredResults);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);

        // 4. Create SseEmitter (5 min timeout)
        SseEmitter emitter = new SseEmitter(300_000L);

        // 5. Build citations ahead of time
        List<Citation> citations = filteredResults.stream()
                .map(r -> Citation.builder()
                        .documentId(r.getDocumentId())
                        .fileName(r.getFileName())
                        .fragmentIndex(r.getFragmentIndex())
                        .score(r.getScore())
                        .build())
                .toList();

        // 6. Stream LLM response
        StringBuilder fullAnswer = new StringBuilder();

        streamingChatModel.generate(
                List.of(SystemMessage.from(systemPrompt), UserMessage.from(question)),
                new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            fullAnswer.append(token);
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data("{\"content\":" + escapeJson(token) + "}"));
                        } catch (Exception e) {
                            log.warn("Failed to send SSE token: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            long elapsed = System.currentTimeMillis() - startTime;
                            String answer = fullAnswer.toString();

                            // Send done event with citations
                            String doneData = buildDoneEvent(citations, filteredResults.size(), elapsed);
                            emitter.send(SseEmitter.event().name("done").data(doneData));
                            emitter.complete();

                            // Save audit log
                            saveQaLog(userId, question, answer, filteredResults, elapsed,
                                    filteredResults.isEmpty() ? "SUCCESS_NO_CONTEXT" : "SUCCESS", null);
                        } catch (Exception e) {
                            log.warn("Failed to send SSE done: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            long elapsed = System.currentTimeMillis() - startTime;
                            log.error("RAG stream failed: userId={}, error={}", userId, error.getMessage(), error);

                            emitter.send(SseEmitter.event().name("error")
                                    .data("{\"message\":\"" + escapeJson(error.getMessage()) + "\"}"));
                            emitter.complete();

                            saveQaLog(userId, question, null, filteredResults, elapsed, "FAILED", error.getMessage());
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }
                }
        );

        return emitter;
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private String buildDoneEvent(List<Citation> citations, int hitCount, long elapsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"citations\":[");
        for (int i = 0; i < citations.size(); i++) {
            if (i > 0) sb.append(",");
            Citation c = citations.get(i);
            sb.append(String.format("{\"documentId\":%d,\"fileName\":\"%s\",\"fragmentIndex\":%d,\"score\":%.2f}",
                    c.getDocumentId(),
                    c.getFileName().replace("\"", "\\\""),
                    c.getFragmentIndex(),
                    c.getScore()));
        }
        sb.append("],\"hitCount\":").append(hitCount);
        sb.append(",\"responseTimeMs\":").append(elapsed);
        sb.append("}");
        return sb.toString();
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return ragConfig.getDefaultTopK();
        }
        return Math.min(topK, ragConfig.getMaxTopK());
    }

    private String formatContext(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            if (i > 0) sb.append("\n");
            sb.append(String.format("[来源 %d：文件 \"%s\"，片段 #%d，相似度=%.2f]\n",
                    i + 1, r.getFileName(), r.getFragmentIndex(), r.getScore()));
            sb.append(r.getContent());
            sb.append("\n---");
        }
        return sb.toString();
    }

    private void saveQaLog(Long userId, String question, String answer,
                           List<SearchResult> results, long responseTimeMs,
                           String status, String errorMessage) {
        try {
            String truncatedQuestion = question.length() > ragConfig.getMaxQuestionLength()
                    ? question.substring(0, ragConfig.getMaxQuestionLength())
                    : question;

            boolean answerTruncated = false;
            String truncatedAnswer = answer;
            if (answer != null && answer.length() > ragConfig.getMaxAnswerLength()) {
                truncatedAnswer = answer.substring(0, ragConfig.getMaxAnswerLength());
                answerTruncated = true;
            }

            String hitDocIds = results.stream()
                    .map(r -> String.valueOf(r.getDocumentId()))
                    .distinct()
                    .collect(Collectors.joining(","));

            String hitFragmentIds = results.stream()
                    .map(r -> String.valueOf(r.getFragmentId()))
                    .collect(Collectors.joining(","));

            Double topScore = results.stream()
                    .map(SearchResult::getScore)
                    .max(Double::compareTo)
                    .orElse(null);

            QaLog qaLog = QaLog.builder()
                    .userId(userId)
                    .question(truncatedQuestion)
                    .answer(truncatedAnswer)
                    .answerTruncated(answerTruncated)
                    .hitDocumentIds(hitDocIds.isEmpty() ? null : hitDocIds)
                    .hitFragmentIds(hitFragmentIds.isEmpty() ? null : hitFragmentIds)
                    .hitCount(results.size())
                    .topScore(topScore)
                    .responseTimeMs(responseTimeMs)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            qaLogRepository.save(qaLog);
        } catch (Exception e) {
            log.warn("Failed to save QA log: {}", e.getMessage());
        }
    }
}
