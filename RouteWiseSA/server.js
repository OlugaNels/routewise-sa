const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const db = require('./db');

const PORT = Number(process.env.PORT || 3000);
const HTML_PATH = path.join(__dirname, 'Sentinel — Command Center.html');
const PUBLIC_DIR = path.join(__dirname, 'public');
const STATIC_FILES = new Map([
  ['/firebase.js', path.join(__dirname, 'firebase.js')],
  ['/firestoreService.js', path.join(__dirname, 'firestoreService.js')],
  ['/notificationService.js', path.join(__dirname, 'notificationService.js')],
  ['/firebase-messaging-sw.js', path.join(__dirname, 'firebase-messaging-sw.js')]
]);
const activeTokens = new Map();

function ensureDb() {
  db.ensureDb();
}

function loadDb() {
  ensureDb();
  return db.loadDb();
}

function saveDb(dbData) {
  db.saveDb(dbData);
}

function createToken(username) {
  const token = crypto.randomBytes(16).toString('hex');
  activeTokens.set(token, username);
  return token;
}

function authFromRequest(req) {
  const authHeader = req.headers.authorization || '';
  const match = authHeader.match(/^Bearer\s+(.+)$/i);
  if (!match) return null;
  const token = match[1];
  return activeTokens.has(token) ? { token, username: activeTokens.get(token) } : null;
}

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
  });
  res.end(JSON.stringify(payload));
}

function sendText(res, statusCode, text, contentType = 'text/plain; charset=utf-8') {
  res.writeHead(statusCode, {
    'Content-Type': contentType,
    'Cache-Control': 'no-store',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
  });
  res.end(text);
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
    });
    req.on('end', () => {
      if (!body) return resolve({});
      try {
        resolve(JSON.parse(body));
      } catch (error) {
        reject(new Error('Invalid JSON body'));
      }
    });
    req.on('error', reject);
  });
}

const provinceCityAliases = {
  'western cape': ['cape town', 'stellenbosch', 'george', 'mossel bay', 'paarl', 'saldanha'],
  gauteng: ['johannesburg', 'pretoria', 'soweto', 'benoni', 'vanderbijlpark', 'germiston', 'sandton', 'centurion', 'midrand'],
  'kwazulu-natal': ['durban', 'pietermaritzburg', 'richards bay', 'ballito', 'newcastle', 'pinetown'],
  'eastern cape': ['port elizabeth', 'gqeberha', 'east london', 'mthatha', 'grahamstown', 'queenstown', 'qonce', 'mdantsane'],
  'free state': ['bloemfontein', 'welkom', 'sasolburg', 'bethlehem', 'kroonstad'],
  limpopo: ['polokwane', 'mokopane', 'musina', 'thohoyandou', 'tzaneen', 'makhado'],
  mpumalanga: ['mbombela', 'middelburg', 'ermelo', 'witbank', 'emalahleni', 'secunda'],
  'north west': ['mafikeng', 'mahikeng', 'rustenburg', 'klerksdorp', 'potchefstroom'],
  'northern cape': ['kimberley', 'upington', 'springbok', 'de aar']
};

function normalizeProvinceName(provinceName) {
  const normalized = String(provinceName || '').toLowerCase().trim().replace(/[_\s]+/g, '-').replace(/-province$/, '');
  const aliases = {
    'kwazulu-natal': 'kwazulu-natal',
    kzn: 'kwazulu-natal',
    'north-west': 'north west',
    'northern-cape': 'northern cape',
    'eastern-cape': 'eastern cape',
    'western-cape': 'western cape',
    'free-state': 'free state'
  };
  return aliases[normalized] || normalized;
}

function reportBelongsToProvince(report, provinceName) {
  const province = normalizeProvinceName(provinceName);
  const reportProvince = normalizeProvinceName(report.province || report.provinceName);
  if (reportProvince) return reportProvince === province;
  const city = String(report.city || '').toLowerCase();
  return (provinceCityAliases[province] || []).some(alias => city.includes(alias));
}

function getReportTimestampSeconds(report) {
  const value = report.time_reported || report.timestamp || report.createdAt || report.created_at;
  if (typeof value === 'number') return value > 1e12 ? Math.floor(value / 1000) : value;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : Math.floor(parsed / 1000);
}

function getProvinceStats(provinceName, reports) {
  const matchedReports = (reports || [])
    .filter(report => reportBelongsToProvince(report, provinceName))
    .sort((left, right) => (getReportTimestampSeconds(right) || 0) - (getReportTimestampSeconds(left) || 0));

  const now = Math.floor(Date.now() / 1000);
  const day = 86400;
  const week = day * 7;
  const d = new Date();
  const startOfToday = Math.floor(new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime() / 1000);
  const firstDayThisMonth = Math.floor(new Date(d.getFullYear(), d.getMonth(), 1).getTime() / 1000);
  const firstDayLastMonth = Math.floor(new Date(d.getFullYear(), d.getMonth() - 1, 1).getTime() / 1000);

  function countSince(from) {
    return matchedReports.filter(report => (getReportTimestampSeconds(report) || 0) >= from).length;
  }

  const thisMonth = countSince(firstDayThisMonth);
  const lastMonth = matchedReports.filter(report => {
    const timestamp = getReportTimestampSeconds(report) || 0;
    return timestamp >= firstDayLastMonth && timestamp < firstDayThisMonth;
  }).length;
  const monthlyComparison = {
    thisMonth,
    lastMonth,
    change: lastMonth === 0 ? 0 : Math.round(((thisMonth - lastMonth) / lastMonth) * 100)
  };

  return {
    province: normalizeProvinceName(provinceName),
    reports: matchedReports,
    today: countSince(startOfToday),
    thisWeek: countSince(now - week),
    thisMonth,
    lastMonth,
    monthlyComparison
  };
}

function getProvinceReportCounts(reports) {
  return [
    ['Western Cape', 'western cape'],
    ['Gauteng', 'gauteng'],
    ['KwaZulu-Natal', 'kwazulu-natal'],
    ['Eastern Cape', 'eastern cape'],
    ['Free State', 'free state'],
    ['Limpopo', 'limpopo'],
    ['Mpumalanga', 'mpumalanga'],
    ['North West', 'north west'],
    ['Northern Cape', 'northern cape']
  ].map(([name, province]) => ({
    name,
    value: reports.filter(report => reportBelongsToProvince(report, province)).length
  }));
}

function getTrendDataForProvince(provinceName, reports) {
  const today = new Date();
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime() / 1000;
  const dailyCounts = Array(14).fill(0);
  (reports || []).filter(report => reportBelongsToProvince(report, provinceName)).forEach(report => {
    const timestamp = getReportTimestampSeconds(report);
    if (timestamp === null) return;
    const reportedAt = new Date(timestamp * 1000);
    const reportDay = new Date(reportedAt.getFullYear(), reportedAt.getMonth(), reportedAt.getDate()).getTime() / 1000;
    const ageInDays = Math.round((startOfToday - reportDay) / 86400);
    if (ageInDays >= 0 && ageInDays < dailyCounts.length) {
      dailyCounts[dailyCounts.length - 1 - ageInDays] += 1;
    }
  });
  return dailyCounts;
}

function getStats(db) {
  const reports = db.reports || [];
  const active = reports.filter(report => report.status === 'active').length;
  return {
    pending: 0,
    active,
    health: 98.7,
    api: 99,
    ingest: 97,
    geo: 100,
    queueDepth: reports.length,
    updatedAt: Math.floor(Date.now() / 1000)
  };
}

function handleApi(req, res, db) {
  const pathname = new URL(req.url, 'http://localhost').pathname;

  if (req.method === 'OPTIONS') {
    sendText(res, 204, '');
    return;
  }

  if (req.method === 'POST' && pathname === '/api/login') {
    parseJsonBody(req).then(payload => {
      const username = (payload.username || '').toString().trim();
      const password = (payload.password || '').toString().trim();
      const user = (db.users || []).find(entry => entry.username === username && entry.password === password);
      if (!user) {
        return sendJson(res, 401, { ok: false, error: 'Invalid credentials' });
      }
      const token = createToken(username);
      return sendJson(res, 200, { ok: true, token, username, role: user.role || 'operator' });
    }).catch(error => sendJson(res, 400, { ok: false, error: error.message }));
    return;
  }

  if (req.method === 'GET' && pathname === '/api/me') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, { ok: true, username: auth.username });
  }

  if (req.method === 'GET' && pathname === '/api/stats') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, getStats(db));
  }

  if (req.method === 'POST' && pathname === '/api/incidents/report') {
    parseJsonBody(req).then(payload => {
      const { kind, road, city, latitude, longitude, trust } = payload || {};
      if (!kind || !road || !city) {
        return sendJson(res, 400, { error: 'Missing required fields: kind, road, city' });
      }
      try {
        const result = db.insertIncidentReport({ kind, road, city, latitude, longitude, trust });
        if (result.changes > 0) {
          return sendJson(res, 201, { success: true, message: 'Report received', reportId: result.lastInsertRowid });
        }
        return sendJson(res, 500, { error: 'Could not save report' });
      } catch (err) {
        return sendJson(res, 500, { error: err.message });
      }
    }).catch(error => sendJson(res, 400, { error: error.message }));
    return;
  }

  if (req.method === 'GET' && pathname === '/api/incidents/live') {
    try {
      return sendJson(res, 200, db.getLiveIncidents());
    } catch (err) {
      return sendJson(res, 500, { error: err.message });
    }
  }

  if (req.method === 'POST' && pathname === '/api/admin/app-reports') {
    parseJsonBody(req).then(payload => {
      const { user_id, type, message } = payload || {};
      if (!type || !message) {
        return sendJson(res, 400, { error: 'Missing required fields: type (bug/help), message' });
      }
      try {
        const result = db.insertAdminReport({ user_id, type, message });
        if (result.changes > 0) {
          return sendJson(res, 201, { success: true, message: 'Report received' });
        }
        return sendJson(res, 500, { error: 'Could not save report' });
      } catch (err) {
        return sendJson(res, 500, { error: err.message });
      }
    }).catch(error => sendJson(res, 400, { error: error.message }));
    return;
  }

  if (req.method === 'GET' && pathname === '/api/admin/reports') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, db.getAdminReports());
  }

  if (req.method === 'PATCH' && pathname.startsWith('/api/admin/reports/')) {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    parseJsonBody(req).then(payload => {
      const { status } = payload || {};
      if (!status || !['open', 'in_progress', 'resolved'].includes(status)) {
        return sendJson(res, 400, { error: 'Invalid status' });
      }
      const result = db.updateAdminReportStatus(pathname.split('/').pop(), status);
      if (result.changes > 0) {
        return sendJson(res, 200, { success: true });
      }
      return sendJson(res, 404, { error: 'Report not found' });
    }).catch(error => sendJson(res, 400, { error: error.message }));
    return;
  }

  if (req.method === 'GET' && pathname === '/api/reports') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, db.reports || []);
  }

  if (req.method === 'GET' && pathname === '/api/queue') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, db.reports || []);
  }

  if (req.method === 'POST' && pathname === '/api/delete') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    parseJsonBody(req).then(payload => {
      if (!payload.id) return sendJson(res, 400, { ok: false, error: 'Missing report id' });
      const report = (db.reports || []).find(item => String(item.id) === String(payload.id));
      if (!report) return sendJson(res, 404, { ok: false, error: 'Report not found' });
      report.status = 'deleted';
      report.deleted_at = Math.floor(Date.now() / 1000);
      saveDb(db);
      return sendJson(res, 200, { ok: true });
    }).catch(error => sendJson(res, 400, { ok: false, error: error.message }));
    return;
  }

  if (req.method === 'POST' && pathname === '/api/dismiss') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    parseJsonBody(req).then(payload => {
      const report = (db.reports || []).find(item => String(item.id) === String(payload.id));
      if (!report) return sendJson(res, 404, { ok: false, error: 'Report not found' });
      report.status = 'deleted';
      report.deleted_at = Math.floor(Date.now() / 1000);
      saveDb(db);
      sendJson(res, 200, { ok: true });
    }).catch(error => sendJson(res, 400, { ok: false, error: error.message }));
    return;
  }

  if (req.method === 'GET' && pathname === '/api/markers') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, db.markers || []);
  }

  if (req.method === 'GET' && pathname === '/api/province') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, getProvinceReportCounts(db.reports || []));
  }

  if (req.method === 'GET' && pathname.startsWith('/api/province-analytics/')) {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    const province = decodeURIComponent(pathname.split('/').pop());
    return sendJson(res, 200, getProvinceStats(province, db.reports || []));
  }

  if (req.method === 'GET' && pathname === '/api/trend') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, db.trend || []);
  }

  if (req.method === 'GET' && pathname.startsWith('/api/trend/')) {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    const province = decodeURIComponent(pathname.split('/').pop());
    return sendJson(res, 200, getTrendDataForProvince(province, db.reports || []));
  }

  if (req.method === 'POST' && pathname === '/api/broadcast') {
    const auth = authFromRequest(req);
    if (!auth) return sendJson(res, 401, { ok: false, error: 'Unauthorized' });
    return sendJson(res, 200, { ok: true, reach: 2400000 });
  }

  sendJson(res, 404, { ok: false, error: 'Not found' });
}

function handleStatic(req, res) {
  const pathname = new URL(req.url, 'http://localhost').pathname;
  if (pathname === '/index.html') {
    res.writeHead(302, { Location: '/' });
    return res.end();
  }
  if (pathname === '/') {
    const html = fs.readFileSync(HTML_PATH, 'utf8');
    return sendText(res, 200, html, 'text/html; charset=utf-8');
  }

  let filePath = STATIC_FILES.get(pathname);
  if (!filePath && pathname.startsWith('/public/')) {
    const candidate = path.resolve(PUBLIC_DIR, decodeURIComponent(pathname.slice('/public/'.length)));
    if (candidate.startsWith(PUBLIC_DIR + path.sep)) filePath = candidate;
  }

  if (filePath && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const extension = path.extname(filePath);
    const contentType = extension === '.js'
      ? 'text/javascript; charset=utf-8'
      : extension === '.html'
        ? 'text/html; charset=utf-8'
        : 'application/octet-stream';
    return sendText(res, 200, fs.readFileSync(filePath), contentType);
  }

  sendText(res, 404, 'Not found', 'text/plain; charset=utf-8');
}

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const db = loadDb();
    return handleApi(req, res, db);
  }
  return handleStatic(req, res);
});

setInterval(() => {
  try {
    db.ensureDb();
  } catch (error) {
    console.error('Could not expire old incident reports:', error);
  }
}, 60_000);

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Sentinel server listening on http://localhost:${PORT}`);
});
