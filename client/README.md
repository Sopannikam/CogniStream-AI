# CogniStream demo client

A small React + TypeScript interface for the Spring Boot API: GitHub sign-in, repository import/sync, indexing progress, and streamed chat with source links. Business rules and authentication decisions live in the backend. See the [project README](../README.md) for setup.

## Development

Start the backend on port 8080, then run:

```sh
npm ci
npm run dev
```

Open http://localhost:3000. To change the backend address, copy `.env.example` to `.env.local` and set `VITE_API_BASE_URL`. Keep GitHub credentials in the root backend `.env`, never in browser variables.

## Checks

```sh
npm test
npm run check
npm run build
```

Streaming tests cover split UTF-8 characters, SSE framing, server errors, and interrupted responses. The build checks TypeScript and generates static assets in `dist/`. Run `npm start` to preview that build. Static hosting needs an `index.html` fallback for client-side routes.

## Code layout

- `src/App.tsx`: backend session lookup and routes.
- `src/pages/Workspace.tsx`: repository operations and indexing progress.
- `src/pages/RepositoryChat.tsx`: conversations, streaming, and citations.
- `src/lib/api.ts`: typed REST calls using the backend session cookie.
- `src/lib/stream-chat.ts`: incremental SSE decoding.
- `src/styles.css`: plain responsive CSS.

There is no server-rendering framework or component library. Fonts use the local system stack, so builds do not need to download fonts.
