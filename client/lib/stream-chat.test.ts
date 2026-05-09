import assert from "node:assert/strict";
import { test } from "node:test";
import { streamChatMessage } from "./stream-chat";

function mockStream(text: string, chunkSize = 7) {
  const bytes = new TextEncoder().encode(text);
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (let index = 0; index < bytes.length; index += chunkSize) {
        controller.enqueue(bytes.slice(index, index + chunkSize));
      }
      controller.close();
    },
  });
  return new Response(body, { status: 200, headers: { "Content-Type": "text/event-stream" } });
}

test("decodes fragmented UTF-8 and CRLF frames and completes only once", async () => {
  const previousFetch = globalThis.fetch;
  const token = "Hello 👋\nquoted \"code\"";
  globalThis.fetch = async () => mockStream(`event: token\r\ndata: ${JSON.stringify(token)}\r\n\r\nevent: done\r\ndata: [DONE]\r\n\r\n`, 1);
  try {
    let result = "";
    let completions = 0;
    await streamChatMessage("session", "question", {
      onToken: (value) => { result += value; },
      onDone: () => { completions++; },
    });
    assert.equal(result, token);
    assert.equal(completions, 1);
  } finally { globalThis.fetch = previousFetch; }
});

test("provider errors reject the request so the UI shows failure", async () => {
  const previousFetch = globalThis.fetch;
  globalThis.fetch = async () => mockStream('event: error\ndata: {"message":"API quota exhausted"}\n\n');
  try {
    await assert.rejects(streamChatMessage("session", "question"), /API quota exhausted/);
  } finally { globalThis.fetch = previousFetch; }
});

test("an interrupted stream is not reported as a completed answer", async () => {
  const previousFetch = globalThis.fetch;
  globalThis.fetch = async () => mockStream('event: token\ndata: "partial"\n\n');
  try {
    await assert.rejects(streamChatMessage("session", "question"), /before completion/);
  } finally { globalThis.fetch = previousFetch; }
});
