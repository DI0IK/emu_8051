{ pkgs, inputs, ... }:

let
  jdk = pkgs.openjdk21;
  
  # Fetch exactly what you need from the daily-updated repository
  androidSdk = inputs.android-nixpkgs.sdk.${pkgs.stdenv.hostPlatform.system} (sdkPkgs: with sdkPkgs; [
    cmdline-tools-latest
    build-tools-35-0-0
    build-tools-36-0-0
    platform-tools
    platforms-android-35
    platforms-android-36
    emulator
    system-images-android-35-google-apis-playstore-x86-64
  ]);
in {
  packages = [
    jdk
    pkgs.gradle
    pkgs.kotlin
    pkgs.nodejs
    pkgs.yarn
    pkgs.binaryen
    androidSdk
  ];

  env = {
    JAVA_HOME = "${jdk.home}";
    ANDROID_HOME = "${androidSdk}/share/android-sdk";
    ANDROID_SDK_ROOT = "${androidSdk}/share/android-sdk";
    GRADLE_OPTS = "-Dorg.gradle.java.home=${jdk.home} -Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/share/android-sdk/build-tools/36.0.0/aapt2";
    KOTLIN_USE_SYSTEM_NODE = "true";
  };

  enterShell = ''
    echo "☕ Kotlin & Android Development Environment Ready"
    echo "Java Version:   $(java -version 2>&1 | head -n 1)"
    echo "ANDROID_HOME:   $ANDROID_HOME"
  '';
}
