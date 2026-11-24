import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { broadcastBuildEvent } from '../server.js';

export interface BuildJob {
  buildId: string;
  ownerUid: string;
  projectName: string;
  packageName: string;
  target: string;
  gradleTask: string;
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';
  logs: string;
  artifactUrl: string;
  artifactFilePath?: string;
  createdAt: string;
}

const buildJobs = new Map<string, BuildJob>();

export function getBuildJob(buildId: string): BuildJob | undefined {
  return buildJobs.get(buildId);
}

export async function enqueueGradleBuild(params: {
  ownerUid: string;
  projectName: string;
  packageName: string;
  target: string;
  gradleTask: string;
  projectJson: string;
}): Promise<BuildJob> {
  const buildId = `bld_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const job: BuildJob = {
    buildId,
    ownerUid: params.ownerUid,
    projectName: params.projectName,
    packageName: params.packageName,
    target: params.target,
    gradleTask: params.gradleTask,
    status: 'QUEUED',
    logs: 'Build job accepted and queued for isolated Gradle sandbox.\n',
    artifactUrl: '',
    createdAt: new Date().toISOString(),
  };

  buildJobs.set(buildId, job);
  broadcastBuildEvent({ type: 'build_queued', buildId, status: job.status });

  void executeGradleBuild(job, params.projectJson);
  return job;
}

async function findOutputArtifact(dir: string, ext: string): Promise<string | undefined> {
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        const nested = await findOutputArtifact(full, ext);
        if (nested) return nested;
      } else if (entry.name.endsWith(ext)) {
        return full;
      }
    }
  } catch {
    return undefined;
  }
  return undefined;
}

async function executeGradleBuild(job: BuildJob, rawProjectJson: string) {
  const workspaceRoot = process.env.WORKSPACE_ROOT || '/tmp/bcf_builds';
  const workDir = path.join(workspaceRoot, job.buildId);

  try {
    job.status = 'RUNNING';
    job.logs += `Preparing workspace at ${workDir}...\n`;
    await fs.mkdir(workDir, { recursive: true });

    const parsed = JSON.parse(rawProjectJson) as {
      files?: Array<{ path?: string; content?: string }>;
    };

    const files = parsed.files || [];
    for (const file of files) {
      if (!file.path || typeof file.content !== 'string') continue;
      const normalized = path.normalize(file.path).replace(/^(\.\.(\/|\\|$))+/, '');
      const dest = path.resolve(workDir, normalized);
      if (!dest.startsWith(path.resolve(workDir))) continue;
      await fs.mkdir(path.dirname(dest), { recursive: true });
      await fs.writeFile(dest, file.content, 'utf8');
    }

    job.logs += `Materialized ${files.length} files. Invoking gradle ${job.gradleTask}...\n`;
    const timeoutMs = Number(process.env.GRADLE_TIMEOUT_MS || 300000);

    await new Promise<void>((resolve, reject) => {
      const child = spawn('gradle', [job.gradleTask, '--no-daemon'], {
        cwd: workDir,
        env: { ...process.env },
      });

      const timer = setTimeout(() => {
        child.kill('SIGKILL');
        reject(new Error(`Gradle timed out after ${timeoutMs}ms`));
      }, timeoutMs);

      child.stdout.on('data', (chunk: Buffer) => {
        job.logs += chunk.toString();
      });
      child.stderr.on('data', (chunk: Buffer) => {
        job.logs += chunk.toString();
      });
      child.on('error', (err) => {
        clearTimeout(timer);
        reject(err);
      });
      child.on('close', (code) => {
        clearTimeout(timer);
        if (code === 0) resolve();
        else reject(new Error(`Gradle exited with code ${code}`));
      });
    });

    const ext = job.target === 'APP_BUNDLE' ? '.aab' : '.apk';
    const outputsDir = path.join(workDir, 'app', 'build', 'outputs');
    job.artifactFilePath = await findOutputArtifact(outputsDir, ext);
    job.status = 'SUCCEEDED';
    job.artifactUrl = `/api/builds/${job.buildId}/artifact`;
    job.logs += `\nBuild SUCCEEDED: ${job.artifactUrl}\n`;
    broadcastBuildEvent({ type: 'build_succeeded', buildId: job.buildId, artifactUrl: job.artifactUrl });
  } catch (err) {
    job.status = 'FAILED';
    const message = err instanceof Error ? err.message : String(err);
    job.logs += `\nBuild FAILED: ${message}\n`;
    broadcastBuildEvent({ type: 'build_failed', buildId: job.buildId, error: message });
  }
}
