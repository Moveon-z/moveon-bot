package com.moveon.document.service;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentFragment;
import com.moveon.document.entity.DocumentType;
import com.moveon.document.repository.DocumentFragmentRepository;
import com.moveon.infra.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务
 * 负责从文件中提取文本并切分为片段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParserService {

    private final DocumentFragmentRepository fragmentRepository;

    private final Tika tika = new Tika();

    /**
     * 每个片段包含的段落数（3-5 段，取 4）
     */
    private static final int PARAGRAPHS_PER_FRAGMENT = 4;

    /**
     * 相邻片段重叠的段落数（1-2 段，取 1）
     */
    private static final int OVERLAP_PARAGRAPHS = 1;

    /**
     * 从文件字节内容中提取文本
     *
     * @param document 文档元数据
     * @param fileData 文件字节内容
     * @return 提取的纯文本
     */
    public String extractText(Document document, byte[] fileData) {
        DocumentType type = document.getFileType();
        try {
            String text;
            if (type == DocumentType.TXT) {
                text = new String(fileData, StandardCharsets.UTF_8);
            } else {
                // 使用 Tika 统一处理 PDF、DOCX 等格式
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
                    text = tika.parseToString(inputStream);
                }
            }

            if (text == null || text.isBlank()) {
                throw new BusinessException("PARSE_EMPTY", "文档内容为空：" + document.getOriginalFilename());
            }

            // 清理提取的文本
            text = cleanText(text);
            log.info("Text extracted from document {}: {} chars", document.getId(), text.length());
            return text;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text from document {}: {}", document.getId(), e.getMessage(), e);
            throw new BusinessException("PARSE_FAILED", "文档解析失败：" + e.getMessage());
        }
    }

    /**
     * 将文本切分为片段并保存到数据库
     * 切分规则：每个片段 3-5 个段落，相邻片段重叠 1-2 个段落
     *
     * @param documentId 文档 ID
     * @param text       提取的文本
     * @return 保存的片段列表
     */
    @Transactional
    public List<DocumentFragment> splitAndSaveFragments(Long documentId, String text) {
        List<String> paragraphs = splitIntoParagraphs(text);
        List<DocumentFragment> fragments = createFragmentsWithOverlap(documentId, paragraphs);

        // 先清除旧的片段（支持重新解析）
        fragmentRepository.deleteByDocumentId(documentId);

        List<DocumentFragment> saved = fragmentRepository.saveAll(fragments);
        log.info("Document {} split into {} fragments (from {} paragraphs)",
                documentId, saved.size(), paragraphs.size());
        return saved;
    }

    /**
     * 获取文档的所有片段（按序号排序）
     */
    @Transactional(readOnly = true)
    public List<DocumentFragment> getFragmentsByDocumentId(Long documentId) {
        return fragmentRepository.findByDocumentIdOrderByFragmentIndexAsc(documentId);
    }

    /**
     * 删除文档的所有片段
     */
    @Transactional
    public void deleteFragmentsByDocumentId(Long documentId) {
        fragmentRepository.deleteByDocumentId(documentId);
    }

    /**
     * 清理提取的文本
     */
    private String cleanText(String text) {
        // 移除多余的空白行（保留段落分隔）
        text = text.replaceAll("\\r\\n", "\n");
        text = text.replaceAll("\\r", "\n");
        // 压缩连续 3 个以上换行为 2 个（段落分隔）
        text = text.replaceAll("\\n{3,}", "\n\n");
        // 去除首尾空白
        return text.trim();
    }

    /**
     * 将文本按段落分割
     */
    private List<String> splitIntoParagraphs(String text) {
        String[] rawParagraphs = text.split("\\n\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String p : rawParagraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }

        if (paragraphs.isEmpty()) {
            // 如果按双换行分不出段落，按单换行分
            String[] lines = text.split("\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    paragraphs.add(trimmed);
                }
            }
        }

        if (paragraphs.isEmpty()) {
            // 如果整个文本没有换行，作为一个段落
            paragraphs.add(text);
        }

        log.debug("Text split into {} paragraphs", paragraphs.size());
        return paragraphs;
    }

    /**
     * 创建带重叠的片段
     */
    private List<DocumentFragment> createFragmentsWithOverlap(Long documentId, List<String> paragraphs) {
        List<DocumentFragment> fragments = new ArrayList<>();

        if (paragraphs.size() <= PARAGRAPHS_PER_FRAGMENT) {
            // 文本较短，整个作为一个片段
            String content = String.join("\n\n", paragraphs);
            fragments.add(DocumentFragment.builder()
                    .documentId(documentId)
                    .fragmentIndex(0)
                    .content(content)
                    .charCount(content.length())
                    .build());
            return fragments;
        }

        int step = PARAGRAPHS_PER_FRAGMENT - OVERLAP_PARAGRAPHS;
        int index = 0;
        int start = 0;

        while (start < paragraphs.size()) {
            int end = Math.min(start + PARAGRAPHS_PER_FRAGMENT, paragraphs.size());
            List<String> fragmentParagraphs = paragraphs.subList(start, end);
            String content = String.join("\n\n", fragmentParagraphs);

            fragments.add(DocumentFragment.builder()
                    .documentId(documentId)
                    .fragmentIndex(index)
                    .content(content)
                    .charCount(content.length())
                    .build());

            index++;

            // 最后一个片段已经包含末尾所有段落，退出
            if (end >= paragraphs.size()) {
                break;
            }

            start += step;
        }

        return fragments;
    }
}
