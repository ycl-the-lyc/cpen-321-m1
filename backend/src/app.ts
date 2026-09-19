import express, { type Express } from 'express';
import { getTokens as getGoogleTokens, getUrl as getGoogleUrl } from './auth/google';

const MAGIC_OTP = '37'; //TODO

export function createApp(): Express {
  const app = express();

  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/ip', (_req, res) => {
    res.json({ ip: 'localhost' });
  });

  app.get('/time', (_req, res) => {
    const d = new Date();
    const o = d.getTimezoneOffset();
    const os = o < 0 ? '+' : '-'; // Date offset sign inversed
    const oa = Math.abs(o);
    const oh = Math.floor(oa / 60);
    const om = oa - oh * 60;
    res.json({ time: `${d.getHours()}:${d.getMinutes()}:${d.getSeconds()} GMT${os}${String(oh).padStart(2, '0')}:${String(om).padStart(2, '0')}` });
  });

  app.get('/name', (_req, res) => {
    res.json({ first: 'Yecheng', last: 'Liang' });
  });

  app.get('/auth/google', (_req, res) => {
    res.json({ url: getGoogleUrl(MAGIC_OTP) });
  });

  app.get('/auth/google/callback', async (req, res) => {
    try {
      const { code, otp } = req.query;

      if (typeof (code) !== 'string' || typeof (otp) !== 'string')
        res.status(404).json('Wrong OAuth params');

      if (otp !== MAGIC_OTP)
        res.status(400).json('Wrong state');

      const user = await getGoogleTokens(code as string);

      //TODO user shall exist

      res.json({
        user: {
          id: user.id,
          email: user.email
        }
      });
    } catch (err) {
      console.error(err);
      res.status(401).json('Google OAuth failed');
    }
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
