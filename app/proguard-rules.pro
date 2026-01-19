# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep data classes for DataStore
-keep class com.apneaalarm.data.** { *; }

# Keep session state classes
-keep class com.apneaalarm.session.SessionState { *; }
-keep class com.apneaalarm.session.SessionState$* { *; }

# Keep Material Icons
-keep class androidx.compose.material.icons.** { *; }
-dontwarn androidx.compose.material.icons.**
