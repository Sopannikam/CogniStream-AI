# CogniStream AI

Understand your GitHub repositories through retrieval-augmented chat. Connect GitHub, sync your repositories, index their code, and ask questions with streamed answers and source citations.

This project follows Chai aur Code's [Spring Boot, RAG and GenAI tutorial](https://www.youtube.com/watch?v=R8VBA_KtUo0). Its implementation is adapted from the [instructor's DevPilot repository](https://github.com/Aestheticsuraj234/devPilot), with the student's package name and project branding, complete environment configuration, and fixes verified by automated tests. See [implementation notes](docs/IMPLEMENTATION.md) for the adaptations. This is a repository assistant, matching that tutorial's scope.

## Features

- GitHub OAuth login and session-based authentication.
- Encrypted storage of GitHub access tokens.
- Repository synchronization with search and indexing-status filters.
- Asynchronous indexing with progress, retry, and failure states.
- Code filtering and token-based splitting, OpenAI embeddings, and PostgreSQL/pgvector storage.
- Repository-scoped semantic retrieval for each question.
- Persistent chat sessions and messages.
- Server-sent event streaming, Markdown rendering, and clickable GitHub citations.
- The tutorial's repository dashboard, overview, settings, and light/dark themes.

## Stack

Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, JPA, Spring Security OAuth2 Client, PostgreSQL 16 with pgvector; Next.js 16.2.12, React 19, TypeScript, Tailwind CSS, TanStack Query, and the instructor's UI components.

## Run locally

Prerequisites: Java 21, Node.js 22 or newer compatible version, and running Docker Desktop with Linux containers. Use `localhost` consistently for both services.

1. Copy `.env.example` to `.env` in the project root. Fill `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, and `OPENAI_API_KEY`. Replace the encryption password and salt; the salt must be hexadecimal with an even length, at least 16 characters. Keep these values stable after your first sign-in.
2. Follow [credential setup](docs/SETUP.md). Credentials stay on the backend; the browser receives only the public backend URL.
3. Start the vector database from the project root:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

   Wait until it is healthy. This configuration uses port **5433** and a new `cognistream_vector_data` volume. It does not replace your earlier plain PostgreSQL container or its data.

4. Start the backend in a terminal:

   ```powershell
   cd cognistream-backend
   .\mvnw.cmd spring-boot:run
   ```

   The Maven run configuration sets `Asia/Kolkata`, avoiding the earlier `Asia/Calcutta` connection failure. When using IntelliJ's Java Run configuration, add `-Duser.timezone=Asia/Kolkata` to VM options. Open `cognistream-backend/pom.xml` as the Maven project and select Java 21.

5. Start the frontend in another terminal:

   ```powershell
   cd client
   npm ci
   npm run dev
   ```

6. Open [http://localhost:3000](http://localhost:3000). The backend runs on port 8080. Sign in, sync repositories, index a small code repository, then open chat.

Windows convenience scripts under `scripts/` run these same commands. Backend configuration imports the root `.env` automatically. Frontend defaults to `http://localhost:8080`; use `client/.env.local` only if changing its API address.

## Build and tests

```powershell
cd cognistream-backend
.\mvnw.cmd -Duser.timezone=Asia/Kolkata test
.\mvnw.cmd -DskipTests package
cd ..\client
npm ci
npm run test
npm run lint
npm run build
```

Backend tests use H2 and mock external AI/vector services. They check the actual application context, authentication boundaries, encrypted-token persistence, ownership checks, and indexing failure behavior. They do not demonstrate live OpenAI or pgvector operation. Production uses PostgreSQL and actual OpenAI models, with no mock-login or fabricated-answer mode.

For a production frontend preview after building, run `npm start`. The build scripts use Next.js's supported Webpack mode for compatibility with this Windows environment.

## Project structure

```text
cognistream-backend/  Spring Boot Maven application
  src/main/java/com/sopannikam/cognistream/
    config/          CORS, authentication, encryption, async execution
    controllers/     Auth, repository, and chat REST endpoints
    entity/          User, Repository, ChatSession, ChatMessage
    repository/      JPA persistence
    security/        GitHub OAuth user mapping and current-user checks
    services/        Profile sync, repository sync, indexing, RAG, SSE
client/              Next.js frontend, matching the tutorial flows
docker/postgres/     Extension initialization
docs/                Setup, architecture, API, and submission walkthrough
scripts/             Windows start commands
```

## Submission walkthrough

See [submission guide](docs/SUBMISSION.md), [API routes](docs/API.md), and [architecture](docs/IMPLEMENTATION.md). Run the live walkthrough after filling credentials; a compile/test pass alone is not an end-to-end demo.

## Practical limits

GitHub sync reads at most 1,000 repositories, matching the instructor's pagination cap. Indexing skips unsupported/generated files and files over 100 KiB. GitHub errors for individual files are logged; empty or failed embedding jobs stay FAILED. Large truncated GitHub trees are rejected instead of silently indexing only part of the repository. Citations identify retrieved source files; precise line ranges are optional and are not invented. Chat uses the current question and retrieved code, matching the tutorial, while storing conversation history for display. OpenAI usage requires an API key and available API quota/credits.
