package com.reminder.reminderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.reminder.reminderservice.model.*;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderBatchServiceImpl implements ReminderBatchService {

    @Override
    public Map<String, List<Lead>> scheduleReminders() throws ParseException, IOException {
        MongoClient client = MongoClients.create("mongodb+srv://ner2393:Tejas%400511@crmcluster.svmbz.mongodb.net/CRMDatabase?retryWrites=true&w=majority");
        MongoDatabase mongoDatabase = client.getDatabase("CRMDatabase");
        MongoCollection<Document> leadsCollection = mongoDatabase.getCollection("leads");
        MongoCollection<Document> leadProjectsCollection = mongoDatabase.getCollection("leadprojects");
        MongoCollection<Document> projectsCollection = mongoDatabase.getCollection("projects");
        MongoCollection<Document> telegramCollection = mongoDatabase.getCollection("telegrams");
        MongoCollection<Document> reminderBatchCollection = mongoDatabase.getCollection("reminderBatch");

        FindIterable<Document> leadIterator = leadsCollection.find();
        SimpleDateFormat sdFormat = new SimpleDateFormat("dd-MM-yyyy");
        //Date today = Calendar.getInstance().getTime();
        Date today = sdFormat.parse("01-04-2022");
        String todayString = sdFormat.format(today);
        this.addBatchStatusEntry(todayString, reminderBatchCollection);

        List<Lead> todaysLeads = new ArrayList<>();
        List<String> leadIdStrings = new ArrayList<>();
        leadIterator.forEach(document -> {
            java.util.Date followupdate = (Date) document.get("followupdate");
            String followupDateString = sdFormat.format(followupdate);
            if (followupDateString.equals(todayString)) {
                todaysLeads.add(convertDocumentToLead(document));
                leadIdStrings.add(document.get("_id").toString());
            }
        });

        List<LeadProject> allLeadProjects = new ArrayList<>();
        leadProjectsCollection.find().forEach(document -> {
            allLeadProjects.add(convertDocumentToLeadProject(document));
        });

        List<Project> allProjects = new ArrayList<>();
        projectsCollection.find().forEach(document -> {
            allProjects.add(convertDocumentToProject(document));
        });

        List<LeadProject> matchedLeadProjects = allLeadProjects.stream()
                .filter(leadProject -> leadIdStrings.contains(leadProject.getLeadId()))
                .collect(Collectors.toList());

        Map<String, List<LeadProject>> projectIdVsleadProjectMap = matchedLeadProjects
                .stream()
                .collect(Collectors.groupingBy(LeadProject::getProjectId, Collectors.toList()));

        Map<String, List<Lead>> finalmap = new HashMap<>(); //Map<ProjectId VS List<Lead>>
        projectIdVsleadProjectMap.forEach((projectId, leadProjects) -> {
            List<Lead> filteredLeads = leadProjects
                    .stream()
                    .map(leadProject -> todaysLeads
                            .stream()
                            .filter(lead -> lead.getLeadId().equals(leadProject.getLeadId()))
                            .findAny().get()).collect(Collectors.toList());
            finalmap.put(projectId, filteredLeads);
        });

        this.buildAndFireMessages(finalmap, allProjects, telegramCollection, reminderBatchCollection, todayString);
        return finalmap;
    }

    private void addBatchStatusEntry(String todayString, MongoCollection<Document> reminderBatchCollection) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("batchDate", todayString);
        MongoIterable<Document> cursor = reminderBatchCollection.find(whereQuery);
        Document d = cursor.first();
        if (d != null && d.get("batchDate").toString().equals(todayString) && d.get("batchStatus").equals("COMPLETED"))
            throw new RuntimeException("Batch already executed for : " + todayString + ". Please run batch for another day");
        else {
            Document document = new Document();
            document.put("batchDate", todayString);
            document.put("batchStatus", BatchStatus.STARTED.toString());
            reminderBatchCollection.insertOne(document);
        }
    }

    private void buildAndFireMessages(Map<String, List<Lead>> finalmap, List<Project> allProjects, MongoCollection<Document> telegramCollection,
                                      MongoCollection<Document> reminderBatchCollection, String todayString) throws IOException {

        finalmap.forEach((projectId, leads) -> {
            Optional<Project> project = allProjects.stream().filter(project1 -> project1.getProjectId().equals(projectId)).findAny();
            Telegram telegramDetails = null;
            if (project.isPresent()) {
                telegramDetails = getTelegramDetails(project.get().getTelegramLink(), telegramCollection);
            }
            //create texte here..
            StringBuilder sb = new StringBuilder();
            leads.forEach(lead -> sb.append(" | ").append(lead.getLeadName()).append(" | ").append(lead.getLeadPhone()).append(" | ")
                    .append(lead.getLeadEmail()).append(" | ").append("\n"));
            String message = sb.toString();

            try {
                this.buildMessage(telegramDetails, message);
            } catch (IOException e) {
                this.updateBatchStatus(todayString, reminderBatchCollection, BatchStatus.FAILED.toString(), e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
        this.updateBatchStatus(todayString, reminderBatchCollection, BatchStatus.COMPLETED.toString(), finalmap);
    }

    private void updateBatchStatus(String todayString, MongoCollection<Document> reminderBatchCollection, String batchStatus, Map<String, List<Lead>> finalmap) throws JsonProcessingException {
        BasicDBObject document = new BasicDBObject();
        document.put("batchDate", todayString);
        document.put("batchStatus", batchStatus);
        document.put("failureRootCause", "NA");
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(finalmap);
        BasicDBObject res = BasicDBObject.parse(json);
        document.put("projectVsLeadsMap", res);
        reminderBatchCollection.updateOne(new Document("batchDate", todayString), new Document("$set", document));
    }

    private void updateBatchStatus(String todayString, MongoCollection<Document> reminderBatchCollection, String batchStatus, String rootCause) {
        Document document = new Document();
        document.put("batchDate", todayString);
        document.put("batchStatus", batchStatus);
        document.put("failureRootCause", rootCause);
        reminderBatchCollection.insertOne(document);
    }

    private void buildMessage(Telegram telegramDetails, String message) throws IOException {
        if (message.length() > 1000) {
            List<String> messages = new ArrayList<>();
            int chunkSize = (message.length() / 1000);
            int mod = (message.length()) % 1000;
            int currRange = 0;
            for (int i = 0; i < chunkSize; i++) {
                String updatedMsg = message.substring(1000 * i, 1000 * (i + 1));
                currRange = 1000 * (i + 1);
                this.fireMessage(updatedMsg);
            }
            if (mod > 0) {
                String updatedMsg = message.substring(currRange, currRange + mod + 1);
                this.fireMessage(updatedMsg);
            }
        } else {
            this.fireMessage(message);
        }
    }

    private void fireMessage(String message) throws IOException {
        /*String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
        String apiToken = "1946029803:AAHDKD2AdVRU5nl_1kGd5XWiXxzjq1CiZQk";
        String chatId = "-460020621";
        urlString = String.format(urlString, apiToken, chatId, message);
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        StringBuilder sb = new StringBuilder();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        String response = sb.toString();*/
    }


    private Telegram getTelegramDetails(String telegramLink, MongoCollection<Document> telegramCollection) {
        Telegram telegram = new Telegram();
        telegramCollection.find().forEach(document -> {
            if (document.get("_id").toString().equals(telegramLink)) {
                telegram.setId(document.get("_id").toString());
                telegram.setTelegramBotKey((String) document.get("telegramBotKey"));
                telegram.setTelegramBotName((String) document.get("telegramBotName"));
                telegram.setOrganization(document.get("organization").toString());
                telegram.setCreatedAt((Date) document.get("createdAt"));
                telegram.setUpdatedAt((Date) document.get("updatedAt"));
            }
        });
        return telegram;

    }

    private static Project convertDocumentToProject(Document document) {
        Project project = new Project();
        project.setProjectId(document.get("_id").toString());
        project.setProjectKey((String) document.get("projectKey"));
        project.setProjectName((String) document.get("projectName"));
        project.setOrganization(document.get("organization").toString());
        project.setCreatedAt((Date) document.get("createdAt"));
        project.setUpdatedAt((Date) document.get("updatedAt"));
        project.setTelegramLink(document.get("telegramLink").toString());
        return project;
    }

    private static LeadProject convertDocumentToLeadProject(Document document) {
        LeadProject leadProject = new LeadProject();
        leadProject.setLeadProjectId(document.get("_id").toString());
        leadProject.setOrganization(document.get("organization").toString());
        leadProject.setLeadId(document.get("leadId").toString());
        leadProject.setProjectId(document.get("projectId").toString());
        leadProject.setCreatedAt((Date) document.get("createdAt"));
        leadProject.setUpdatedAt((Date) document.get("updatedAt"));
        return leadProject;
    }

    private static Lead convertDocumentToLead(Document document) {
        Lead lead = new Lead();
        lead.setLeadId(document.get("_id").toString());
        lead.setOrganization(document.get("organization").toString());
        lead.setLeadName((String) document.get("leadName"));
        lead.setLeadPhone((String) document.get("leadPhone"));
        lead.setLeadEmail((String) document.get("leadEmail"));
        lead.setFollowupdate((Date) document.get("followupdate"));
        lead.setLeadSource((Integer) document.get("leadSource"));
        lead.setCreatedAt((Date) document.get("createdAt"));
        lead.setUpdatedAt((Date) document.get("updatedAt"));
        return lead;
    }
}
