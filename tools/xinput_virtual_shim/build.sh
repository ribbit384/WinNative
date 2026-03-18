#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC_DIR="$ROOT_DIR/tools/xinput_virtual_shim"
BUILD_DIR="$ROOT_DIR/build/xinput_virtual_shim"
ASSET_DIR="$ROOT_DIR/app/src/main/assets/wincomponents"
PACKAGE_ROOT="$BUILD_DIR/package"
ASSET_PATH="$ASSET_DIR/xinput_virtual.tzst"

DLL_NAMES=(
  xinput1_1.dll
  xinput1_2.dll
  xinput1_3.dll
  xinput1_4.dll
  xinput9_1_0.dll
  xinputuap.dll
)

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/x64" "$BUILD_DIR/x86" "$BUILD_DIR/arm64" "$BUILD_DIR/arm64ec"
mkdir -p "$PACKAGE_ROOT/system32" "$PACKAGE_ROOT/syswow64" "$PACKAGE_ROOT/sysarm64" "$PACKAGE_ROOT/sysarm64ec" "$ASSET_DIR"

# x64
if command -v x86_64-w64-mingw32-gcc >/dev/null; then
  x86_64-w64-mingw32-gcc -O2 -s -shared -Wall -Wextra -Wl,--kill-at "$SRC_DIR/xinput_virtual.c" "$SRC_DIR/xinput_virtual.def" -o "$BUILD_DIR/x64/xinput_virtual.dll"
  for dll in "${DLL_NAMES[@]}"; do cp "$BUILD_DIR/x64/xinput_virtual.dll" "$PACKAGE_ROOT/system32/$dll"; done
else
  echo "Warning: x86_64-w64-mingw32-gcc not found, skipping x64 build."
fi

# x86
if command -v i686-w64-mingw32-gcc >/dev/null; then
  i686-w64-mingw32-gcc -O2 -s -shared -Wall -Wextra -Wl,--kill-at "$SRC_DIR/xinput_virtual.c" "$SRC_DIR/xinput_virtual.def" -o "$BUILD_DIR/x86/xinput_virtual.dll"
  for dll in "${DLL_NAMES[@]}"; do cp "$BUILD_DIR/x86/xinput_virtual.dll" "$PACKAGE_ROOT/syswow64/$dll"; done
else
  echo "Warning: i686-w64-mingw32-gcc not found, skipping x86 build."
fi

# arm64 (Standard ARM64 Windows)
if command -v aarch64-w64-mingw32-gcc >/dev/null; then
  aarch64-w64-mingw32-gcc -O2 -s -shared -Wall -Wextra -Wl,--kill-at "$SRC_DIR/xinput_virtual.c" "$SRC_DIR/xinput_virtual.def" -o "$BUILD_DIR/arm64/xinput_virtual.dll"
  for dll in "${DLL_NAMES[@]}"; do cp "$BUILD_DIR/arm64/xinput_virtual.dll" "$PACKAGE_ROOT/sysarm64/$dll"; done
else
  echo "Warning: aarch64-w64-mingw32-gcc not found, skipping arm64 build."
fi

# arm64ec (Emulation Compatible ARM64)
# Requires LLVM/Clang with -arm64ec target support
if command -v clang >/dev/null && clang -print-targets | grep -q arm64ec; then
  clang -target arm64ec-pc-windows-msvc -O2 -shared -Wall -Wextra "$SRC_DIR/xinput_virtual.c" "$SRC_DIR/xinput_virtual.def" -o "$BUILD_DIR/arm64ec/xinput_virtual.dll"
  for dll in "${DLL_NAMES[@]}"; do cp "$BUILD_DIR/arm64ec/xinput_virtual.dll" "$PACKAGE_ROOT/sysarm64ec/$dll"; done
else
  echo "Warning: arm64ec-aware clang not found, skipping arm64ec build."
fi

if [ -d "$PACKAGE_ROOT" ]; then
  tar -C "$PACKAGE_ROOT" -cf "$BUILD_DIR/xinput_virtual.tar" .
  zstd -19 -f "$BUILD_DIR/xinput_virtual.tar" -o "$ASSET_PATH"
  echo "Built $ASSET_PATH"
else
  echo "Error: No DLLs were built."
  exit 1
fi
