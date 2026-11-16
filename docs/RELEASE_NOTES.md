# RouteWise SA release notes

Module XISD6329w, Group 2. The prototype is version 1.0.0, the build submitted for Task 2.

## 1.2.0 (final submission, Task 3)

Download: the APK attached to the v1.2.0 release on GitHub.

### Innovative features added since the prototype

| Feature | What it does | Where in the code |
|---|---|---|
| Hazard notifications | A heads-up notification appears when another driver reports a new hazard. Existing incidents are ignored when the app opens, and at most three notifications are shown per update. | `service/HazardNotifier.kt`, `service/AlertNotifications.kt`, started from `MainActivity` |
| Hazard alert switches | The Live Hazard Layer, Accident, Police and Roadwork switches change what the map and the alerts feed show immediately. | `util/HazardFilter.kt`, `ui/map/MapFragment.kt`, `ui/alerts/LiveAlertsFragment.kt` |
| Change password | Settings, Account and Security. Re-authenticates with the current password, validates the new one, then saves it through Firebase Authentication. Google accounts see an explanation instead. | `ui/settings/SettingsFragment.kt`, `InputValidator.changePasswordError` |
| Notification permission prompt | On Android 13 and newer, switching Hazard Notifications on asks for the notification permission. | `ui/settings/SettingsFragment.kt` |
| Signed release build | `assembleRelease` signs the APK with a private release key kept outside the repository. | `app/build.gradle.kts` |

### Other changes

- Logging added to sign in and registration, Google sign-in, password reset, change password, profile, hazard reporting, route calculation and re-routing, settings, push messages and hazard notifications. Passwords, emails and device tokens are never logged.
- Push messages respect the Hazard Notifications switch.
- Removed the "Speed Warnings" switch: the app has no real speed-limit data.
- Comments and references added to the main classes.
- Added 15 unit tests (`HazardFilterTest`, new `InputValidatorTest` cases). All 76 pass.
- Version code 2, version name 1.2.0.

### Response to Task 1 and Task 2 feedback

| Feedback | Response |
|---|---|
| Task 2: the video did not show changing passwords and editing the user profile | Change password and an "Edit my profile" shortcut are now in Settings |
| Task 2: alert notifications were not shown and toggles did not show changes at runtime | Hazard notification and alert switches now change the map, the feed and the notifications at once |
| Task 1: List of Figures and sitemaps | Added to the final report |

### Known limitations

- Hazard notifications appear while the app is running. Alerts to a closed app need Firebase Cloud Messaging topics sent from the Sentinel website.
- The speed-limit badge on the navigation screen is an estimate.

## 1.1.0 (map and navigation update)

- Clearer street map, sharper tiles, satellite view, night map after dark
- Real turn-by-turn text, real ETA and distance
- Follows the GPS between steps and re-routes if the driver leaves the route
- Real distance and drive time on the place card
- Error messages instead of made-up routes

## 1.0.0 (prototype, Task 2)

Registration, login with email and Google, forgot password, live map, alerts feed, hazard reporting, settings, profile, Sentinel admin website, 32 unit tests and the GitHub Actions pipeline.
