# CogniStream AI

A GitHub repository assistant that helps you explore a codebase through questions. Connect your GitHub account, index a repository, and get streamed answers with links to the relevant source files.

[![Build and test](https://github.com/Sopannikam/CogniStream-AI/actions/workflows/ci.yml/badge.svg)](https://github.com/Sopannikam/CogniStream-AI/actions/workflows/ci.yml)

## Features

- GitHub OAuth login with encrypted access-token storage.
- Repository synchronization, search, and indexing-status filters.
- Background code indexing with progress tracking and retries.
- Repository-specific semantic retrieval using OpenAI embeddings and pgvector.
- Chat sessions with saved messages, streamed Markdown answers, and source citations.
- Repository ownership checks and light/dark themes.

## Tech stack

| Backend | Frontend | Infrastructure |
| --- | --- | --- |
| Java 21, Spring Boot, Spring AI | Next.js, React, TypeScript | PostgreSQL 16, pgvector |
| Spring Security OAuth2, Spring Data JPA | Tailwind CSS, TanStack Query, Streamdown | Docker Compose, GitHub Actions |

## How it works

The indexing service reads eligible files from a repository's default branch, splits them into chunks, and stores their embeddings in pgvector. Each question retrieves the eight closest chunks from that repository. Spring AI sends the question and retrieved code to the model, and the frontend displays its response through server-sent events.

Chat history is saved for display. Each answer uses the current question and retrieved code context.

## Run locally

You need Java 21, Node.js 22, Docker Desktop with Linux containers, a GitHub OAuth app, and an OpenAI API key with available quota.

1. Copy `.env.example` to `.env` at the project root. Configure the GitHub credentials, OpenAI key, and encryption settings using the [setup guide](docs/SETUP.md).
2. Start PostgreSQL and wait for it to become healthy:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

3. Start the backend:

   ```powershell
   cd cognistream-backend
   .\mvnw.cmd spring-boot:run
   ```

4. In another terminal, start the frontend:

   ```powershell
   cd client
   npm ci
   npm run dev
   ```

5. Open [localhost:3000](http://localhost:3000), sign in, sync your repositories, and index a repository before opening chat.

The backend runs on port **8080** and PostgreSQL on **5433**. Windows launch scripts are available in `scripts/`.

For IntelliJ IDEA, open `cognistream-backend/pom.xml`, select Java 21, and add `-Duser.timezone=Asia/Kolkata` to the application VM options.

## Build and test

Backend:

```powershell
cd cognistream-backend
.\mvnw.cmd '-Duser.timezone=Asia/Kolkata' verify
```

Frontend:

```powershell
cd client
npm ci
npm test
npm run lint
npm run build
```

Backend tests use H2 and mocked external services. Frontend tests cover SSE decoding and error handling. GitHub Actions runs the backend and frontend checks. See [verification results](docs/VERIFICATION.md) for test coverage and live integration status.

## Project layout

```text
cognistream-backend/  Spring Boot API, persistence, indexing, and chat
client/              Next.js dashboard and chat interface
docker/postgres/     Database extension initialization
scripts/             Windows launch scripts
docs/                Setup and technical documentation
```

## Documentation

- [Environment and credential setup](docs/SETUP.md)
- [Architecture](docs/IMPLEMENTATION.md)
- [API endpoints](docs/API.md)

## Current limits

Repository sync reads up to 1,000 repositories. Indexing skips generated files, unsupported formats, and files larger than 100 KiB. Truncated GitHub trees and failed embedding jobs are reported as indexing failures. Citations link to retrieved files; exact line ranges are not available for every chunk.
