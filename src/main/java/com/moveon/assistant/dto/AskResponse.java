package com.moveon.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse {

    private String answer;
    private List<Citation> citations;
    private Integer hitCount;
    private Long responseTimeMs;
}
