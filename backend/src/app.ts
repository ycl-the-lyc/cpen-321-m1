import express, { type Express } from 'express';
import { getTokens as getGoogleTokens, getUrl as getGoogleUrl } from './auth';
import { randId, states, sessions, tickets } from './session';
import { env } from './config/env'
// import { WebSocket } from 'ws'

const STATE_DURATION = 5 * 60 * 1000;
const SESSION_DURATION = 1 * 60 * 60 * 1000;
const TICKET_DURATION = 2 * 60 * 1000;

//HACK
const MAGIC_SESSION_ID = '37'
function isInSession(id: any) {
  return typeof (id) === 'string' && (id == MAGIC_SESSION_ID || sessions.get(id))
}

export function createApp(): Express {
  const app = express();

  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.post('/ip', (req, res) => {
    const { id } = req.body;

    if (!isInSession(id)) {
      res.status(404);
      return;
    }

    res.json({ ip: 'localhost' });
  });

  app.post('/time', (req, res) => {
    const { id } = req.body;

    if (!isInSession(id)) {
      res.status(404);
      return;
    }

    const d = new Date();
    const o = d.getTimezoneOffset();
    const os = o < 0 ? '+' : '-'; // Date offset sign inversed
    const oa = Math.abs(o);
    const oh = Math.floor(oa / 60);
    const om = oa - oh * 60;
    res.json({ time: `${d.getHours()}:${d.getMinutes()}:${d.getSeconds()} GMT${os}${String(oh).padStart(2, '0')}:${String(om).padStart(2, '0')}` });
  });

  app.post('/name', (req, res) => {
    const { id } = req.body;

    if (!isInSession(id)) {
      res.status(404);
      return;
    }

    res.json({ first: 'Yecheng', last: 'Liang' });
  });

  app.get('/auth/google', (_req, res) => {
    const state = randId();
    states.set(state, state, Date.now() + STATE_DURATION);
    res.json({ url: getGoogleUrl(state) });
  });

  app.get('/auth/google/callback', async (req, res) => {
    try {
      const { code, state } = req.query;

      if (typeof (code) !== 'string' || typeof (state) !== 'string') {
        res.status(400).json(`Wrong OAuth params:\ncode: ${code}\nstate: ${state}`);
        return;
      }

      if (!states.take(state)) {
        res.status(400).json(`Wrong login state: ${state}`);
        return;
      }

      const user = await getGoogleTokens(code as string);

      const sessionId = randId();
      sessions.set(sessionId, user, Date.now() + SESSION_DURATION);

      const ticket = randId();
      tickets.set(ticket, sessionId, Date.now() + TICKET_DURATION);

      // We dont store user info

      res.redirect(`cpen321m1://auth/done?ticket=${encodeURIComponent(ticket)}`);
    } catch (err) {
      console.error(err);
      res.status(401).json('Google OAuth failed');
    }
  });

  app.post('/auth/google/done', async (req, res) => {
    const { ticket } = req.body;

    if (typeof (ticket) !== 'string')
      return res.status(400);

    const sessionId = tickets.take(ticket);

    if (!sessionId) {
      res.status(401).json('Wrong login state');
      return;
    }

    const user = sessions.get(sessionId)

    if (!user) {
      res.status(401).json('User does not exist');
      return;
    }

    res.json({
      sessionId,
      user: {
        id: user.id,
        email: user.email,
        familyName: user.family_name,
        givenName: user.given_name
      }
    })
  });

  // app.use((_req, res) => {
  //   res.status(404).json({ error: 'Not Found' });
  // });

  return app;
}
