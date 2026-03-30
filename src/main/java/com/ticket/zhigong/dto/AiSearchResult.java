package com.ticket.zhigong.dto;

public class AiSearchResult {

    private Long kbId;
    private String title;
    private String content;
    private double similarity;

    public AiSearchResult(Long kbId, String title, String content, double similarity) {
        this.kbId = kbId;
        this.title = title;
        this.content = content;
        this.similarity = similarity;
    }

    public Long getKbId() { return kbId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public double getSimilarity() { return similarity; }
}
