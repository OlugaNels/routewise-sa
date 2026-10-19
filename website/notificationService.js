import { getMessaging, getToken, isSupported, onMessage } from 'https://www.gstatic.com/firebasejs/10.13.1/firebase-messaging.js';
import { app, firebaseConfigReady } from './firebase.js';

const VAPID_KEY = 'YOUR_FIREBASE_WEB_PUSH_VAPID_KEY';

async function getSupportedMessaging() {
  if (!firebaseConfigReady) throw new Error('Configure the Firebase Web app values before enabling alerts.');
  if (!('Notification' in window) || !('serviceWorker' in navigator)) {
    throw new Error('This browser does not support web push notifications.');
  }
  if (!(await isSupported())) {
    throw new Error('Firebase Messaging is not supported in this browser.');
  }
  return getMessaging(app);
}

export async function requestWebNotificationPermission() {
  try {
    if (!firebaseConfigReady) throw new Error('Configure the Firebase Web app values before enabling alerts.');
    if (VAPID_KEY === 'YOUR_FIREBASE_WEB_PUSH_VAPID_KEY') {
      throw new Error('Add the Web Push certificate key from Firebase Console before enabling alerts.');
    }

    let permission = Notification.permission;
    if (permission === 'default') permission = await Notification.requestPermission();
    if (permission !== 'granted') return null;

    const serviceWorkerRegistration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
    const messaging = await getSupportedMessaging();
    const token = await getToken(messaging, { vapidKey: VAPID_KEY, serviceWorkerRegistration });
    if (!token) throw new Error('Firebase did not return a web push token.');
    return token;
  } catch (error) {
    console.error('Could not enable web notifications:', error);
    throw error;
  }
}

export function listenToForegroundNotifications(onNotificationReceived) {
  let active = true;
  let unsubscribe;

  getSupportedMessaging().then((messaging) => {
    if (active) unsubscribe = onMessage(messaging, onNotificationReceived);
  }).catch((error) => {
    console.warn('Foreground web notifications are unavailable:', error.message);
  });

  return () => {
    active = false;
    if (unsubscribe) unsubscribe();
  };
}
