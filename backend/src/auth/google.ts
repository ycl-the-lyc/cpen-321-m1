import { OAuth2Client } from 'google-auth-library'
import { env } from '../config/env'
import { User } from '../session/store';

const client = new OAuth2Client({
  clientId: env.google_client_id as string,
  clientSecret: env.google_client_secret as string,
  redirectUri: 'http://localhost:3000/auth/google/callback'
});

export function getUrl(state: string) {
  return client.generateAuthUrl({ scope: ['openid', 'email', 'profile'], state });
}

export async function getTokens(code: string): Promise<User> {
  const { tokens } = await client.getToken(code);

  if (!tokens.id_token)
    throw new Error('[GOOGLE] Missing ID token');

  const ticket = await client.verifyIdToken({
    idToken: tokens.id_token,
    audience: process.env.GOOGLE_CLIENT_ID as string
  });

  const payload = ticket.getPayload();

  if (!payload)
    throw new Error('[GOOGLE] Invalid ID token');

  return {
    id: payload.sub,
    email: payload.email as string,
    family_name: payload.family_name as string,
    given_name: payload.given_name as string
  };
}
