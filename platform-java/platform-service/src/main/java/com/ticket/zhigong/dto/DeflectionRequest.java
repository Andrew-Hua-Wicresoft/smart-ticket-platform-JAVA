package com.ticket.zhigong.dto;

public class DeflectionRequest {

    private Long matchedKbId;
    private String searchQuery;

    public Long getMatchedKbId() { return matchedKbId; }
    public void setMatchedKbId(Long matchedKbId) { this.matchedKbId = matchedKbId; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
}
