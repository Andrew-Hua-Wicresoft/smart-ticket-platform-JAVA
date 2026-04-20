package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 200, message = "标题长度必须在5-200字之间")
    private String title;

    @NotBlank(message = "描述不能为空")
    @Size(min = 10, max = 5000, message = "描述长度必须在10-5000字之间")
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
