import { Router } from 'express';
import { z } from 'zod';
import { requireAuth } from '../middleware/auth.js';

export const aiRouter = Router();

const aiTaskSchema = z.object({
  provider: z.enum(['gemini', 'openai']).default('gemini'),
  model: z.string().min(1).max(80).default('gemini-2.5-flash'),
  prompt: z.string().min(1).max(16000),
});

aiRouter.use(requireAuth);

aiRouter.post('/complete', async (req, res) => {
  const parsed = aiTaskSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'Invalid AI request', issues: parsed.error.issues });
  }

  const { provider, model, prompt } = parsed.data;

  if (provider === 'openai') {
    const key = process.env.OPENAI_API_KEY;
    if (!key) {
      return res.status(503).json({ error: 'OPENAI_API_KEY not configured on server' });
    }
    const resp = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        messages: [{ role: 'user', content: prompt }],
      }),
    });
    const data = await resp.json();
    return res.status(resp.status).json(data);
  }

  const geminiKey = process.env.GEMINI_API_KEY;
  if (!geminiKey) {
    return res.status(503).json({ error: 'GEMINI_API_KEY not configured on server' });
  }

  const resp = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(geminiKey)}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
    }
  );
  const data = await resp.json();
  return res.status(resp.status).json(data);
});
