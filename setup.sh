#!/usr/bin/env bash
set -euo pipefail

# Sets up the Android SDK needed to build this repository on Linux.
#
# Usage:
#   scripts/setup-android-env.sh
#   source .android-env
#   gradle :app:assembleDebug
#
# Environment overrides:
#   ANDROID_HOME=/custom/sdk/path scripts/setup-android-env.sh
#   ANDROID_API_LEVEL=35 scripts/setup-android-env.sh
#   ANDROID_BUILD_TOOLS_VERSION=35.0.0 scripts/setup-android-env.sh
#   JAVA_HOME=/path/to/jdk17 scripts/setup-android-env.sh

ANDROID_API_LEVEL="${ANDROID_API_LEVEL:-35}"
ANDROID_BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS_VERSION:-35.0.0}"
COMMAND_LINE_TOOLS_VERSION="${COMMAND_LINE_TOOLS_VERSION:-11076708}"
COMMAND_LINE_TOOLS_ZIP="commandlinetools-linux-${COMMAND_LINE_TOOLS_VERSION}_latest.zip"
COMMAND_LINE_TOOLS_URL="https://dl.google.com/android/repository/${COMMAND_LINE_TOOLS_ZIP}"

if [[ -z "${ANDROID_HOME:-}" ]]; then
  if [[ -w /opt || ( ! -e /opt/android-sdk && -w /opt ) ]]; then
    ANDROID_HOME="/opt/android-sdk"
  else
    ANDROID_HOME="${HOME}/android-sdk"
  fi
fi

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

need_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

find_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return 0
  fi

  local candidate
  for candidate in \
    "${HOME}/.local/share/mise/installs/java/17.0.2" \
    "${HOME}/.sdkman/candidates/java/current" \
    "/usr/lib/jvm/java-17-openjdk-amd64" \
    "/usr/lib/jvm/temurin-17-jdk-amd64"; do
    if [[ -x "${candidate}/bin/java" ]]; then
      JAVA_HOME="${candidate}"
      return 0
    fi
  done

  if command -v java >/dev/null 2>&1; then
    JAVA_HOME="$(cd "$(dirname "$(readlink -f "$(command -v java)")")/.." && pwd)"
    return 0
  fi

  echo "Could not find Java. Install JDK 17 or set JAVA_HOME before running this script." >&2
  exit 1
}

need_command curl
need_command unzip
find_java_home

export ANDROID_HOME
export ANDROID_SDK_ROOT
export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

mkdir -p "${ANDROID_HOME}/cmdline-tools"

if [[ ! -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" ]]; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "${tmp_dir}"' EXIT

  echo "Downloading Android command-line tools from ${COMMAND_LINE_TOOLS_URL}"
  curl -fsSLo "${tmp_dir}/${COMMAND_LINE_TOOLS_ZIP}" "${COMMAND_LINE_TOOLS_URL}"
  unzip -q "${tmp_dir}/${COMMAND_LINE_TOOLS_ZIP}" -d "${tmp_dir}"

  rm -rf "${ANDROID_HOME}/cmdline-tools/latest"
  mkdir -p "${ANDROID_HOME}/cmdline-tools/latest"
  mv "${tmp_dir}/cmdline-tools/"* "${ANDROID_HOME}/cmdline-tools/latest/"
fi

echo "Accepting Android SDK licenses"
yes | sdkmanager --sdk_root="${ANDROID_HOME}" --licenses >/dev/null || true

echo "Installing Android SDK packages"
sdkmanager --sdk_root="${ANDROID_HOME}" \
  "cmdline-tools;latest" \
  "platform-tools" \
  "platforms;android-${ANDROID_API_LEVEL}" \
  "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"

cat > "${REPO_ROOT}/local.properties" <<PROPERTIES
sdk.dir=${ANDROID_HOME}
PROPERTIES

cat > "${REPO_ROOT}/.android-env" <<ENV
export ANDROID_HOME=${ANDROID_HOME}
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT}
export JAVA_HOME=${JAVA_HOME}
export PATH=${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:\$PATH
ENV

cat <<SUMMARY

Android environment is ready.

SDK:       ${ANDROID_HOME}
Java home: ${JAVA_HOME}
Repo:      ${REPO_ROOT}

For future shells, run:
  source ${REPO_ROOT}/.android-env

Then build with:
  gradle :app:assembleDebug
SUMMARY
