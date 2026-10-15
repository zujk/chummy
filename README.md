# Chummy

A small Android app for keeping in touch with friends. Android take on
[Chumlord](https://morelandjs.github.io/chumlord-site/).

Add the people you want to stay in contact with, set a rough cadence for each
(weekly, monthly, whatever), and log a catchup when it happens. The list re-ranks
so the friend you're most overdue to call sits at the top.

Each friend is scored by days since last contact divided by their cadence, so a
week-late weekly friend outranks a month-late annual friend.

No accounts, no notifications, no streaks. Everything stays on your phone.

## Build
Open in Android Studio, or `gradle :app:assembleDebug`. CI builds a debug APK on every push
(Actions → Build APK → artifact `chummy-debug-apk`).
