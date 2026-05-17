import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "dev.dominikstahl.emu_8051.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.dominikstahl.emu_8051"
            packageVersion = project.property("versionName").toString()

            macOS {
                iconFile.set(project.file("desktopIcons/icon.icns"))
            }
            windows {
                iconFile.set(project.file("desktopIcons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("desktopIcons/icon.png"))
            }
        }
    }
}