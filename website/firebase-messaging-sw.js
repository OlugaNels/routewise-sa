importScripts('https://www.gstatic.com/firebasejs/10.13.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.1/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'AIzaSyCE5Q6rYlBj1Urz9KCBKLz_RU61U3Nd5X0',
  authDomain: 'routewisesa-fcde8.firebaseapp.com',
  projectId: 'routewisesa-fcde8',
  storageBucket: 'routewisesa-fcde8.firebasestorage.app',
  messagingSenderId: '890979373130',
  appId: '1:890979373130:web:4a55b3ff71648244da16b3'
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const notificationTitle = payload.data?.title || payload.notification?.title || 'RouteWise SA Alert';
  const notificationOptions = {
    body: payload.data?.body || payload.notification?.body || 'Traffic alert along your corridor.',
    data: { url: payload.data?.url || '/' }
  };

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = new URL(event.notification.data?.url || '/', self.location.origin).href;
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
    const existingWindow = windows.find((client) => client.url === targetUrl);
    if (existingWindow) return existingWindow.focus();
    return self.clients.openWindow(targetUrl);
  }));
});
