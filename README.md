# Enhanced MCP Agent with Google Gemini

This is an advanced example of a Mission Control Protocol (MCP) agent using Google's Gemini AI. The project demonstrates how to create a sophisticated agent that can handle complex tasks, maintain state, and use specialized tools.

## Features

- **Task Planning and Execution**
  - Break down complex tasks into manageable subtasks
  - Track task progress and status
  - Maintain task context across conversations

- **Tool Integration**
  - Built-in calculator for mathematical operations
  - Web search capabilities (simulated)
  - Extensible tool system for adding new capabilities

- **State Management**
  - Session-based conversation history
  - Goal tracking and management
  - Context-aware responses

- **Advanced MCP Concepts**
  - Task decomposition
  - Tool selection and execution
  - State persistence
  - Goal-oriented behavior

## Prerequisites

- Java 17 or higher
- Maven
- Google Cloud API key with Gemini API access

## Setup

1. Clone this repository
2. Set your Google Cloud API key as an environment variable:
   ```bash
   export GOOGLE_API_KEY="your-api-key-here"
   ```
3. Build the project:
   ```bash
   mvn clean install
   ```

## Running the Example

Run the example using:
```bash
mvn exec:java -Dexec.mainClass="com.example.Example"
```

## Available Commands

- `goal <description>` - Set a goal for the agent
- `plan <task>` - Break down a complex task into subtasks
- `calculate <number> <operation> <number>` - Perform mathematical calculations
- `search <query>` - Search for information (simulated)
- `clear` - Clear conversation history
- `exit` - Exit the application

## Project Structure

- `SimpleMCPAgent.java`: The main agent class with enhanced MCP capabilities
- `Example.java`: Interactive command-line interface
- `pom.xml`: Maven project configuration and dependencies

## Architecture

The agent is built with several key components:

1. **Session Management**
   - Maintains conversation history
   - Tracks active goals and tasks
   - Preserves context across interactions

2. **Tool System**
   - Extensible tool interface
   - Built-in tools for common operations
   - Easy integration of new tools

3. **Task Planning**
   - Task decomposition
   - Progress tracking
   - Status management

4. **State Management**
   - Persistent session state
   - Goal tracking
   - Context awareness

## Important Notes

- Keep your API key secure and never commit it to version control
- The agent uses the Gemini Pro model
- The example includes proper resource cleanup with the `shutdown()` method
- Session state is maintained in memory and cleared on exit

## Customization

You can extend the agent by:

1. Adding new tools by implementing the `Tool` interface
2. Enhancing the task planning system
3. Adding more sophisticated state management
4. Implementing additional MCP concepts

## Contributing

Feel free to submit issues and enhancement requests! 