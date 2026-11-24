import 'dotenv/config';
import http from 'node:http';
import { WebSocketServer } from 'ws';
import { createApp } from './app.js';

const port = Number(process.env.PORT || 8080);
const app = createApp();
const server = http.createServer(app);

export const wss = new WebSocketServer({ server, path: '/ws/builds' });

wss.on('connection', (socket) => {
  socket.send(JSON.stringify({ type: 'connected', message: 'BCF Build Worker WebSocket stream active' }));
});

export function broadcastBuildEvent(event: Record<string, unknown>) {
  const frame = JSON.stringify(event);
  for (const client of wss.clients) {
    if (client.readyState === 1) {
      client.send(frame);
    }
  }
}

server.listen(port, () => {
  console.log(`BCF Builder AI Backend + Gradle Worker listening on :${port}`);
});
