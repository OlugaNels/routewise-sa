# RouteWise SA

Real-time road alert Android app for Gauteng, South Africa. Built with Kotlin, Firebase Authentication and Firestore, Room, and OpenStreetMap.

**Module:** XISD6329w | **Group 2** | Task 2: Mobile App Development

## Team

| Name | Student number | Role |
|---|---|---|
| Neo Madima | ST10444163 | Lead Developer |
| Mufhumudzi Rasilingwana | ST10441516 | Database |
| Oluga Neluvhalani | ST10443034 | Project Manager |
| Phathutshedzo Kevin Makherana | ST10438610 | UX / Tester |

## Live links

- Website and API (Render): https://routewise-sentinel.onrender.com (API example: `/api/incidents/live`)
- Firebase Hosting (static page only): https://routewisesa-fcde8.web.app
- GitHub Actions: https://github.com/OlugaNels/routewise-sa/actions

## Features

- Register and log in with email and password (Firebase Authentication, passwords hashed by Firebase)
- Sign in with Google (single sign-on through Firebase)
- Forgot password by emailed reset link
- Live map, alert feed, route planning
- Report a road incident (type, road, severity, description)
- Settings (units and alert preferences) saved on the device
- Input checks on every form so wrong input shows a message instead of crashing

## Build and test

Requirements: JDK 17 or newer (Android Studio's bundled JDK works), Android SDK with platform 34.

```
./gradlew testDebugUnitTest   # run the unit tests
./gradlew assembleDebug       # build app/build/outputs/apk/debug/app-debug.apk
```

Every push runs both steps in GitHub Actions: see `.github/workflows/android-build.yml`.

## Firebase setup needed

1. Firebase console, Authentication, Sign-in method: enable **Email/Password** and **Google**.
2. Project settings, your Android app (`com.routewise.sa`): add the **SHA-1** of every key that signs an APK you install (your debug key, and the one used by CI if you install CI builds), then download a fresh `google-services.json` into `app/`.

## Layout

- `app/src/main/java/com/routewise/sa/` app source (`ui/`, `data/`, `util/`)
- `app/src/test/` unit tests (`InputValidatorTest`)
- `website/` Sentinel Command Center admin website (Node.js server, Firebase)
- `.github/workflows/` GitHub Actions build and test workflow
