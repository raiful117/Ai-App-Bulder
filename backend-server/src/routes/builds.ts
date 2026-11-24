import { Router } from 'express';
import { z } from 'zod';
import { AuthedRequest, requireAuth } from '../middleware/auth.js';
import { enqueueGradleBuild, getBuildJob } from '../workers/gradleWorker.js';

export const buildsRouter = Router();

const createBuildSchema = z.object({
  projectName: z.string().min(1).max(80),
  packageName: z.string().min(3).max(120),
  target: z.enum(['DEBUG_APK', 'RELEASE_APK', 'APP_BUNDLE']).default('DEBUG_APK'),
  gradleTask: z.enum(['assembleDebug', 'assembleRelease', 'bundleRelease']).default('assembleDebug'),
  projectJson: z.string().min(2),
});

buildsRouter.use(requireAuth);

buildsRouter.post('/', async (req: AuthedRequest, res) => {
  const parsed = createBuildSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'Invalid build request', issues: parsed.error.issues });
  }

  const job = await enqueueGradleBuild({
    ownerUid: req.uid || 'worker-admin',
    projectName: parsed.data.projectName,
    packageName: parsed.data.packageName,
    target: parsed.data.target,
    gradleTask: parsed.data.gradleTask,
    projectJson: parsed.data.projectJson,
  });

  res.status(202).json(job);
});

buildsRouter.get('/:buildId', (req: AuthedRequest, res) => {
  const job = getBuildJob(req.params.buildId);
  if (!job || job.ownerUid !== req.uid) {
    return res.status(404).json({ error: 'Build job not found' });
  }
  res.json(job);
});

buildsRouter.get('/:buildId/artifact', (req: AuthedRequest, res) => {
  const job = getBuildJob(req.params.buildId);
  if (!job || job.ownerUid !== req.uid) {
    return res.status(404).json({ error: 'Build job not found' });
  }
  if (!job.artifactFilePath) {
    return res.status(404).json({ error: 'Compiled APK/AAB artifact not available yet' });
  }
  return res.download(job.artifactFilePath);
});
