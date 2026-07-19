#!/bin/bash
# Compile le framework Phoenix et produit framework.jar dans build/classes.
# Utilise automatiquement tous les jars présents dans lib/ comme classpath.
#
# Prérequis :
#   - lib/servlet-api.jar (déjà présent)
#   - un jar Spring apportant org.springframework.web.context.WebApplicationContext
#     et ses super-types : spring-web.jar + spring-context.jar + spring-beans.jar
#     + spring-core.jar (à ajouter dans lib/ si absents)
#
# Usage :
#   ./build.sh

set -e

LIB_DIR="lib"
CP=$(find "$LIB_DIR" -name "*.jar" | tr '\n' ':')

if [ -z "$CP" ]; then
  echo "Aucun jar trouvé dans $LIB_DIR/ — impossible de compiler."
  exit 1
fi

echo "Classpath : $CP"

rm -rf build/classes
mkdir -p build/classes

echo "Compilation..."
javac -d build/classes -cp "$CP" $(find src -name "*.java")

echo "Empaquetage..."
cd build/classes
jar cf ../../framework.jar .
cd ../..

echo "OK -> framework.jar régénéré à la racine."