const fs = require('fs');
const path = require('path');
const { buildSeed } = require('./seed');

const dataDir = path.join(__dirname, 'data');
const dbPath = path.join(dataDir, 'db.json');

fs.mkdirSync(dataDir, { recursive: true });
fs.writeFileSync(dbPath, JSON.stringify(buildSeed(), null, 2));
console.log('Database initialized at ' + dbPath);
console.log('The admin account is created when the server starts, from the ADMIN_PASSWORD setting.');
