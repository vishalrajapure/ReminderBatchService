package com.reminder.reminderservice.service;

import com.reminder.reminderservice.model.Lead;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface ReminderBatchService {
    Map<String, List<Lead>> scheduleReminders() throws ParseException, IOException;
}
