import { AccessToken } from 'livekit-server-sdk';

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { room, username } = req.query;

  if (!room || !username) {
    return res.status(400).json({ error: 'Missing room or username parameter' });
  }

  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;

  if (!apiKey || !apiSecret) {
    return res.status(500).json({ error: 'LiveKit credentials missing on server' });
  }

  try {
    const at = new AccessToken(apiKey, apiSecret, {
      identity: String(username),
      name: String(username),
    });

    at.addGrant({
      roomJoin: true,
      room: String(room),
      canPublish: true,
      canSubscribe: true,
    });

    const token = await at.toJwt();
    return res.status(200).json({ token });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
}
