import { NextFunction, Request, Response } from 'express';
import admin from 'firebase-admin';

if (!admin.apps.length && process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
  try {
    const creds = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    admin.initializeApp({ credential: admin.credential.cert(creds) });
  } catch (err) {
    console.warn('Firebase Admin init skipped:', err);
  }
}

export interface AuthedRequest extends Request {
  uid?: string;
}

export async function requireAuth(req: AuthedRequest, res: Response, next: NextFunction) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : '';

  if (!token) {
    return res.status(401).json({ error: 'Missing Authorization Bearer token' });
  }

  // Support configured worker shared token for private self-hosted build workers
  const workerSecret = process.env.BUILD_WORKER_SECRET?.trim();
  if (workerSecret && token === workerSecret) {
    req.uid = 'worker-admin';
    return next();
  }

  if (admin.apps.length) {
    try {
      const decoded = await admin.auth().verifyIdToken(token);
      req.uid = decoded.uid;
      return next();
    } catch {
      return res.status(401).json({ error: 'Invalid Firebase ID token' });
    }
  }

  return res.status(401).json({ error: 'Authentication backend not configured' });
}
