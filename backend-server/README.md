# BCF Builder AI — Backend Server & Gradle Build Worker

This directory contains the real Node.js + TypeScript Express API, WebSocket build log streamer, Prisma PostgreSQL schema, and isolated Gradle Android Build Worker used by **BCF Builder AI**.

## Features
- `GET /api/health` — Worker & Android SDK readiness check.
- `GET/POST/DELETE /api/projects` — Authenticated project persistence API.
- `POST /api/builds` & `GET /api/builds/:buildId` — Real sandboxed Gradle worker (`assembleDebug`, `assembleRelease`, `bundleRelease`) with path-traversal protection and process timeout enforcement.
- `POST /api/ai/complete` — Server-side proxy for Gemini / OpenAI-compatible APIs keeping provider keys off client devices.
- `/ws/builds` — WebSocket stream for live build status updates.

## Setup
```bash
cd backend-server
cp .env.example .env
npm install
npx prisma generate
npm run dev
```

## Connecting from Android App
1. Deploy this service (Cloud Run, Fly.io, VPS, or local LAN/ngrok with HTTPS).
2. Open **BCF Builder AI -> Settings**.
3. Enter your **BCF Build Worker Base URL** and optional **Worker Bearer Token**.
4. Tap **Test Worker** or open **Build Center -> Ping Health** to submit real APK/AAB builds.
