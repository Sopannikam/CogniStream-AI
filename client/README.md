# CogniStream AI client

The Next.js client for GitHub login, repository sync, indexing progress, and chat. See the [project README](../README.md) for setup.

## Development

Start the backend on port 8080, then run:

```sh
npm ci
npm run dev
```

Open http://localhost:3000. To change the backend address, copy `.env.example` to `.env.local` and set `NEXT_PUBLIC_API_BASE_URL`.

## Checks

```sh
npm test
npm run lint
npm run build
```

Streaming tests cover split UTF-8 characters, SSE framing, server errors, and interrupted responses. Run `npm start` to preview a production build.

## Code layout

- `app/`: routes and layouts.
- `components/dashboard/`: repository sync, filters, and indexing status.
- `components/chat/`: sessions, messages, composer, and source citations.
- `hooks/`: authentication and query state.
- `lib/stream-chat.ts`: reads the backend SSE stream.

Shared UI primitives live in `components/ui/`; Markdown answers use Streamdown. Fonts use the local system stack, so builds do not need to download fonts.
