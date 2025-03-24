package com.example;

import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Example {
    public static void main(String[] args) {
        // TODO: Replace this with your actual Google Cloud API key
        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("Error: Please set the GOOGLE_API_KEY environment variable with your Google Cloud API key");
            System.out.println("You can get one from: https://makersuite.google.com/app/apikey");
            return;
        }
        SimpleMCPAgent agent = new SimpleMCPAgent(apiKey);
        Scanner scanner = new Scanner(System.in);
        String sessionId = "demo-session-" + System.currentTimeMillis();

        System.out.println("Welcome to the Enhanced MCP Agent Demo!");
        System.out.println("This agent can:");
        System.out.println("1. Engage in conversations with memory");
        System.out.println("2. Plan and break down complex tasks");
        System.out.println("3. Use specialized tools (calculator, web search)");
        System.out.println("4. Track goals and maintain context");
        System.out.println("\nCommands:");
        System.out.println("- Type 'goal <description>' to set a goal");
        System.out.println("- Type 'plan <task>' to break down a task");
        System.out.println("- Type 'calculate <number> <operation> <number>' for calculations");
        System.out.println("- Type 'search <query>' to search the web");
        System.out.println("- Type 'clear' to clear conversation history");
        System.out.println("- Type 'exit' to quit");
        System.out.println("- Type any other text to chat\n");

        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            } else if (input.equalsIgnoreCase("clear")) {
                agent.clearHistory(sessionId);
                System.out.println("Conversation history cleared!");
                continue;
            } else if (input.startsWith("goal ")) {
                String goal = input.substring(5);
                agent.setGoal(sessionId, goal);
                System.out.println("Goal set: " + goal);
                continue;
            } else if (input.startsWith("plan ")) {
                String task = input.substring(5);
                String response = agent.processMessage("Please help me plan: " + task, sessionId);
                System.out.println("Assistant: " + response);
                continue;
            } else if (input.startsWith("calculate ")) {
                String response = agent.processMessage(input, sessionId);
                System.out.println("Assistant: " + response);
                continue;
            } else if (input.startsWith("search ")) {
                String response = agent.processMessage(input, sessionId);
                System.out.println("Assistant: " + response);
                continue;
            } else {
                String response = agent.processMessage(input, sessionId);
                System.out.println("Assistant: " + response);
            }
        }

        scanner.close();
        agent.shutdown();
    }
} 