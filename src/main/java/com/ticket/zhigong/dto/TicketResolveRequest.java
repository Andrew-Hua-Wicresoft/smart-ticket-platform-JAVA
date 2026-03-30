package com.ticket.zhigong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketResolveRequest {

    @NotBlank(message = "解决方案说明不能为空")
    @Size(min = 10, max = 10000, message = "解决方案说明长度必须在10-10000字之间")
    private String resolutionNotes;

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
