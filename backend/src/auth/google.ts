import { OAuth2Client } from 'google-auth-library'

const client = new OAuth2Client({
  clientId: process.env.GOOGLE_CLIENT_ID as string,
  clientSecret: process.env.GOOGLE_CLIENT_SECRET as string,
  redirectUri: '/auth/google/callback'
});

export function getUrl(otp: string) {
  return client.generateAuthUrl({ scope: ['openid', 'email'], otp });
}

export async function getTokens(code: string) {
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
    email: payload.email
  };
}
