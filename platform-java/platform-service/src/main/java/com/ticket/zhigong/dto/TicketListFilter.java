package com.ticket.zhigong.dto;

import com.ticket.zhigong.enums.TicketAssigneeScope;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;

import java.util.List;

public class TicketListFilter {

    private final List<TicketStatus> statuses;
    private final List<TicketPriority> priorities;
    private final String keyword;
    private final TicketAssigneeScope assignee;

    public TicketListFilter(List<TicketStatus> statuses,
                            List<TicketPriority> priorities,
                            String keyword,
                            TicketAssigneeScope assignee) {
        this.statuses = statuses == null ? List.of() : List.copyOf(statuses);
        this.priorities = priorities == null ? List.of() : List.copyOf(priorities);
        this.keyword = keyword == null ? null : keyword.trim();
        this.assignee = assignee == null ? TicketAssigneeScope.ALL : assignee;
    }

    public static TicketListFilter empty() {
        return new TicketListFilter(List.of(), List.of(), null, TicketAssigneeScope.ALL);
    }

    public List<TicketStatus> getStatuses() { return statuses; }
    public List<TicketPriority> getPriorities() { return priorities; }
    public String getKeyword() { return keyword; }
    public TicketAssigneeScope getAssignee() { return assignee; }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
