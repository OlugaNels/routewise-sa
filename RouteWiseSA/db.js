const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, 'data');
const DB_PATH = path.join(dataDir, 'db.json');
const REPORT_RETENTION_SECONDS = 12 * 60 * 60;

function getReportExpiryTime(report) {
  const timestamp = report.time_reported || report.timestamp || report.created_at;
  if (typeof timestamp === 'number') return timestamp;
  const parsed = Date.parse(timestamp);
  return Number.isNaN(parsed) ? null : Math.floor(parsed / 1000);
}

function removeExpiredReports(db) {
  const now = Math.floor(Date.now() / 1000);
  const reports = db.reports || [];
  db.reports = reports.filter(report => {
    const expiryTime = getReportExpiryTime(report);
    return expiryTime === null || now - expiryTime < REPORT_RETENTION_SECONDS;
  });
  return db.reports.length !== reports.length;
}

function ensureSeedData() {
  if (fs.existsSync(DB_PATH)) return;

  const seed = {
    users: [
      { username: 'admin', password: 'sentinel', role: 'admin' }
    ],
    reports: [],
    markers: [],
    province: [],
    trend: [],
    admin_reports: []
  };

  fs.mkdirSync(dataDir, { recursive: true });
  fs.writeFileSync(DB_PATH, JSON.stringify(seed, null, 2));
}

function loadDb() {
  ensureSeedData();
  const db = JSON.parse(fs.readFileSync(DB_PATH, 'utf8'));
  if (!Array.isArray(db.users)) db.users = [];
  if (!Array.isArray(db.reports)) db.reports = [];
  if (!Array.isArray(db.markers)) db.markers = [];
  if (!Array.isArray(db.province)) db.province = [];
  if (!Array.isArray(db.trend)) db.trend = [];
  if (!Array.isArray(db.admin_reports)) db.admin_reports = [];
  if (removeExpiredReports(db)) saveDb(db);
  return db;
}

function saveDb(db) {
  fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2));
}

function ensureDb() {
  ensureSeedData();
  const db = loadDb();
  if (!Array.isArray(db.admin_reports)) db.admin_reports = [];
  if (!Array.isArray(db.reports)) db.reports = [];
  db.reports.forEach(report => {
    if (report.status === 'pending') report.status = 'active';
  });
  saveDb(db);
}

function insertIncidentReport(data) {
  const db = loadDb();
  const { kind, road, city, latitude, longitude, trust } = data || {};
  const reportId = 'RPT-' + String(Date.now()).slice(-4) + String(Math.floor(Math.random() * 1000));
  const now = Math.floor(Date.now() / 1000);
  const report = {
    id: 'r' + Date.now() + Math.random().toString(36).slice(2, 7),
    report_id: reportId,
    kind,
    road,
    city,
    time_reported: now,
    trust: trust || 50,
    status: 'active',
    latitude: latitude || null,
    longitude: longitude || null
  };
  db.reports.push(report);
  saveDb(db);
  return { changes: 1, lastInsertRowid: report.id };
}

function getLiveIncidents() {
  return loadDb().reports
    .filter(report => report.status === 'verified' || report.status === 'active')
    .sort((a, b) => (b.time_reported || 0) - (a.time_reported || 0))
    .slice(0, 100)
    .map(report => ({
      id: report.id,
      report_id: report.report_id,
      kind: report.kind,
      road: report.road,
      city: report.city,
      time_reported: report.time_reported,
      latitude: report.latitude,
      longitude: report.longitude,
      trust: report.trust
    }));
}

function insertAdminReport(data) {
  const db = loadDb();
  const { user_id, type, message } = data || {};
  const entry = {
    id: 'ar' + Date.now() + Math.random().toString(36).slice(2, 7),
    user_id: user_id || 'anonymous',
    type,
    message,
    status: 'open',
    created_at: Math.floor(Date.now() / 1000),
    resolved_at: null
  };
  db.admin_reports.push(entry);
  saveDb(db);
  return { changes: 1, lastInsertRowid: entry.id };
}

function getAdminReports() {
  return loadDb().admin_reports
    .slice()
    .sort((a, b) => (b.created_at || 0) - (a.created_at || 0));
}

function updateAdminReportStatus(id, status) {
  const db = loadDb();
  const entry = (db.admin_reports || []).find(item => String(item.id) === String(id));
  if (!entry) return { changes: 0 };
  entry.status = status;
  entry.resolved_at = status === 'resolved' ? Math.floor(Date.now() / 1000) : null;
  saveDb(db);
  return { changes: 1 };
}

module.exports = {
  ensureDb,
  loadDb,
  saveDb,
  insertIncidentReport,
  getLiveIncidents,
  insertAdminReport,
  getAdminReports,
  updateAdminReportStatus
};
