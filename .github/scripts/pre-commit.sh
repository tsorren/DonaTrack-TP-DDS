#!/bin/bash

echo "Iniciando validaciones locales..."

# 1. Validacion de formato
mvn spotless:check
if [ $? -ne 0 ]; then
    echo "Error: El formato de codigo no cumple con los estandares. Ejecute 'mvn spotless:apply'."
    exit 1
fi

# 2. Validacion de tests por modulo modificado
MODIFIED_MODULES=$(git diff --cached --name-only | cut -d/ -f1 | sort -u)

for MODULE in $MODIFIED_MODULES; do
    if [ -f "$MODULE/pom.xml" ]; then
        echo "Ejecutando tests: $MODULE"
        mvn test -pl "$MODULE" -am
        if [ $? -ne 0 ]; then
            echo "Error: Tests fallidos en el modulo $MODULE."
            exit 1
        fi
    fi
done

echo "Validaciones completadas exitosamente."