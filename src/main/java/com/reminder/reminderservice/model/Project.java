package com.reminder.reminderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String projectId;
    private String projectKey;
    private String projectName;
    private String organization;
    private Date createdAt;
    private Date updatedAt;
    private String telegramLink;
}
