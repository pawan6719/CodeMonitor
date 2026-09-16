# CodeReviewer

CodeReviewer is an intelligent, automated pull request review assistant designed to provide real-time code quality, security, and performance feedback directly within your GitHub workflow.

## Overview

CodeReviewer integrates with GitHub webhooks to automatically trigger code analysis for every Pull Request. It leverages Google Gemini AI to analyze code changes (diffs), comparing them against historical user performance data, and provides actionable improvement suggestions directly as GitHub comments.

## Key Features

- **Automated PR Reviews**: Automatically analyzes incoming GitHub PRs using Google Gemini.
- **Historical Context Awareness**: Tracks user review history to provide personalized feedback and monitor progress over time.
- **Security & Quality Metrics**: Evaluates PRs based on security scores, performance, readability, and code coverage.
- **GitHub Integration**: Automatically posts line-level comments and a summary directly to the PR thread.
- **Robustness**: Implements built-in retry mechanisms for resilient API communication even in cloud-native environments (like GCP).
- **Asynchronous Processing**: Handles reviews asynchronously to ensure webhook response times remain within GitHub's limits.

## Technology Stack

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring AI (Google GenAI)**
- **Spring Data MongoDB**
- **GitHub API (RestClient)**

## Getting Started

### Prerequisites

- Java 21 or higher
- MongoDB instance
- GitHub Personal Access Token (PAT)
- Google Gemini API Key

### Configuration

Set the following environment variables:

- `MONGODB_URI`: Connection string for your MongoDB database.
- `GEMINI_API_KEY`: API Key for Google Gemini.
- `GITHUB_TOKEN`: GitHub Personal Access Token with PR read/write permissions.

Configure these in your `application.yml`:

```yaml
spring:
  mongodb:
    uri: ${MONGODB_URI}
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        chat:
          model: gemini-3.6-flash

github:
  token: ${GITHUB_TOKEN}
```

## Running the Application

1. **Build**: `./mvnw clean install`
2. **Run**: `./mvnw spring-boot:run`

## Running Tests

Execute the comprehensive test suite with:

```bash
./mvnw clean test
```

The project includes:
- **Unit & Slice Tests**: Covering all layers (Controller, Service, Component, Model).
- **Resilience Testing**: Ensures API communication, retries, and error parsing work correctly.

