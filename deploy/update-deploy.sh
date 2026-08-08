#!/usr/bin/env bash
set -euo pipefail
# =============================================================================
# update-deploy.sh — Sync atomique de deploy/ depuis GitHub
# =============================================================================
# NHUB_GITHUB_REPO=owner/repo (défaut: AQUILA04/notification-hub)
# =============================================================================

REPO="${NHUB_GITHUB_REPO:-AQUILA04/notification-hub}"
ROOT="/opt/notification-hub"

echo ">>> [update-deploy] Fetching latest deploy scripts from GitHub ($REPO)..."
rm -rf /tmp/notification-hub_src
git clone --depth 1 "https://github.com/${REPO}.git" /tmp/notification-hub_src > /dev/null 2>&1

echo ">>> [update-deploy] Applying new scripts..."
rm -rf "$ROOT/deploy.new"
cp -r /tmp/notification-hub_src/deploy "$ROOT/deploy.new"
rm -rf /tmp/notification-hub_src

chmod +x "$ROOT/deploy.new"/*.sh 2>/dev/null || true

BACKUP_DIR="$ROOT/deploy.old_$(date +%s)"
if [[ -d "$ROOT/deploy" ]]; then
  mv "$ROOT/deploy" "$BACKUP_DIR"
  echo ">>> [update-deploy] Old scripts backed up in $BACKUP_DIR"
fi
mv "$ROOT/deploy.new" "$ROOT/deploy"

echo ">>> [update-deploy] Update complete!"
