#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
git_root="$(git -C "$repo_root" rev-parse --show-toplevel)"

if [[ "$git_root" != "$repo_root" ]]; then
  echo "ERROR: la raíz Git no coincide con la carpeta del proyecto." >&2
  echo "Proyecto: $repo_root" >&2
  echo "Git:      $git_root" >&2
  exit 1
fi

if git -C "$repo_root" check-ignore -q local.properties; then
  :
else
  echo "ERROR: local.properties debe permanecer ignorado." >&2
  exit 1
fi

if rg -n --hidden \
  -g '!.git/**' \
  -g '!.gradle/**' \
  -g '!**/build/**' \
  -g '!local.properties' \
  '(BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|AIza[0-9A-Za-z_-]{30,}|ghp_[0-9A-Za-z]{30,})' \
  "$repo_root"; then
  echo "ERROR: se detectó un posible secreto en el repositorio." >&2
  exit 1
fi

git -C "$repo_root" diff --check
git -C "$repo_root" diff --cached --check

cd "$repo_root"
./gradlew testDebugUnitTest lintDebug assembleDebug jacocoDebugReport --no-daemon

echo "Validación completada correctamente."
