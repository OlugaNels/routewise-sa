# Sentinel-Command-Center

Sentinel is a web-based operations dashboard for monitoring reported road hazards in South Africa. It brings incident activity, moderation, and regional reporting into one interface for operators.

## Features

- **Live operations map** — view reported hazards on a map centered on South Africa.
- **Incident moderation** — review reports in the moderation queue and manage their status.
- **Regional analytics** — inspect provincial report counts and incident trends.
- **Notifications and broadcasts** — view dashboard notifications and access the emergency broadcast interface.
- **Firebase integration** — receive incident updates from Cloud Firestore and support browser push notifications through Firebase Cloud Messaging.
- **Local API and data** — run the Node.js server with JSON-file storage for the dashboard's API.

## Technology

- HTML, CSS, and browser JavaScript for the dashboard
- Node.js built-in HTTP server for the local API
- JSON file storage for local server data
- Firebase Cloud Firestore and Firebase Cloud Messaging integration

- 

## Running and deploying

Run locally (Node 18 or newer):

```
set ADMIN_PASSWORD=choose-a-password   (PowerShell: $env:ADMIN_PASSWORD="choose-a-password")
node server.js
```

Open http://localhost:3000 and sign in as `admin`. If `ADMIN_PASSWORD` is not set, a temporary password is printed in the server console.

Free hosting on Render: see `render.yaml` in the repository root. Create a Web Service from this repository, accept the free plan, and set `ADMIN_PASSWORD` under Environment. The free plan sleeps after 15 minutes without requests, and its disk is not kept between restarts, so sample data is re-created on start.
