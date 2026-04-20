package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketCommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论内容不能超过5000字")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
