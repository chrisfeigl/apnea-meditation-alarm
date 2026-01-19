# Apnea Meditation Alarm

An Android app that combines an alarm clock with guided breath-hold (apnea) training using Tibetan bowl and chime sounds.

## Features

- **Wake-up Alarm**: Schedule daily alarms that wake you with a gentle Tibetan bowl sound
- **Breath-Hold Training**: Configurable breath-hold cycles with audio cues
- **Audio Cues**:
  - Intro bowl sound (with optional fade-in)
  - Hold chime to signal breath-hold start
  - Breath chime to signal breathing period
- **Custom Audio**: Replace any sound with your own audio files
- **Configurable Sessions**:
  - Breath-hold duration
  - Number of cycles
  - Breathing interval duration (with progressive shortening)
- **Snooze**: Configurable snooze duration
- **Background Sessions**: Sessions continue running as a foreground service

## How It Works

1. Set your wake-up alarm time and breath-hold duration in Settings
2. When the alarm triggers (or you start manually), the session begins:
   - Intro bowl fades in over ~48 seconds (optional)
   - 3-second countdown
   - Hold chime plays → Hold your breath
   - After the hold duration, breath chime plays → Breathe
   - Breathing intervals progressively shorten between cycles
   - After all cycles complete, continuous bowl plays until you stop

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
