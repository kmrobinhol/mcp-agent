package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMCPAgent {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent";
    private static final String GEMINI_VISION_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-vision:generateContent";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final Map<String, SessionState> sessionStates;
    private final Map<String, Tool> availableTools;

    public SimpleMCPAgent(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.sessionStates = new ConcurrentHashMap<>();
        this.availableTools = initializeTools();
    }

    private Map<String, Tool> initializeTools() {
        Map<String, Tool> tools = new HashMap<>();
        tools.put("calculator", new CalculatorTool());
        tools.put("web_search", new WebSearchTool());
        tools.put("task_planner", new TaskPlannerTool());
        return tools;
    }

    public String processMessage(String message, String sessionId) {
        try {
            SessionState state = getOrCreateSession(sessionId);
            
            // First, analyze if this is a task that needs planning
            if (isComplexTask(message)) {
                return handleComplexTask(message, state);
            }

            // Check if we need to use any tools
            Tool selectedTool = selectTool(message);
            if (selectedTool != null) {
                return selectedTool.execute(message, state);
            }

            // Regular conversation flow
            return handleRegularConversation(message, state);
        } catch (Exception e) {
            return "Error processing message: " + e.getMessage();
        }
    }

    private boolean isComplexTask(String message) {
        // Check if the message contains task-related keywords
        String[] taskKeywords = {"plan", "organize", "schedule", "break down", "steps", "sequence"};
        return Arrays.stream(taskKeywords).anyMatch(message.toLowerCase()::contains);
    }

    private String handleComplexTask(String message, SessionState state) {
        // Use the task planner tool to break down the task
        TaskPlannerTool planner = (TaskPlannerTool) availableTools.get("task_planner");
        List<String> subtasks = planner.planTask(message);
        
        StringBuilder response = new StringBuilder("I'll help you break this down into manageable steps:\n\n");
        for (int i = 0; i < subtasks.size(); i++) {
            response.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
        }
        
        // Store the task plan in the session state
        state.currentTask = new Task(message, subtasks);
        state.taskStatus = "PLANNED";
        
        return response.toString();
    }

    private Tool selectTool(String message) {
        // Simple tool selection based on message content
        if (message.toLowerCase().contains("calculate") || message.toLowerCase().contains("math")) {
            return availableTools.get("calculator");
        } else if (message.toLowerCase().contains("search") || message.toLowerCase().contains("find")) {
            return availableTools.get("web_search");
        }
        return null;
    }

    private String handleRegularConversation(String message, SessionState state) {
        // Create request body with enhanced context
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = objectMapper.createArrayNode();
        
        // Add system context
        addSystemContext(contents, state);
        
        // Add conversation history
        addConversationHistory(contents, state);
        
        // Add current message
        addCurrentMessage(contents, message);
        
        requestBody.set("contents", contents);

        // Execute request and handle response
        return executeRequest(requestBody, message, state);
    }

    private void addSystemContext(ArrayNode contents, SessionState state) {
        ObjectNode contextContent = objectMapper.createObjectNode();
        ArrayNode contextParts = objectMapper.createArrayNode();
        ObjectNode contextPart = objectMapper.createObjectNode();
        
        StringBuilder context = new StringBuilder("Current context:\n");
        if (state.currentTask != null) {
            context.append("Current task: ").append(state.currentTask.description).append("\n");
            context.append("Task status: ").append(state.taskStatus).append("\n");
        }
        if (!state.goals.isEmpty()) {
            context.append("Active goals:\n");
            state.goals.forEach(goal -> context.append("- ").append(goal).append("\n"));
        }
        
        contextPart.put("text", context.toString());
        contextParts.add(contextPart);
        contextContent.set("parts", contextParts);
        contextContent.put("role", "system");
        contents.add(contextContent);
    }

    private void addConversationHistory(ArrayNode contents, SessionState state) {
        if (!state.conversationHistory.isEmpty()) {
            ObjectNode historyContent = objectMapper.createObjectNode();
            ArrayNode historyParts = objectMapper.createArrayNode();
            ObjectNode historyPart = objectMapper.createObjectNode();
            historyPart.put("text", "Previous conversation:\n" + String.join("\n", state.conversationHistory));
            historyParts.add(historyPart);
            historyContent.set("parts", historyParts);
            historyContent.put("role", "user");
            contents.add(historyContent);
        }
    }

    private void addCurrentMessage(ArrayNode contents, String message) {
        ObjectNode content = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", message);
        parts.add(part);
        content.set("parts", parts);
        content.put("role", "user");
        contents.add(content);
    }

    private String executeRequest(ObjectNode requestBody, String message, SessionState state) {
        try {
            Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + apiKey)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    MediaType.parse("application/json")
                ))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    return "Error: " + response.code() + " - " + response.message() + "\nDetails: " + errorBody;
                }

                String responseBody = response.body().string();
                ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(responseBody);
                String generatedText = jsonResponse
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("No response generated");

                // Update session state
                state.conversationHistory.add("User: " + message);
                state.conversationHistory.add("Assistant: " + generatedText);
                if (state.conversationHistory.size() > 10) {
                    state.conversationHistory.remove(0);
                    state.conversationHistory.remove(0);
                }

                return generatedText;
            }
        } catch (Exception e) {
            return "Error executing request: " + e.getMessage();
        }
    }

    private SessionState getOrCreateSession(String sessionId) {
        return sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
    }

    public void setGoal(String sessionId, String goal) {
        SessionState state = getOrCreateSession(sessionId);
        state.goals.add(goal);
    }

    public void clearHistory(String sessionId) {
        sessionStates.remove(sessionId);
    }

    public void shutdown() {
        client.dispatcher().executorService().shutdown();
    }

    // Inner classes for state management and tools
    private static class SessionState {
        List<String> conversationHistory = new ArrayList<>();
        List<String> goals = new ArrayList<>();
        Task currentTask;
        String taskStatus;
    }

    private static class Task {
        String description;
        List<String> subtasks;

        Task(String description, List<String> subtasks) {
            this.description = description;
            this.subtasks = subtasks;
        }
    }

    private interface Tool {
        String execute(String input, SessionState state);
    }

    private static class CalculatorTool implements Tool {
        @Override
        public String execute(String input, SessionState state) {
            // Simple calculator implementation
            try {
                // Extract numbers and operation from input
                String[] parts = input.toLowerCase().split("\\s+");
                double result = 0;
                double num1 = Double.parseDouble(parts[1]);
                double num2 = Double.parseDouble(parts[3]);
                String operation = parts[2];

                switch (operation) {
                    case "+": result = num1 + num2; break;
                    case "-": result = num1 - num2; break;
                    case "*": result = num1 * num2; break;
                    case "/": result = num1 / num2; break;
                    default: return "Unsupported operation: " + operation;
                }

                return String.format("%.2f %s %.2f = %.2f", num1, operation, num2, result);
            } catch (Exception e) {
                return "Error calculating: " + e.getMessage();
            }
        }
    }

    private static class WebSearchTool implements Tool {
        @Override
        public String execute(String input, SessionState state) {
            // Simulate web search
            return "I would search the web for: " + input;
        }
    }

    private static class TaskPlannerTool implements Tool {
        @Override
        public String execute(String input, SessionState state) {
            return "Planning task: " + input;
        }

        public List<String> planTask(String task) {
            // Simple task planning implementation
            List<String> subtasks = new ArrayList<>();
            subtasks.add("Analyze requirements");
            subtasks.add("Break down into components");
            subtasks.add("Create timeline");
            subtasks.add("Assign resources");
            subtasks.add("Monitor progress");
            return subtasks;
        }
    }
} 