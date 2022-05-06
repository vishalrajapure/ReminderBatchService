package com.reminder.reminderservice.controller;

import com.reminder.reminderservice.model.Lead;
import com.reminder.reminderservice.service.ReminderBatchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reminderbatch")
public class ReminderBatchController {

    // this is test code 
    @Autowired
    private ReminderBatchServiceImpl reminderBatchServiceImpl;

    @GetMapping("/trigger")
    public Map<String, List<Lead>> triggerBatch() throws ParseException, IOException {
        return reminderBatchServiceImpl.scheduleReminders();
    }
}
