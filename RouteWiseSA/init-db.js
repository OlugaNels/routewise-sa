const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, 'data');
const dbPath = path.join(dataDir, 'db.json');

// db.exec(`
// CREATE TABLE IF NOT EXISTS admin_reports (
//     id INTEGER PRIMARY KEY AUTOINCREMENT,
//     user_id TEXT,
//     type TEXT NOT NULL,          -- 'bug' or 'help'
//     message TEXT NOT NULL,
//     status TEXT DEFAULT 'open',  -- open, in_progress, resolved
//     created_at INTEGER DEFAULT (strftime('%s','now')),
//     resolved_at INTEGER
// );
//
// -- We'll reuse the existing 'reports' table for incoming incident reports.
// -- It already has all needed columns.
// `);

const seed = {
  users: [
    { username: 'admin', password: 'sentinel', role: 'admin' }
  ],
  reports: [
    {
      id: 'r1',
      report_id: 'R-1001',
      kind: 'accident',
      road: 'N1',
      city: 'Cape Town',
      trust: 89,
      time_reported: Math.floor(Date.now() / 1000) - 600,
      status: 'active'
    },
    {
      id: 'r2',
      report_id: 'R-1002',
      kind: 'pothole',
      road: 'R300',
      city: 'Johannesburg',
      trust: 74,
      time_reported: Math.floor(Date.now() / 1000) - 1800,
      status: 'active'
    },
    {
      id: 'r3',
      report_id: 'R-1003',
      kind: 'traffic',
      road: 'M4',
      city: 'Durban',
      trust: 82,
      time_reported: Math.floor(Date.now() / 1000) - 3600,
      status: 'active'
    }
  ],
  markers: [
    { id: 'm1', kind: 'accident', name: 'N1 Cape Town', severity: 'high' },
    { id: 'm2', kind: 'pothole', name: 'R300 Johannesburg', severity: 'medium' },
    { id: 'm3', kind: 'traffic', name: 'M4 Durban', severity: 'high' }
  ],
  province: [
    { name: 'Western Cape', value: 14 },
    { name: 'Gauteng', value: 19 },
    { name: 'KwaZulu-Natal', value: 11 },
    { name: 'Eastern Cape', value: 8 }
  ],
  trend: [12, 18, 14, 21, 16, 24, 19, 27, 23, 31, 28, 36, 33, 41]
};

fs.mkdirSync(dataDir, { recursive: true });
fs.writeFileSync(dbPath, JSON.stringify(seed, null, 2));
console.log('Database initialized at ' + dbPath);
