#!/bin/sh
# ----------------------------------------------------------------------------
# Minimal Maven bootstrap wrapper.
#   - Locate a JDK (needs javac, not just a JRE) and export JAVA_HOME.
#   - If `mvn` is already on PATH, delegate to it.
#   - Otherwise download a pinned Apache Maven into .mvn/ and use that.
# Keeps the repo buildable without a system Maven and without committing a
# binary wrapper jar.
# ----------------------------------------------------------------------------
set -e

MAVEN_VERSION="3.9.9"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- Locate a JDK -----------------------------------------------------------
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
  if command -v javac >/dev/null 2>&1; then
    : # a JDK is already on PATH; let Maven use it
  else
    for candidate in \
        "$HOME/.local/jdk-21" \
        /usr/lib/jvm/java-21-openjdk \
        /usr/lib/jvm/java-25-openjdk \
        /opt/android-studio/jbr; do
      if [ -x "$candidate/bin/javac" ]; then
        JAVA_HOME="$candidate"
        export JAVA_HOME
        PATH="$JAVA_HOME/bin:$PATH"
        export PATH
        break
      fi
    done
  fi
fi
if ! command -v javac >/dev/null 2>&1 && { [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; }; then
  echo "WARNING: no JDK with 'javac' found. Set JAVA_HOME to a JDK 21+ install." >&2
fi

# --- Locate Maven ---------------------------------------------------------
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
