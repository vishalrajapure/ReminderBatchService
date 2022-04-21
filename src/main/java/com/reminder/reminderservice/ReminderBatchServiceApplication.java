package com.reminder.reminderservice;

import com.mongodb.client.*;
import com.reminder.reminderservice.model.Lead;
import com.reminder.reminderservice.model.LeadProject;
import com.reminder.reminderservice.model.Project;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class ReminderBatchServiceApplication {

    public static void main(String[] args) throws ParseException {
        SpringApplication.run(ReminderBatchServiceApplication.class, args);
        System.out.println("Batch started");
    }



}
