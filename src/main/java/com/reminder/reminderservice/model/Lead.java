package com.reminder.reminderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lead {
    private String leadId;
    private String organization;
    private String leadName;
    private String leadPhone;
    private String leadEmail;
    private Date followupdate;
    private Integer leadSource;
    private Date createdAt;
    private Date updatedAt;
}
