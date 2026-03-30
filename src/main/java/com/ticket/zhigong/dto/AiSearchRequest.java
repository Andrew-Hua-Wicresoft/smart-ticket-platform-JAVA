package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiSearchRequest {

    @NotBlank(message = "搜索内容不能为空")
    @Size(min = 2, max = 5000, message = "搜索内容长度必须在2-5000字之间")
    private String query;

    private int topK = 3;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
