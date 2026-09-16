plugins {
    alias(libs.plugins.cmp.feature.convention)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.ui)
            implementation(projects.core.model)

            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.koin.compose.navigation)
        }

        commonTest.dependencies {
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation("com.tom-roush:pdfbox-android:2.0.27.0")
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.jb.composeViewmodel)
        }
    }
}
