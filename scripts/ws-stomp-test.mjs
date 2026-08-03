// Exercises the mainframe's STOMP-over-WebSocket layer end to end: register, login,
// create a room, connect, subscribe to the room topic, send a PLAY action. Builds
// STOMP frames with real bytes (actual newlines + a real NUL terminator) - Postman's
// native WebSocket message box can't reliably send either, so don't use it for this;
// use this script (or a real STOMP client) instead.
//
// Requires the mainframe running against a reachable Postgres/Redis (LocalStack only
// needed if you want the SNS publish inside PlaybackController to succeed too).
//
// Run: node scripts/ws-stomp-test.mjs

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

  console.log("=== Opening WebSocket ===", WS_URL);
  const ws = new WebSocket(WS_URL);

  ws.addEventListener("open", () => {
    console.log("[ws] open - sending CONNECT");
    ws.send(frame("CONNECT", { "accept-version": "1.1,1.2", host: "localhost", Authorization: accessToken }));
  });

  ws.addEventListener("message", (ev) => {
    console.log("[ws] <<< received:\n" + JSON.stringify(ev.data));

    if (ev.data.startsWith("CONNECTED")) {
      console.log("[ws] CONNECTED - sending SUBSCRIBE for room", roomId);
      ws.send(frame("SUBSCRIBE", { id: "sub-0", destination: `/topic/room/${roomId}` }));

      setTimeout(() => {
        console.log("[ws] sending SEND PLAY action");
        ws.send(
          frame(
            "SEND",
            { destination: `/app/room/${roomId}/action`, "content-type": "application/json" },
            JSON.stringify({ type: "PLAY", position_ms: 0 })
          )
        );
      }, 500);
    }
  });

  ws.addEventListener("error", (ev) => {
    console.error("[ws] error:", ev.message || ev);
  });

  ws.addEventListener("close", (ev) => {
    console.log(`[ws] closed: code=${ev.code} reason=${ev.reason}`);
  });

  // Keep alive long enough to observe CONNECTED + any broadcast/error frames.
  await new Promise((resolve) => setTimeout(resolve, 8000));
  console.log("=== done, closing ===");
  ws.close();
  await new Promise((resolve) => setTimeout(resolve, 500));
}

main().catch((e) => {
  console.error("FATAL:", e);
  process.exit(1);
});
