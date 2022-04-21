package com.reminder.reminderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Telegram {
    private String id;
    private String telegramBotName;
    private String telegramBotKey;
    private String telegramGroupId;
    private String organization;
    private Date createdAt;
    private Date updatedAt;

}
