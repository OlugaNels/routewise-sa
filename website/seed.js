// Demo data for the dashboard. Timestamps are relative to "now" so the reports are always recent.
// Contains no users: the admin account is created from the ADMIN_PASSWORD setting (see db.js).
function buildSeed() {
  const now = Math.floor(Date.now() / 1000);
  return {
    users: [],
    reports: [
      { id: 'r1', report_id: 'R-1001', kind: 'accident', road: 'N1', city: 'Cape Town', trust: 89, time_reported: now - 600, status: 'active' },
      { id: 'r2', report_id: 'R-1002', kind: 'pothole', road: 'R300', city: 'Johannesburg', trust: 74, time_reported: now - 1800, status: 'active' },
      { id: 'r3', report_id: 'R-1003', kind: 'traffic', road: 'M4', city: 'Durban', trust: 82, time_reported: now - 3600, status: 'active' }
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
    trend: [12, 18, 14, 21, 16, 24, 19, 27, 23, 31, 28, 36, 33, 41],
    admin_reports: []
  };
}

module.exports = { buildSeed };
