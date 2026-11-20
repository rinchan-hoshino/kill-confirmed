#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_NAME="kill-confirmed-verify:local"

USE_CONTAINER=0
if [[ "${1:-}" == "--container" ]]; then
    USE_CONTAINER=1
    shift
fi

discover_jdk() {
    local major="$1"
    local env_var="JAVA_${major}_HOME"
    if [[ -n "${!env_var:-}" && -x "${!env_var}/bin/java" ]]; then
        echo "${!env_var}"
        return 0
    fi
    local candidates=(
        "/opt/homebrew/opt/openjdk@${major}/libexec/openjdk.jdk/Contents/Home"
        "/Users/rin/Library/Java/JavaVirtualMachines/temurin-${major}.jdk/Contents/Home"
        "/usr/lib/jvm/temurin-${major}-jdk-amd64"
        "/usr/lib/jvm/temurin-${major}-jdk-arm64"
    )
    for c in "${candidates[@]}"; do
        if [[ -x "${c}/bin/java" ]]; then
            echo "${c}"
            return 0
        fi
    done
    for p in /opt/homebrew/Cellar/openjdk@"${major}"/*/libexec/openjdk.jdk/Contents/Home "${HOME}"/.gradle/jdks/*-"${major}"-*/Contents/Home; do
        if [[ -x "${p}/bin/java" ]]; then
            echo "${p}"
            return 0
        fi
    done
    return 1
}

run_in_container() {
    local runtime=""
    if command -v docker >/dev/null 2>&1; then
        runtime="docker"
    elif command -v podman >/dev/null 2>&1; then
        runtime="podman"
    else
        echo "ERROR: Container mode requested (or missing local JDKs) but neither docker nor podman is available." >&2
        exit 1
    fi
    echo "==> Building verification container image (${IMAGE_NAME})..."
    "${runtime}" build -f "${ROOT}/Dockerfile.verify" -t "${IMAGE_NAME}" "${ROOT}"
    echo "==> Running full verification suite inside container..."
    exec "${runtime}" run --rm --init \
        -v "${ROOT}:/workspace" \
        -v "rinchan-gradle-cache:/root/.gradle" \
        -w /workspace \
        "${IMAGE_NAME}" \
        ./scripts/verify.sh
}

if [[ "${USE_CONTAINER}" -eq 1 ]]; then
    run_in_container
fi

if ! JAVA_17_HOME="$(discover_jdk 17)" || ! JAVA_21_HOME="$(discover_jdk 21)" || ! JAVA_25_HOME="$(discover_jdk 25)"; then
    echo "==> Local JDK 17/21/25 not all present; falling back to containerized verification..."
    run_in_container
fi

export JAVA_17_HOME JAVA_21_HOME JAVA_25_HOME
cd "${ROOT}"

echo "==> [1/3] Building all 8 matrix targets (build-matrix.py)..."
python3 scripts/build-matrix.py

echo "==> [2/3] Running Property, ScreenLifecycle & full 8-JAR ArchitectureContract gates..."
JAVA_HOME="${JAVA_21_HOME}" ./gradlew :neoforge:test \
    -Pminecraft_version=1.21.1 \
    -Ploader=neoforge \
    -Dkillconfirmed.scanAllReleases=true \
    --no-configuration-cache \
    --rerun-tasks

echo "==> [3/3] Running Packaged-JAR E2E GameTest Server (1.21.1 NeoForge)..."
JAVA_HOME="${JAVA_21_HOME}" ./gradlew :neoforge:runGameTestServer \
    -Pminecraft_version=1.21.1 \
    -Ploader=neoforge \
    --no-configuration-cache

echo "==> ALL VERIFICATION GATES PASSED."
