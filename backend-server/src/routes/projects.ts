import { Router } from 'express';
import { z } from 'zod';
import { AuthedRequest, requireAuth } from '../middleware/auth.js';

export const projectsRouter = Router();

const projectStore = new Map<string, { id: string; ownerUid: string; name: string; packageName: string; payload: unknown; updatedAt: string }>();

const saveProjectSchema = z.object({
  id: z.string().min(1).max(120).optional(),
  name: z.string().min(1).max(80),
  packageName: z.string().min(3).max(120),
  payload: z.record(z.unknown()),
});

projectsRouter.use(requireAuth);

projectsRouter.get('/', (req: AuthedRequest, res) => {
  const uid = req.uid || '';
  const list = Array.from(projectStore.values()).filter((p) => p.ownerUid === uid);
  res.json({ projects: list });
});

projectsRouter.post('/', (req: AuthedRequest, res) => {
  const parsed = saveProjectSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'Invalid project payload', issues: parsed.error.issues });
  }

  const id = parsed.data.id || `proj_${Date.now()}`;
  const record = {
    id,
    ownerUid: req.uid || 'unknown',
    name: parsed.data.name,
    packageName: parsed.data.packageName,
    payload: parsed.data.payload,
    updatedAt: new Date().toISOString(),
  };
  projectStore.set(id, record);
  res.status(201).json(record);
});

projectsRouter.delete('/:id', (req: AuthedRequest, res) => {
  const existing = projectStore.get(req.params.id);
  if (!existing || existing.ownerUid !== req.uid) {
    return res.status(404).json({ error: 'Project not found' });
  }
  projectStore.delete(req.params.id);
  res.json({ deleted: true });
});
