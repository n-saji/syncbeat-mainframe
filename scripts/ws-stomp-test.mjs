// Exercises the mainframe's STOMP-over-WebSocket layer end to end: register, login,
// create a room, connect, subscribe to the room topic, send a PLAY action.
//
// As of the JWTHandshakeInterceptor fix, auth happens on the WS handshake itself (the
// access_token httpOnly cookie), NOT on the STOMP CONNECT frame - so this script needs
// to control the raw HTTP Upgrade request's headers (to attach a Cookie), which the
// browser-standard WebSocket API deliberately does not allow. Node's global `WebSocket`
// can't do this either, so this script performs the handshake itself via `http.request`
// + the 'upgrade' event, then implements minimal client-side WS frame encode/decode by
// hand (masked frames out, unmasked frames in) rather than pulling in the `ws` package -
// keeps this a dependency-free script per the project's existing convention.
//
// Requires the mainframe running against a reachable Postgres/Redis (LocalStack only
// needed if you want the SNS publish inside PlaybackController to succeed too).
//
// Run: node scripts/ws-stomp-test.mjs

import http from "node:http";
import crypto from "node:crypto";

const BASE_URL = process.env.SYNCBEAT_BASE_URL ?? "http://localhost:8080";
const WS_URL = process.env.SYNCBEAT_WS_URL ?? "ws://localhost:8080/ws";
const EMAIL = `stomp-test-${Date.now()}@example.com`;
const PASSWORD = "password123";

function frame(command, headers, body = "") {
  const headerLines = Object.entries(headers)
    .map(([k, v]) => `${k}:${v}`)
    .join("\n");
  return `${command}\n${headerLines}\n\n${body}\x00`;
}

function encodeTextFrame(str) {
  const payload = Buffer.from(str, "utf8");
  const maskKey = crypto.randomBytes(4);
  const masked = Buffer.alloc(payload.length);
  for (let i = 0; i < payload.length; i++) masked[i] = payload[i] ^ maskKey[i % 4];

  const len = payload.length;
  let header;
  if (len < 126) {
    header = Buffer.from([0x81, 0x80 | len]);
  } else if (len < 65536) {
    header = Buffer.alloc(4);
    header[0] = 0x81;
    header[1] = 0x80 | 126;
    header.writeUInt16BE(len, 2);
  } else {
    header = Buffer.alloc(10);
    header[0] = 0x81;
    header[1] = 0x80 | 127;
    header.writeBigUInt64BE(BigInt(len), 2);
  }
  return Buffer.concat([header, maskKey, masked]);
}

// Buffers incoming bytes and emits each complete text frame's payload as a string.
// Server frames are unmasked per spec, but the mask branch is handled defensively anyway.
function createFrameDecoder(onText) {
  let buf = Buffer.alloc(0);
  return function onData(chunk) {
    buf = Buffer.concat([buf, chunk]);
    for (;;) {
      if (buf.length < 2) return;
      const masked = (buf[1] & 0x80) !== 0;
      let len = buf[1] & 0x7f;
      let offset = 2;
      if (len === 126) {
        if (buf.length < 4) return;
        len = buf.readUInt16BE(2);
        offset = 4;
      } else if (len === 127) {
        if (buf.length < 10) return;
        len = Number(buf.readBigUInt64BE(2));
        offset = 10;
      }
      if (masked) offset += 4;
      if (buf.length < offset + len) return;

      let payload = buf.subarray(offset, offset + len);
      if (masked) {
        const maskKey = buf.subarray(offset - 4, offset);
        const unmasked = Buffer.alloc(len);
        for (let i = 0; i < len; i++) unmasked[i] = payload[i] ^ maskKey[i % 4];
        payload = unmasked;
      }
      onText(payload.toString("utf8"));
      buf = buf.subarray(offset + len);
    }
  };
}

// Performs the raw HTTP Upgrade handshake by hand so we control the Cookie header.
// Resolves { socket, status } - socket is null if the server never upgraded (rejected handshake).
function openRawWebSocket(wsUrl, { cookie } = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(wsUrl);
    const headers = {
      Connection: "Upgrade",
      Upgrade: "websocket",
      "Sec-WebSocket-Version": "13",
      "Sec-WebSocket-Key": crypto.randomBytes(16).toString("base64"),
    };
    if (cookie) headers.Cookie = cookie;

    const req = http.request({
      hostname: url.hostname,
      port: url.port || 80,
      path: url.pathname + url.search,
      headers,
    });

    req.on("upgrade", (res, socket) => resolve({ socket, status: res.statusCode }));
    req.on("response", (res) => {
      let body = "";
      res.on("data", (c) => (body += c));
      res.on("end", () => resolve({ socket: null, status: res.statusCode, body }));
    });
    req.on("error", reject);
    req.end();
  });
}

async function main() {
  console.log("=== Register ===");
  let res = await fetch(`${BASE_URL}/api/users/create`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ first_name: "Stomp", last_name: "Tester", email: EMAIL, password: PASSWORD }),
  });
  console.log(res.status, await res.text());

  console.log("=== Login ===");
  res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });
  const setCookieHeaders = res.headers.getSetCookie ? res.headers.getSetCookie() : [res.headers.get("set-cookie")];
  const accessTokenCookie = setCookieHeaders.find((c) => c && c.startsWith("access_token="));
  if (!accessTokenCookie) {
    console.error("No access_token cookie in login response:", setCookieHeaders);
    process.exit(1);
  }
  const accessToken = accessTokenCookie.split(";")[0].split("=")[1];
  const cookieHeader = `access_token=${accessToken}`;
  console.log("access_token:", accessToken.slice(0, 20) + "...");

  console.log("=== Create Room ===");
  res = await fetch(`${BASE_URL}/api/rooms/create`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ name: `Stomp Test Room ${Date.now()}`, is_public: true }),
  });
  const room = await res.json();
  console.log(res.status, room);
  const roomId = room.id;
  if (!roomId) {
    console.error("Room creation failed, aborting.");
    process.exit(1);
  }

  console.log("\n=== Negative check: handshake with NO cookie should be rejected ===");
  const rejected = await openRawWebSocket(WS_URL);
  console.log(`no-cookie handshake status: ${rejected.status} (expect 401, socket=${rejected.socket ? "upgraded (BAD)" : "not upgraded (good)"})`);
  rejected.socket?.destroy();

  console.log("\n=== Positive check: handshake WITH cookie ===");
  const { socket, status } = await openRawWebSocket(WS_URL, { cookie: cookieHeader });
  console.log(`cookie handshake status: ${status}`);
  if (!socket) {
    console.error("Handshake was rejected even with a valid cookie - aborting.");
    process.exit(1);
  }

  const decoder = createFrameDecoder((text) => {
    console.log("[ws] <<< received:\n" + JSON.stringify(text));

    if (text.startsWith("CONNECTED")) {
      console.log("[ws] CONNECTED - sending SUBSCRIBE for room", roomId);
      socket.write(encodeTextFrame(frame("SUBSCRIBE", { id: "sub-0", destination: `/topic/room/${roomId}` })));

      setTimeout(() => {
        console.log("[ws] sending SEND PLAY action (no Authorization header - handshake auth only)");
        socket.write(
          encodeTextFrame(
            frame(
              "SEND",
              { destination: `/app/room/${roomId}/action`, "content-type": "application/json" },
              JSON.stringify({ type: "PLAY", position_ms: 0 })
            )
          )
        );
      }, 500);
    }
  });
  socket.on("data", decoder);
  socket.on("error", (e) => console.error("[ws] socket error:", e.message));
  socket.on("close", () => console.log("[ws] socket closed"));

  console.log("[ws] sending CONNECT (no Authorization header)");
  socket.write(encodeTextFrame(frame("CONNECT", { "accept-version": "1.1,1.2", host: "localhost" })));

  // Keep alive long enough to observe CONNECTED + any broadcast/error frames.
  await new Promise((resolve) => setTimeout(resolve, 8000));
  console.log("=== done, closing ===");
  socket.destroy();
  await new Promise((resolve) => setTimeout(resolve, 500));
}

main().catch((e) => {
  console.error("FATAL:", e);
  process.exit(1);
});
