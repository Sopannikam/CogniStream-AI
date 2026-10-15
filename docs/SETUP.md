# Credential setup

## GitHub OAuth

Use [GitHub's official OAuth app instructions](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app). In GitHub, open Settings → Developer settings → OAuth Apps → New OAuth App.

Set application name to **CogniStream AI**, homepage to `http://localhost:3000`, and callback to **`http://localhost:8080/login/oauth2/code/github`**. Register the app, obtain its client ID, and generate its client secret. Add them to the root `.env` as `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`. Token refresh and device flow are not implemented.

The application requests `read:user` and `repo` to read repositories accessible to the signed-in user. Organization repositories can require organization approval.

## Local AI (no paid API)

Install [Ollama for Windows](https://ollama.com/download/windows). Open Ollama or run `ollama serve`, then download the two models once:

```powershell
ollama pull nomic-embed-text:latest
ollama pull qwen2.5-coder:1.5b
```

The Windows helpers `scripts/start-ai.cmd` and `scripts/setup-models.cmd` run the same steps. The AI launcher disables Ollama cloud features. Keep Ollama running alongside the backend and database.

The root `.env` uses `OLLAMA_BASE_URL=http://localhost:11434`, `OLLAMA_CHAT_MODEL=qwen2.5-coder:1.5b`, and `OLLAMA_EMBEDDING_MODEL=nomic-embed-text:latest`. No OpenAI key or API billing is needed. Model downloads use internet and disk space; inference uses your computer's CPU/RAM or supported GPU. The small coding model is the default for modest hardware; answers may be slower and less accurate than larger models.

Embeddings have 768 dimensions and use a separate `code_vectors_nomic` table. Existing users, repositories, chats, and the old vector table are preserved. Re-index repositories after switching models before using chat. Changing the embedding model again requires a matching dimension and a separate/rebuilt vector table; embeddings from different models cannot be mixed.

GitHub OAuth and repository downloads still need internet. Larger local chat models can be selected through `OLLAMA_CHAT_MODEL` after downloading them, if your computer has enough memory. Restart the backend after changing `.env`.

## Local encryption

Use a private long password for `TOKEN_ENCRYPTOR_PASSWORD` and at least 16 even-numbered hexadecimal characters for `TOKEN_ENCRYPTOR_SALT`. They encrypt/decrypt the GitHub token stored in `users`. Keep both constant after signing in. If you change them, sign in again to replace the stored encrypted GitHub token.

## Database

Start Docker Desktop, then `docker compose up -d postgres` from the root. The Compose project is named `cognistream-local`; the container is `cognistream-pgvector`, and the database is `cognistream_db`. The host port comes from `DB_PORT` in `.env` (default 5433). If that port is occupied, change both `DB_PORT` and the port in `DB_URL`, then start Compose again. Extensions are initialized from `docker/postgres/init-extensions.sql`; Spring AI creates the vector table, and JPA updates the application tables.

## Troubleshooting

- Ollama connection refused: open Ollama or run `scripts/start-ai.cmd`.
- Model not found: run `scripts/setup-models.cmd`, then retry.
- Local model out of memory: close unused apps or choose a smaller chat model in `.env`, then restart the backend.

- Docker engine unavailable: finish starting Docker Desktop with Linux containers, then rerun Compose.
- `localhost:5433` connection refused: check `docker compose ps` and database health.
- `Asia/Calcutta` rejected: add `-Duser.timezone=Asia/Kolkata` to IntelliJ VM options; Maven's run configuration already includes it.
- OAuth mismatch: the callback must be the backend's `/login/oauth2/code/github`, not the frontend's `/auth/callback`.
- 401 after login: use `localhost` for both origins, verify the cookie name is `COGNISTREAM_SESSION`, then sign in again.
- Indexing FAILED: inspect the backend log and displayed job error; check GitHub access, make sure Ollama is running at the configured URL, and check `ollama list` for both models.
- Changing a PostgreSQL password in `.env` does not change a password inside an existing volume. Keep the initial database credentials or change the database role password deliberately.
