package com.moveon.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题长度不能超过1000个字符")
    private String question;

    private Integer topK;
}
