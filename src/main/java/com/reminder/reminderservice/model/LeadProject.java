package com.reminder.reminderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadProject {
    private String leadProjectId;
    private String organization;
    private String leadId;
    private String projectId;
    private Date createdAt;
    private Date updatedAt;
}
