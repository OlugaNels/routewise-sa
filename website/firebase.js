import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.13.1/firebase-app.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/10.13.1/firebase-firestore.js';

export const firebaseConfig = {
  apiKey: 'AIzaSyCE5Q6rYlBj1Urz9KCBKLz_RU61U3Nd5X0',
  authDomain: 'routewisesa-fcde8.firebaseapp.com',
  projectId: 'routewisesa-fcde8',
  storageBucket: 'routewisesa-fcde8.firebasestorage.app',
  messagingSenderId: '890979373130',
  appId: '1:890979373130:web:4a55b3ff71648244da16b3',
  measurementId: 'G-GWLKPGH4XP'
};

export const firebaseConfigReady = !Object.values(firebaseConfig).some((value) => value.startsWith('YOUR_'));
export const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
