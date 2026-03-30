package com.moveon.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {

    private Long documentId;
    private String fileName;
    private Integer fragmentIndex;
    private Double score;
}
