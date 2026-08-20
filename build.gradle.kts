// RunBuddy/build.gradle.kts
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false

    // ✅ Firebase용 Google Services 플러그인(루트에는 apply false)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
