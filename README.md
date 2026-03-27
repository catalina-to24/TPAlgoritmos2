# Logi-UADE 2026

Trabajo practico de AyED II / Programacion II desarrollado en Java a partir de la consigna del PDF "Sistema de Gestion Logistica".

La aplicacion resuelve las dos iteraciones pedidas:

- Iteracion 1:
  - `Paquete<T>` generico
  - gestion de camiones con pila LIFO
  - deshacer de ultima carga en `O(1)`
  - centro de distribucion con prioridad para urgentes o paquetes de mas de 50 kg
  - carga inicial desde `inventario.json`
  - carga manual de datos
- Iteracion 2:
  - ABB implementado desde cero para depositos
  - auditoria postorden con marca de `visitado`
  - consulta de depositos por nivel
  - grafo de rutas con distancia minima

## Requisitos

- Java 21 o superior
- No usa dependencias externas para la aplicacion principal
- El proyecto incluye pruebas unitarias en `src/test`, pensadas para ejecutarse con JUnit 5

## Estructura

- `src/main/java/ar/edu/uade/logistica/Main.java`: punto de entrada
- `src/main/java/ar/edu/uade/logistica/gui/LogisticaFrame.java`: interfaz grafica Swing
- `src/main/java/ar/edu/uade/logistica/cli/MenuConsola.java`: menu por consola
- `src/main/java/ar/edu/uade/logistica/model`: entidades del dominio
- `src/main/java/ar/edu/uade/logistica/structures`: pila, cola con prioridad, ABB y grafo
- `src/main/java/ar/edu/uade/logistica/persistence`: parser JSON simple y carga de inventario
- `src/main/java/ar/edu/uade/logistica/service/LogisticaService.java`: coordinacion de casos de uso
- `data/inventario.json`: dataset de ejemplo

## Ejecucion

Compilar:

```powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -d out (Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName })
```

Abrir la interfaz grafica:

```powershell
java -cp out ar.edu.uade.logistica.Main
```

Abrir el modo consola:

```powershell
java -cp out ar.edu.uade.logistica.Main --cli
```

## Uso de la interfaz

- Boton `Cargar inventario JSON`: carga `data/inventario.json` o cualquier archivo compatible.
- Pestaña `Paquetes`: alta manual y procesamiento hacia el camion.
- Pestaña `Camion`: ver carga actual, deshacer ultima carga y descargar.
- Pestaña `Depositos`: ejecutar auditoria y consultar depositos por nivel.
- Pestaña `Rutas`: calcular distancia minima entre dos depositos.

## Formato JSON esperado

El archivo puede incluir tres bloques: `paquetes`, `depositos` y `rutas`.

```json
{
  "paquetes": [
    {
      "id": "PKG-001",
      "peso": 12.5,
      "destino": "Buenos Aires",
      "contenido": "Electronica",
      "urgente": false
    }
  ],
  "depositos": [
    {
      "id": 50,
      "nombre": "Hub Central Buenos Aires",
      "visitado": false,
      "fechaUltimaAuditoria": "2026-01-10T09:00:00",
      "conexiones": [20, 80]
    }
  ],
  "rutas": [
    {
      "origen": 50,
      "destino": 20,
      "distanciaKm": 700
    }
  ]
}
```

## Justificacion de estructuras

- Pila para camion:
  - modela exactamente la descarga LIFO pedida por la consigna
  - permite deshacer la ultima carga en `O(1)`
- Priority queue para centro de distribucion:
  - prioriza urgentes y paquetes pesados sin reordenar toda la coleccion en cada consulta
- ABB manual para depositos:
  - permite busqueda eficiente por ID y recorrido postorden para auditoria
- Grafo con lista de adyacencia:
  - representa mejor la red de rutas y permite calcular caminos minimos con buen costo

## Complejidad temporal y espacial

- `Camion.cargar`: tiempo `O(1)`, espacio extra `O(1)`
- `Camion.descargar`: tiempo `O(1)`, espacio extra `O(1)`
- `Camion.deshacerUltimaCarga`: tiempo `O(1)`, espacio extra `O(1)`
- `CentroDistribucion.recibir`: tiempo `O(log n)`, espacio extra `O(1)`
- `CentroDistribucion.procesarSiguiente`: tiempo `O(log n)`, espacio extra `O(1)`
- `ArbolDepositos.insertar`: tiempo `O(log n)` promedio, `O(n)` peor caso; espacio `O(h)`
- `ArbolDepositos.buscar`: tiempo `O(log n)` promedio, `O(n)` peor caso; espacio `O(1)`
- `ArbolDepositos.auditarDepositosPendientes`: tiempo `O(n)`, espacio `O(h)`
- `ArbolDepositos.obtenerNivel`: tiempo `O(n)`, espacio `O(n)`
- `GrafoDepositos.conectar`: tiempo `O(1)`, espacio extra `O(1)`
- `GrafoDepositos.distanciaMinima`: tiempo `O((V + E) log V)`, espacio `O(V)`
- `InventarioLoader.cargar`: tiempo `O(n)` respecto del tamano del JSON; espacio `O(n)`

## Pruebas

Se incluyeron pruebas unitarias para:

- pila de camion
- cola prioritaria del centro
- auditoria y consulta por nivel del ABB
- distancia minima del grafo
- carga de datos desde JSON

Archivos:

- `src/test/java/ar/edu/uade/logistica/structures/CamionTest.java`
- `src/test/java/ar/edu/uade/logistica/structures/CentroDistribucionTest.java`
- `src/test/java/ar/edu/uade/logistica/structures/ArbolDepositosTest.java`
- `src/test/java/ar/edu/uade/logistica/structures/GrafoDepositosTest.java`
- `src/test/java/ar/edu/uade/logistica/persistence/InventarioLoaderTest.java`

No pude ejecutar JUnit en este entorno porque la descarga del `jar` de JUnit quedo bloqueada por resolucion de red, pero los tests ya quedaron escritos y listos para correr donde haya acceso al artefacto.
