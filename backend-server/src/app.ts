import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { projectsRouter } from './routes/projects.js';
import { buildsRouter } from './routes/builds.js';
import { aiRouter } from './routes/ai.js';

export function createApp() {
  const app = express();

  app.use(helmet());
  app.use(
    cors({
      origin: process.env.ALLOWED_ORIGINS?.split(',').map((s) => s.trim()) || '*',
    })
  );
  app.use(express.json({ limit: '15mb' }));
  app.use(
    rateLimit({
      windowMs: 60 * 1000,
      max: 120,
      standardHeaders: true,
      legacyHeaders: false,
    })
  );

  app.get('/api/health', (_req, res) => {
    res.json({
      status: 'ok',
      service: 'bcf-builder-ai-backend',
      gradleWorkerReady: Boolean(process.env.ANDROID_SDK_ROOT),
      timestamp: new Date().toISOString(),
    });
  });

  app.use('/api/projects', projectsRouter);
  app.use('/api/builds', buildsRouter);
  app.use('/api/ai', aiRouter);

  app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
    const message = err instanceof Error ? err.message : 'Internal server error';
    res.status(500).json({ error: message });
  });

  return app;
}
