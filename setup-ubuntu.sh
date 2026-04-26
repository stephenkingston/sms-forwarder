#!/usr/bin/env bash
#
# One-shot toolchain installer for building SMS Forwarder on Ubuntu 24.04.
# Installs JDK 17, Android command-line SDK, and bootstraps the Gradle wrapper.
#
# Re-running is safe — each step is idempotent.

set -euo pipefail

ANDROID_SDK_DIR="${ANDROID_SDK_DIR:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708_latest"
GRADLE_VERSION="8.10.2"
ANDROID_PLATFORM="android-35"
ANDROID_BUILD_TOOLS="35.0.0"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

step()  { printf "\n\033[1;36m==>\033[0m \033[1m%s\033[0m\n" "$1"; }
info()  { printf "    %s\n" "$1"; }
done_() { printf "    \033[1;32mok\033[0m\n"; }

step "1/5  Installing system packages (JDK 17, unzip, curl)"
if ! command -v javac >/dev/null 2>&1 || ! javac -version 2>&1 | grep -q '17'; then
    sudo apt-get update -y
    sudo apt-get install -y openjdk-17-jdk-headless unzip curl
else
    info "JDK 17 already installed"
fi
done_

step "2/5  Installing Android command-line tools to $ANDROID_SDK_DIR"
mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
if [ ! -d "$ANDROID_SDK_DIR/cmdline-tools/latest" ]; then
    info "Downloading commandlinetools-linux-${CMDLINE_TOOLS_VERSION}.zip"
    TMP_ZIP="$(mktemp --suffix=.zip)"
    curl -fL --progress-bar \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}.zip" \
        -o "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$ANDROID_SDK_DIR/cmdline-tools"
    mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
    rm -f "$TMP_ZIP"
else
    info "Already present at $ANDROID_SDK_DIR/cmdline-tools/latest"
fi
done_

export ANDROID_HOME="$ANDROID_SDK_DIR"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/javac" ]; then
    if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    elif command -v javac >/dev/null 2>&1; then
        export JAVA_HOME="$(readlink -f "$(command -v javac)" | sed 's|/bin/javac||')"
    fi
fi

step "3/5  Installing Android SDK packages and accepting licenses"
info "JAVA_HOME=${JAVA_HOME:-<unset>}"
info "sdkmanager: $(command -v sdkmanager)"
info "Refreshing package index (first run downloads several MB)"
sdkmanager --update
info "Accepting licenses (printing 'y' to each — output is intentionally visible)"
yes | sdkmanager --licenses
info "Installing platform-tools, ${ANDROID_PLATFORM}, build-tools;${ANDROID_BUILD_TOOLS}"
sdkmanager --install \
    "platform-tools" \
    "platforms;${ANDROID_PLATFORM}" \
    "build-tools;${ANDROID_BUILD_TOOLS}"
done_

step "4/5  Bootstrapping Gradle wrapper"
cd "$SCRIPT_DIR"
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    TMP_GRADLE_DIR="/tmp/gradle-${GRADLE_VERSION}"
    if [ ! -x "$TMP_GRADLE_DIR/bin/gradle" ]; then
        info "Downloading gradle-${GRADLE_VERSION}-bin.zip"
        TMP_GRADLE_ZIP="$(mktemp --suffix=.zip)"
        curl -fL --progress-bar \
            "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
            -o "$TMP_GRADLE_ZIP"
        unzip -q "$TMP_GRADLE_ZIP" -d /tmp
        rm -f "$TMP_GRADLE_ZIP"
    fi
    info "Generating wrapper jar via gradle wrapper task"
    "$TMP_GRADLE_DIR/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION" >/dev/null
    chmod +x "$SCRIPT_DIR/gradlew"
else
    info "gradle-wrapper.jar already present"
fi
done_

step "5/5  Writing local.properties"
cat > "$SCRIPT_DIR/local.properties" <<EOF
sdk.dir=$ANDROID_HOME
EOF
done_

cat <<EOF

\033[1;32mSetup complete.\033[0m

To build the debug APK:

    export ANDROID_HOME=$ANDROID_HOME
    export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH
    ./gradlew assembleDebug

The APK will be at:

    app/build/outputs/apk/debug/app-debug.apk

To install on a connected phone (USB debugging enabled):

    adb install -r app/build/outputs/apk/debug/app-debug.apk

Or transfer the APK to the phone and tap to install.

EOF
