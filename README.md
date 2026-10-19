# CogniStream

CogniStream is a Java/Spring Boot service for understanding GitHub repositories. It imports metadata, indexes source code asynchronously, retrieves relevant chunks with pgvector, and streams answers from a local model.

A small React client demonstrates the backend APIs: GitHub sign-in, repository import, indexing progress, and chat. Authentication, access rules, indexing, retrieval, and persistence live in the backend.

## Backend capabilities

- GitHub login, repository sync, and public repository imports by URL
- Transactional indexing admission, bounded background execution, progress tracking, and retries
- Semantic search using local embeddings and pgvector
- Streamed chat responses with Markdown and source links
- Saved chat sessions and a repository dashboard
- Encrypted token storage and repository ownership checks
- Request validation and consistent JSON errors

## Built with

**Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA/Hibernate, Spring AI, PostgreSQL/pgvector, and Maven.

**Local models:** Ollama, `nomic-embed-text:latest` (768 dimensions), and `qwen2.5-coder:1.5b`. No paid AI API key is required.

**Demo client:** React, TypeScript, Vite, React Router, plain CSS, and Markdown rendering.

## Getting started

Install Java 21, Node.js 22.12+, Docker Desktop, and [Ollama](https://ollama.com/download/windows). Create a free GitHub OAuth app for sign-in. Indexing and chat run locally without a paid API key.

Copy `.env.example` to `.env` and fill in your credentials and encryption settings. See [setup instructions](docs/SETUP.md) for the OAuth callback and configuration details.

Start Ollama with `ollama serve` (or open the Ollama app), then download the local models once:

```powershell
ollama pull nomic-embed-text:latest
ollama pull qwen2.5-coder:1.5b
```

Keep Ollama running. Downloads require internet; local inference uses your computer and can be slower on a CPU. GitHub sign-in and repository sync still need internet.

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

Open [localhost:3000](http://localhost:3000). Sign in, sync your repositories, and index one before opening chat. To explore someone else’s public repository, paste its GitHub URL into **Import a public repository**, import it, then choose **Index** in its row.

The backend runs on port 8080. PostgreSQL uses `DB_PORT` from `.env`, with 5433 as the default; keep the port in `DB_URL` consistent.

For IntelliJ IDEA, open `cognistream-backend/pom.xml`, select Java 21, and add `-Duser.timezone=Asia/Kolkata` to the application VM options.

## Project structure

```text
cognistream-backend/  Spring Boot backend
client/              Minimal React demo
docker/              Database configuration
scripts/             Windows launch scripts
docs/                Setup, architecture, and API documentation
```

## Development

Run backend checks with `.\mvnw.cmd '-Duser.timezone=Asia/Kolkata' verify` inside `cognistream-backend/`. Tests use H2 and mocked external services. In `client/`, use `npm test` and `npm run build`; the build checks TypeScript. GitHub Actions validates both parts.

## Explore the backend

- [API contract and examples](docs/API.md)
- [OpenAPI specification](docs/openapi.json), importable into Postman
- [HTTP requests](docs/requests.http) for repository, indexing, and streaming operations
- [Architecture](docs/IMPLEMENTATION.md)
- [Backend decisions and limitations](docs/BACKEND-DESIGN.md)


