import { WebSocket, WebSocketServer } from 'ws';
import { createApp } from './app';
import { env } from './config/env';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}

const wsc = new WebSocket('wss://8.229.22.124')
wsc.on('error', console.error);

const wss = new WebSocketServer({ port: 8080 })

wss.on('connection', function connection(ws) {
  ws.on('error', console.error);
  ws.on('close', () => { console.debug('client left') })

  console.debug(`client joint: ${ws.url}`)
});

wsc.on('message', function message(data) {
  wss.clients.forEach((ws) => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(data)
    }
  })
});
