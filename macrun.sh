#!/bin/bash
echo "=== Logi-UADE 2026 ==="

# Verificar Java
if ! command -v javac &> /dev/null; then
    echo "ERROR: Java JDK no encontrado en PATH."
    echo "Instala JDK 21 con: brew install --cask temurin@21"
    exit 1
fi

echo "[1/3] Compilando..."
mkdir -p out
find src/main/java -name "*.java" | xargs javac -d out
if [ $? -ne 0 ]; then
    echo "ERROR: Fallo la compilacion."
    exit 1
fi

echo "[2/3] Copiando recursos web..."
if [ -d src/main/resources/web ]; then
    cp -r src/main/resources/web out/
fi

echo "[3/3] Iniciando servidor en http://localhost:7070"
echo "Presiona Ctrl+C para detener."
java -cp out ar.edu.uade.logistica.Main
