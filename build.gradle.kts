@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}

// On NixOS (devenv.nix sets KOTLIN_USE_SYSTEM_NODE=true), Kotlin-downloaded Node.js / Binaryen
// binaries fail under stub-ld; use the Nixpkgs-provided toolchain on PATH instead.
if (providers.environmentVariable("KOTLIN_USE_SYSTEM_NODE").orNull == "true") {
    plugins.withType<NodeJsRootPlugin> {
        extensions.configure<NodeJsEnvSpec> { download.set(false) }
    }
    plugins.withType<WasmNodeJsRootPlugin> {
        extensions.configure<WasmNodeJsEnvSpec> { download.set(false) }
    }
    plugins.withType<YarnPlugin> {
        extensions.configure<YarnRootEnvSpec> { download.set(false) }
    }
    plugins.withType<WasmYarnPlugin> {
        extensions.configure<WasmYarnRootEnvSpec> { download.set(false) }
    }
    plugins.withType<BinaryenPlugin> {
        extensions.configure<BinaryenEnvSpec> { download.set(false) }
    }
    allprojects {
        plugins.withType<NodeJsPlugin> {
            extensions.configure<NodeJsEnvSpec> { download.set(false) }
        }
        plugins.withType<WasmNodeJsPlugin> {
            extensions.configure<WasmNodeJsEnvSpec> { download.set(false) }
        }
    }
}