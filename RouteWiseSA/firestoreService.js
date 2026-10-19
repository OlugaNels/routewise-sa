import {
  collection,
  onSnapshot,
  doc,
  updateDoc,
  increment
} from 'https://www.gstatic.com/firebasejs/10.13.1/firebase-firestore.js';
import { db, firebaseConfig } from './firebase.js';

function decodeFirestoreValue(value) {
  if ('stringValue' in value) return value.stringValue;
  if ('integerValue' in value) return Number(value.integerValue);
  if ('doubleValue' in value) return value.doubleValue;
  if ('booleanValue' in value) return value.booleanValue;
  if ('nullValue' in value) return null;
  if ('timestampValue' in value) return value.timestampValue;
  if ('geoPointValue' in value) return value.geoPointValue;
  if ('arrayValue' in value) return (value.arrayValue.values || []).map(decodeFirestoreValue);
  if ('mapValue' in value) return decodeFirestoreFields(value.mapValue.fields || {});
  return null;
}

function decodeFirestoreFields(fields) {
  return Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, decodeFirestoreValue(value)]));
}

function getIncidentSortTime(incident) {
  if (typeof incident.timestamp === 'number') return incident.timestamp;
  const parsed = Date.parse(incident.timestamp);
  return Number.isNaN(parsed) ? 0 : parsed;
}

/**
 * 1. Listen for Live Hazard Reports submitted by Android App Drivers
 */
export function initSentinelLiveStream(onNewHazardReport, onError = console.error, onSyncState = () => {}) {
  const incidentsCollection = collection(db, 'incidents');
  let isActive = true;
  let lastSnapshot = '';

  const publishSnapshot = (snapshot) => {
    const incidents = snapshot.docs.map((incidentDoc) => {
      const data = incidentDoc.data();
      const timestamp = data.timestamp && typeof data.timestamp.toDate === 'function'
        ? data.timestamp.toDate().toISOString()
        : data.timestamp;
      return { id: incidentDoc.id, ...data, timestamp };
    }).filter(hasIncidentData);
    incidents.sort((left, right) => getIncidentSortTime(right) - getIncidentSortTime(left));

    const signature = JSON.stringify(incidents);
    if (signature === lastSnapshot) return;
    lastSnapshot = signature;
    onNewHazardReport(incidents);
  };

  const fetchAllIncidentPages = async () => {
    const incidents = [];
    let pageToken;

    do {
      const endpoint = new URL(`https://firestore.googleapis.com/v1/projects/${firebaseConfig.projectId}/databases/(default)/documents/incidents`);
      endpoint.searchParams.set('key', firebaseConfig.apiKey);
      endpoint.searchParams.set('pageSize', '1000');
      if (pageToken) endpoint.searchParams.set('pageToken', pageToken);

      const response = await fetch(endpoint);
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.error?.message || `Firestore REST read failed: ${response.status}`);

      incidents.push(...(payload.documents || []).map((document) => ({
        id: document.name.split('/').pop(),
        ...(document.createTime ? { createdAt: document.createTime } : {}),
        ...decodeFirestoreFields(document.fields || {})
      })).filter(hasIncidentData));
      pageToken = payload.nextPageToken;
    } while (pageToken && isActive);

    incidents.sort((left, right) => getIncidentSortTime(right) - getIncidentSortTime(left));
    return incidents;
  };

  const refreshFromServer = () => {
    return fetchAllIncidentPages()
      .then((incidents) => {
        if (!isActive) return;
        const signature = JSON.stringify(incidents);
        if (signature !== lastSnapshot) {
          lastSnapshot = signature;
          onNewHazardReport(incidents);
        }
        onSyncState({ connected: true, source: 'server', reportCount: incidents.length });
      })
      .catch((error) => {
        if (isActive) onSyncState({ connected: false, source: 'server', error });
        onError(error);
      });
  };

  const unsubscribe = onSnapshot(incidentsCollection, (snapshot) => {
    publishSnapshot(snapshot);
    if (!snapshot.metadata.fromCache) {
      onSyncState({ connected: true, source: 'realtime', reportCount: snapshot.size });
    }
  }, (error) => {
    onSyncState({ connected: false, source: 'realtime', error });
    onError(error);
  });

  refreshFromServer();
  const refreshTimer = setInterval(() => {
    if (isActive) refreshFromServer();
  }, 15000);

  return () => {
    isActive = false;
    clearInterval(refreshTimer);
    unsubscribe();
  };
}

export const subscribeToLiveIncidents = initSentinelLiveStream;

/**
 * 2. Allow Website Visitors to Upvote / Confirm an App Driver's Report
 */
export async function upvoteIncidentFromWeb(incidentId) {
  const incidentRef = doc(db, 'incidents', incidentId);
  await updateDoc(incidentRef, { upvotes: increment(1) });
}

/**
 * 3. Fetch Driver Impact Points Leaderboard for Website
 */
export function subscribeToDriverLeaderboard(onLeaderboardUpdate) {
  const q = query(collection(db, 'users'), orderBy('impactPoints', 'desc'));

  return onSnapshot(q, (snapshot) => {
    const drivers = snapshot.docs.map(doc => doc.data());
    onLeaderboardUpdate(drivers);
  });
}

function hasIncidentData(incident) {
  return Object.keys(incident).some((key) => key !== 'id' && key !== 'timestamp');
}