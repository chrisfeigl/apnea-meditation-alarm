# Apnea Meditation Alarm

A zen-inspired Android app that combines an alarm clock with guided breath-hold (apnea) training using Tibetan bowl and chime sounds.

## Features

- **Wake-up Alarm**: Schedule daily alarms that wake you with a gentle Tibetan bowl sound
- **Breath-Hold Training**: Configurable breath-hold cycles with audio cues
- **Audio Cues**:
  - Intro bowl sound (with optional fade-in over 48 seconds)
  - Hold chime to signal breath-hold start
  - Breath chime to signal breathing period
- **Custom Audio**: Replace any sound with your own audio files
- **Configurable Sessions**:
  - Breath-hold duration
  - Number of cycles (default 10)
  - Breathing interval duration (with progressive shortening)
- **Session Completion**:
  - Continuous bowl at max volume when session ends
  - Escalates to repeated chimes after 3 minutes if not stopped
- **Snooze**: Configurable snooze duration
- **Background Sessions**:
  - Sessions continue as a foreground service
  - Stop button in notification works even when app is closed
  - Return to active session from home screen
- **Zen Theme**: Calming color palette inspired by Buddhist aesthetics
  - Deep twilight indigo and warm gold (dark mode)
  - Warm parchment and earthy browns (light mode)

## How It Works

1. Set your wake-up alarm time and breath-hold duration in Settings
2. When the alarm triggers (or you start manually), the session begins:
   - Intro bowl fades in over ~48 seconds (configurable)
   - 3-second countdown
   - Hold chime plays → Hold your breath
   - After the hold duration, breath chime plays → Breathe
   - Breathing intervals progressively shorten between cycles
   - After all cycles complete, continuous bowl plays at max volume
   - If not stopped within 3 minutes, switches to repeated chimes
3. Press STOP in the app or notification to end the session

## Screenshots

The app features a calming zen aesthetic with:
- Warm gold/amber accents (like temple bells)
- Sage green highlights (nature)
- Soft lavender touches (meditation)

## Building

### Requirements
- Android Studio or command line with Android SDK
- JDK 17+
- Android SDK 34

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK
```bash
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/`

## Permissions

- **Alarms**: Schedule exact alarms for wake-up
- **Notifications**: Show foreground service notification during sessions
- **Wake Lock**: Keep CPU active during sessions

## License

MIT
