package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiRefineRequest {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "标题最长200字")
    private String title;

    @NotBlank(message = "工单描述不能为空")
    @Size(max = 5000, message = "描述最长5000字")
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
