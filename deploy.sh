#!/bin/bash
set -e

SRC_DIR="src"
LIB_DIR="lib"
BUILD_DIR="build"
OUTPUT_JAR="framework.jar"
# Surcharge possible : PROJET_TEST_LIB=/autre/chemin ./deploy.sh
PROJET_TEST_LIB="${PROJET_TEST_LIB:-../ProjetTest/lib}"

SERVLET_JAR=$(find "$LIB_DIR" -name "*.jar" | head -1)
if [ -z "$SERVLET_JAR" ]; then
    echo "Erreur: Aucun JAR servlet trouvé dans $LIB_DIR"
    exit 1
fi
echo "Servlet JAR : $SERVLET_JAR"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"

echo "Compilation..."
javac -cp "$SERVLET_JAR" -d "$BUILD_DIR/classes" $(find "$SRC_DIR" -name "*.java")

echo "Création du JAR..."
jar cvf "$OUTPUT_JAR" -C "$BUILD_DIR/classes" .

mkdir -p "$PROJET_TEST_LIB"
cp "$OUTPUT_JAR" "$PROJET_TEST_LIB/framework.jar"
echo "✅ Copié dans $PROJET_TEST_LIB"
