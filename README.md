# ReminderApp

A learning project: a small Kotlin + Jetpack Compose reminder app. The UI is simple on purpose. The point is **Android system design** — how a reminder survives app close, process death, and device reboot.

**Problem:** How do we reliably schedule reminders that survive app closure, process death, and device reboot?

**Answer in this app:**

1. **Room is the source of truth.** Persist the reminder first.
2. **AlarmManager is only a wake-up timer.** Schedule after insert, using the Room id as `PendingIntent` requestCode.
3. **On reboot and on every process start,** read `PENDING` rows from Room and schedule them again.

---

## How to run

### 1. Open the project

1. Install [Android Studio](https://developer.android.com/studio) (latest stable).
2. **File → Open** and select this folder (`ReminderApp`).
3. Wait for Gradle sync to finish.

### 2. Pick a device

- An **Android emulator** (API 24+) or a physical phone with USB debugging.
- Notifications and exact alarms are easier to judge on a **real device**.
- For reboot tests you need a device or emulator you can restart.

### 3. Install and launch

1. Click **Run** (green play button) or press `Ctrl+R` / `⌘R`.
2. Select the app module `:app`.
3. On first launch, Android 13+ will ask for **notification permission**. Tap **Allow**.
4. If a banner says exact alarms are off, tap **Settings** and allow the app to schedule exact alarms.

From the command line:

```bash
./gradlew :app:installDebug
```

Then open **ReminderApp** on the device.

---

## How to use the app

### Create a reminder

1. Tap the **+** button.
2. Enter a title.
3. Tap the date button → pick a date → **OK**.
4. Tap the time button → pick a time a few minutes in the future → **OK**.
5. Tap **Save**.
6. You return to the list. Status is `PENDING`.

Saving a time in the past, or a blank title, shows an error and writes nothing.

### Complete or cancel

On a `PENDING` card:

- **Mark Complete** → status `COMPLETED`, alarm cancelled.
- **Cancel** → status `CANCELLED`, alarm cancelled.

The row stays in the list so you can see history.

### Edit (optional)

1. Tap a `PENDING` card.
2. Change title and/or date/time.
3. Tap **Save**.

The old alarm is cancelled, Room is updated, then a new alarm is scheduled with the **same id**.

### Notification actions

When the alarm fires you get a notification with the title plus:

- **Mark Complete**
- **Cancel**

Those update Room even if you never open the app.

---

## How to verify the system design

Do these in order. They are the actual constraints this project is built around.

### 1. Notification while the app is closed

1. Create a reminder 1–2 minutes ahead.
2. Press Home (do not Force Stop).
3. Wait. You should get a notification.
4. Tap **Mark Complete**. Reopen the app — status is `COMPLETED`.

### 2. Process death

1. Create a reminder a few minutes ahead.
2. In Android Studio, click **Stop** (red square) to kill the process.
3. Reopen the app. The row is still there (Room).
4. Wait for the time. The notification should still appear (AlarmManager is OS-owned).

### 3. Device reboot

1. Create a reminder several minutes ahead.
2. Reboot the phone/emulator.
3. Unlock after boot. Wait for the scheduled time.
4. The notification should still appear (`BootReceiver` rebuilt the alarm from Room).

### 4. Two reminders at the same time

Create two reminders for the same minute. Both should notify. Each alarm uses `requestCode = reminder.id`.

### 5. Cancel vs late alarm

Create a reminder, then tap **Cancel** before it fires. You should not get a notification. If the alarm was already being delivered, `ReminderReceiver` re-reads Room and skips non-`PENDING` rows.

### 6. Notification permission denied (Android 13+)

1. Deny notifications (or revoke in system Settings).
2. Create a reminder anyway — it still saves.
3. A banner is shown on the list.
4. When the time hits, the app must not crash (`notify()` is caught).

### 7. Exact alarm permission denied (Android 12+)

1. In system Settings, turn off exact alarms for ReminderApp.
2. Create a reminder. A banner is shown.
3. The reminder is still saved. The alarm may fire **late** (inexact fallback).

---

## How to run tests

Unit tests are JVM tests. They use fakes for Room and AlarmManager — no emulator required.

```bash
./gradlew :app:testDebugUnitTest
```

In Android Studio: right-click `app/src/test` → **Run Tests**.

| Test | What it proves |
|------|----------------|
| `CreateReminderUseCaseTest` | Persist then schedule; blank title; time in the past; two reminders at the same timestamp get distinct ids |
| `DeleteReminderUseCaseTest` | Status becomes `CANCELLED` and the alarm is cancelled |
| `CompleteReminderUseCaseTest` | Status becomes `COMPLETED` and the alarm is cancelled |
| `UpdateReminderUseCaseTest` | Room update then alarm replace; past time does not write |
| `RescheduleRemindersUseCaseTest` | After "reboot", only future `PENDING` rows are scheduled; same id is not duplicated |

`ReminderScheduler` is an interface so tests can use `FakeReminderScheduler`.

---

## Architecture

```
app/src/main/java/com/reminder/app/
├── data/            Room + repository implementation
├── domain/          models, repository contract, use cases
├── scheduler/       ReminderScheduler + AlarmManager implementation
├── receiver/        alarm, boot, notification-action BroadcastReceivers
├── notification/    NotificationCompat + channel
├── presentation/    Compose screens + ViewModels (StateFlow)
└── di/              Hilt modules
```

```
Compose UI
    → ViewModel (StateFlow)
        → Use case
            → Room (source of truth)
            → ReminderScheduler (AlarmManager)   // after persist
                → ReminderReceiver
                    → re-read Room
                    → notification if still PENDING
```

MVVM + a thin use-case layer. No `BaseViewModel`. Hilt constructor injection.

### Scheduling flow

```
Save
  → CreateReminderUseCase
       1. Reject blank title / time in the past
       2. repository.create(...)      // Room INSERT, get id
       3. scheduler.schedule(...)     // AlarmManager
  → PendingIntent requestCode = reminder.id
       FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
  → at triggerTimeMillis
  → ReminderReceiver.goAsync()
       load Room; if PENDING → show notification
```

Persist **then** schedule. If the process dies after insert, the next app start / reboot recovery schedules from Room.

### Reboot recovery flow

```
BOOT_COMPLETED
  → BootReceiver
  → RescheduleRemindersUseCase
       PENDING rows where triggerTimeMillis > now
       scheduler.schedule(each)
```

The same use case runs in `ReminderApplication.onCreate`. That covers **force-stop**: Android wipes alarms and will not deliver `BOOT_COMPLETED` until the user opens the app.

Scheduling the same id twice **replaces** the previous alarm (no duplicates).

---

## Why Room is the source of truth

| Need | AlarmManager | Room |
|------|--------------|------|
| List reminders in the UI | Cannot query | `Flow` from DAO |
| Survive process death | Yes (OS-owned) | Yes (on disk) |
| Survive reboot | **No** | Yes |
| Know cancelled vs pending | No | `ReminderStatus` |
| Edit the time | Cancel + reschedule | Update row, then reschedule |

The UI never treats in-memory state as truth. `ReminderListViewModel` collects `observeReminders()`. After process death, a new process collects again.

When an alarm fires, `ReminderReceiver` **re-reads Room**. Cancelled/completed rows do not notify.

---

## AlarmManager trade-offs

| Approach | Process death | Exact wall-clock | Reboot | Fit for reminders? |
|----------|---------------|------------------|--------|--------------------|
| Coroutine `delay` | No | While alive | No | No |
| `Handler` | No | While alive | No | No |
| WorkManager | Yes | No (deferrable) | Yes | Poor |
| AlarmManager | Yes | Yes, with caveats | **No** (need `BootReceiver`) | Yes |

WorkManager is for deferrable work. A reminder is a wall-clock event. This app uses `AlarmManager.RTC_WAKEUP`.

### Exact vs inexact

- **Exact** (`setExactAndAllowWhileIdle`): near the chosen time, including Doze. Best UX.
- **Inexact** (`setAndAllowWhileIdle`): may be batched; can be minutes late; rate-limited in idle.

This app never assumes exact alarms are available:

- API 31+: `canScheduleExactAlarms()`. If false → inexact fallback + list banner.
- API 23–30: `setExactAndAllowWhileIdle` (no special permission).

Android 14+ denies `SCHEDULE_EXACT_ALARM` by default. This project does **not** declare `USE_EXACT_ALARM`, so the fallback path stays real.

---

## Permissions

| Permission | Why |
|------------|-----|
| `POST_NOTIFICATIONS` | Android 13+ runtime permission. Without it, `notify()` throws. We catch that. |
| `SCHEDULE_EXACT_ALARM` | Android 12+ special app-op (not a normal runtime permission). |
| `RECEIVE_BOOT_COMPLETED` | Lets `BootReceiver` run after reboot. |

Receivers:

- `ReminderReceiver` / `ReminderActionReceiver`: `exported="false"`.
- `BootReceiver`: `exported="true"` so the system can send `BOOT_COMPLETED`.

---

## Edge cases (implemented)

1. **Time in the past** — rejected; nothing written.
2. **Two reminders at the same time** — distinct `requestCode`s (`reminder.id`).
3. **Reboot before trigger** — `BootReceiver` restores future `PENDING` rows.
4. **Process killed** — Room + OS-owned alarm; reschedule on next start as a safety net.
5. **User cancels** — Room `CANCELLED`, then cancel alarm.
6. **User completes** — Room `COMPLETED`, then cancel alarm.
7. **Alarm fires after cancel** — receiver re-reads Room; skips non-`PENDING`.
8. **Exact alarm unavailable** — inexact fallback + banner.
9. **Notification permission denied** — save still works; `notify()` does not crash.
10. **Time edited** — update Room, cancel old alarm, schedule new one with the same id.

**Force-stop:** alarms are cleared and `BOOT_COMPLETED` will not run until the user opens the app. Reschedule-on-start handles this.

---

## Data model

```kotlin
Reminder(
    id: Long,
    title: String,
    triggerTimeMillis: Long,
    status: ReminderStatus  // PENDING | COMPLETED | CANCELLED
)
```
