#!/bin/sh
# ----------------------------------------------------------------------------
# Minimal Maven bootstrap wrapper.
#   - If `mvn` is already on PATH, delegate to it.
#   - Otherwise download a pinned Apache Maven into .mvn/ and use that.
# This keeps the repo buildable without a system Maven and without committing
# a binary wrapper jar.
# ----------------------------------------------------------------------------
set -e

MAVEN_VERSION="3.9.9"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "mvn not found on PATH; downloading Apache Maven $MAVEN_VERSION ..." >&2
  mkdir -p "$BASE_DIR/.mvn"
  TMP_TGZ="$(mktemp)"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$TMP_TGZ"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$TMP_TGZ"
  else
    echo "ERROR: need curl or wget to bootstrap Maven." >&2
    exit 1
  fi
  tar -xzf "$TMP_TGZ" -C "$BASE_DIR/.mvn"
  rm -f "$TMP_TGZ"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
