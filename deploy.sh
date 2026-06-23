#!/bin/bash
set -e

SRC_DIR="src"
LIB_DIR="lib"
BUILD_DIR="build"
OUTPUT_JAR="framework.jar"
# Surcharge possible : PROJET_TEST_LIB=/autre/chemin ./deploy.sh
PROJET_TEST_LIB="${PROJET_TEST_LIB:-../ProjetTest/lib}"

# Tomcat 10 installation via apt - chemin des librairies
TOMCAT_LIB="/usr/share/tomcat10/lib"
SERVLET_JAR="$TOMCAT_LIB/servlet-api.jar"

# Fallback: chercher dans le dossier lib local si le JAR n'existe pas
if [ ! -f "$SERVLET_JAR" ]; then
    echo "⚠️  Servlet API introuvable dans $TOMCAT_LIB"
    echo "Recherche dans $LIB_DIR..."
    SERVLET_JAR=$(find $LIB_DIR -name "*.jar" 2>/dev/null | head -1)
fi

if [ -z "$SERVLET_JAR" ] || [ ! -f "$SERVLET_JAR" ]; then
    echo "❌ Erreur: Aucun JAR servlet trouvé"
    echo "Vérifiez que Tomcat 10 est installé : sudo apt install tomcat10"
    exit 1
fi
echo "Servlet JAR : $SERVLET_JAR"

echo "📦 Servlet JAR utilisé : $SERVLET_JAR"

echo "Compilation..."
javac -cp "$SERVLET_JAR" -d "$BUILD_DIR/classes" $(find "$SRC_DIR" -name "*.java")

# Compiler
echo "🔨 Compilation en cours..."
javac -cp "$SERVLET_JAR" -d $BUILD_DIR/classes $(find $SRC_DIR -name "*.java")

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie !"
    
    # Créer le JAR
    echo "📦 Création du JAR..."
    jar cvf $OUTPUT_JAR -C $BUILD_DIR/classes .
    
    echo "✅ JAR créé : $OUTPUT_JAR"
    
    # Vérifier le contenu
    echo -e "\n📋 Contenu du JAR :"
    jar tf $OUTPUT_JAR | head -20
else
    echo "❌ Erreur de compilation"
    exit 1
fi
