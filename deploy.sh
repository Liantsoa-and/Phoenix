#!/bin/bash
set -e

# ============================================================
#  deploy.sh — Compile le framework Phoenix → framework.jar
#  Usage : ./deploy.sh
#  Surcharge : PROJET_TEST_LIB=/autre/chemin ./deploy.sh
# ============================================================

SRC_DIR="src"
LIB_DIR="lib"
BUILD_DIR="build"
OUTPUT_JAR="framework.jar"
PROJET_TEST_LIB="${PROJET_TEST_LIB:-../phoenix-test-fixed/lib}"

# --- Couleurs ---
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[ERREUR]${NC} $1"; exit 1; }

# --- 1. Servlet API ---
echo "==> Recherche du Servlet API..."

TOMCAT_LIB="/usr/share/tomcat10/lib"
SERVLET_JAR="$TOMCAT_LIB/servlet-api.jar"

if [ ! -f "$SERVLET_JAR" ]; then
    warn "Servlet API introuvable dans $TOMCAT_LIB"
    echo "    Recherche dans $LIB_DIR..."
    SERVLET_JAR=$(find "$LIB_DIR" -name "*.jar" 2>/dev/null | head -1)
fi

[ -z "$SERVLET_JAR" ] || [ ! -f "$SERVLET_JAR" ] && \
    fail "Aucun JAR servlet trouvé. Installez Tomcat 10 : sudo apt install tomcat10"

ok "Servlet JAR : $SERVLET_JAR"

# --- 2. Nettoyage ---
echo "==> Nettoyage..."
rm -rf "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/classes"
ok "Nettoyage OK"

# --- 3. Compilation ---
echo "==> Compilation du framework..."
javac -encoding UTF-8 \
      -cp "$SERVLET_JAR" \
      -d "$BUILD_DIR/classes" \
      $(find "$SRC_DIR" -name "*.java") \
      || fail "Erreur de compilation"
ok "Compilation OK"

# --- 4. Création du JAR ---
echo "==> Création de $OUTPUT_JAR..."
jar cvf "$OUTPUT_JAR" -C "$BUILD_DIR/classes" .
ok "JAR créé : $OUTPUT_JAR"

echo -e "\n📋 Contenu du JAR :"
jar tf "$OUTPUT_JAR" | grep "\.class" | head -20

# --- 5. Copie dans le projet test ---
echo ""
echo "==> Copie vers $PROJET_TEST_LIB..."
if [ -d "$PROJET_TEST_LIB" ]; then
    cp "$OUTPUT_JAR" "$PROJET_TEST_LIB/"
    ok "framework.jar copié dans $PROJET_TEST_LIB"
else
    warn "Dossier $PROJET_TEST_LIB introuvable — copie ignorée."
    warn "Lancez : PROJET_TEST_LIB=/chemin/vers/lib ./deploy.sh"
fi

echo ""
echo "=========================================="
echo " Framework compilé avec succès !"
echo "=========================================="