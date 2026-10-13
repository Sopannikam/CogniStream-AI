# CogniStream AI

CogniStream lets you ask questions about a GitHub repository and find the code behind the answer. Sign in with GitHub, index a repository, and start a chat with source-file citations.

## Features

- GitHub login and repository sync
- Code indexing with progress tracking and retries
- Semantic search using OpenAI embeddings and pgvector
- Streamed chat responses with Markdown and source links
- Saved chat sessions and a repository dashboard
- Encrypted token storage and repository ownership checks

## Built with

Java 21, Spring Boot, Spring AI, Spring Security, PostgreSQL/pgvector, Next.js, React, TypeScript, and Tailwind CSS.

## Getting started

Install Java 21, Node.js 22, and Docker Desktop. You also need a GitHub OAuth app and an OpenAI API key.

Copy `.env.example` to `.env` and fill in your credentials and encryption settings. See [setup instructions](docs/SETUP.md) for the OAuth callback and configuration details.

Start the database from the project root:

```powershell
docker compose up -d postgres
```

Start the backend:

```powershell
cd cognistream-backend
.\mvnw.cmd spring-boot:run
```

In another terminal, start the frontend:

```powershell
cd client
npm ci
npm run dev
```

Open [localhost:3000](http://localhost:3000). Sign in, sync your repositories, and index one before opening chat. The backend runs on port 8080 and PostgreSQL on port 5433.

For IntelliJ IDEA, open `cognistream-backend/pom.xml`, select Java 21, and add `-Duser.timezone=Asia/Kolkata` to the application VM options.

## Project structure

```text
cognistream-backend/  Spring Boot backend
client/              Next.js frontend
docker/              Database configuration
scripts/             Windows launch scripts
docs/                Setup, architecture, and API documentation
```

## Development

Run backend checks with `.\mvnw.cmd '-Duser.timezone=Asia/Kolkata' verify` inside `cognistream-backend/`. In `client/`, use `npm test`, `npm run lint`, and `npm run build`.


