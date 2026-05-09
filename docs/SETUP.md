# Credential setup

## GitHub OAuth

Use [GitHub's official OAuth app instructions](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app). In GitHub, open Settings → Developer settings → OAuth Apps → New OAuth App.

Set application name to **CogniStream AI**, homepage to `http://localhost:3000`, and callback to **`http://localhost:8080/login/oauth2/code/github`**. Register the app, obtain its client ID, and generate its client secret. Add them to the root `.env` as `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`. For this tutorial implementation, disable expiring user access tokens if that option is present: token refresh is not implemented in the source tutorial. Device flow is not used.

The tutorial requests `read:user` and `repo` so it can read public and private repositories accessible to the signed-in user. Complete GitHub's authorization screen yourself. Organization repositories can require organization approval.

## OpenAI

Follow the [official developer quickstart](https://developers.openai.com/api/docs/quickstart) and create an API key in the [OpenAI Platform](https://platform.openai.com/api-keys). Save it as `OPENAI_API_KEY` in the root `.env`. Check your API project's available quota/credits before the live demo. Keys are loaded by the backend and must not use a `NEXT_PUBLIC_` prefix.

The backend uses Spring AI's OpenAI provider: `text-embedding-3-small` with 1536 dimensions for indexing, and a configurable chat model (`OPENAI_CHAT_MODEL`, default `gpt-4o-mini`). If the default model is unavailable to your API project, set a chat-completions-compatible model you have access to; do not change embedding dimensions without rebuilding the vector table.

## Local encryption

Use a private long password for `TOKEN_ENCRYPTOR_PASSWORD` and at least 16 even-numbered hexadecimal characters for `TOKEN_ENCRYPTOR_SALT`. They encrypt/decrypt the GitHub token stored in `users`. Keep both constant after signing in. If you change them, sign in again to replace the stored encrypted GitHub token.

## Database

Start Docker Desktop, then `docker compose up -d postgres` from the root. The new pgvector container is named `cognistream-pgvector`, with host port 5433 and database `cognistream_db`. The old plain PostgreSQL database is left in place. Extensions are initialized from `docker/postgres/init-extensions.sql`; Spring AI creates the vector table, and JPA updates the application tables.

## Troubleshooting

- Docker engine unavailable: finish starting Docker Desktop with Linux containers, then rerun Compose.
- `localhost:5433` connection refused: check `docker compose ps` and database health.
- `Asia/Calcutta` rejected: add `-Duser.timezone=Asia/Kolkata` to IntelliJ VM options; Maven's run configuration already includes it.
- OAuth mismatch: the callback must be the backend's `/login/oauth2/code/github`, not the frontend's `/auth/callback`.
- 401 after login: use `localhost` for both origins, verify the cookie name is `COGNISTREAM_SESSION`, then sign in again.
- Indexing FAILED: inspect the backend log and displayed job error; check GitHub access, OpenAI key, and quota.
- Changing a PostgreSQL password in `.env` does not change a password inside an existing volume. Keep the initial database credentials or change the database role password deliberately.
