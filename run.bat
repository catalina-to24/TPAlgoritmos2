@echo off
echo === Logi-UADE 2026 ===

:: Verificar Java
where javac >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java JDK no encontrado en PATH.
    echo Instala JDK 21 desde https://adoptium.net/ y reinicia la terminal.
    pause
    exit /b 1
)

echo [1/3] Compilando...
if not exist out mkdir out
javac -d out -sourcepath src\main\java src\main\java\ar\edu\uade\logistica\Main.java 2>&1
if errorlevel 1 (
    echo ERROR: Fallo la compilacion. Ver errores arriba.
    pause
    exit /b 1
)

echo [2/3] Copiando recursos web...
if exist src\main\resources\web (
    xcopy /E /Y /Q src\main\resources\web out\web\ >nul
)

echo [3/3] Iniciando servidor en http://localhost:7070
echo Presiona Ctrl+C para detener.
java -cp out ar.edu.uade.logistica.Main
pause
