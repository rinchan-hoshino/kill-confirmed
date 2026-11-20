#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh
echo "Installed git hooks from .githooks/ (core.hooksPath=.githooks)"
