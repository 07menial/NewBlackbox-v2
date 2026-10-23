plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

// Proprietati citite de modulul Bcore (rootProject.ext.*)
extra["compileSdkVersion"] = 35
extra["minSdk"] = 21
