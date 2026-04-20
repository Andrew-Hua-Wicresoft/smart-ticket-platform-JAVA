package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KbArticleUpdateRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "标题最长200字")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    @Size(max = 50000, message = "内容最长50000字")
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
