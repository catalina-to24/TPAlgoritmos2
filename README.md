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
- `src/main/java/ar/edu/uade/logistica/web/WebServer.java`: servidor HTTP y API REST (JDK `HttpServer`, sin deps)
- `src/main/java/ar/edu/uade/logistica/web/JsonWriter.java`: serializador JSON minimalista
- `src/main/java/ar/edu/uade/logistica/cli/MenuConsola.java`: menu por consola
- `src/main/java/ar/edu/uade/logistica/model`: entidades del dominio
- `src/main/java/ar/edu/uade/logistica/structures`: pila, cola con prioridad, ABB y grafo
- `src/main/java/ar/edu/uade/logistica/persistence`: parser JSON simple y carga de inventario
- `src/main/java/ar/edu/uade/logistica/service/LogisticaService.java`: coordinacion de casos de uso
- `src/main/resources/web`: front-end estatico (HTML + Tailwind CDN + JS vanilla)
- `data/inventario.json`: dataset de ejemplo

## Ejecucion

Compilar:

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/web out/
```

Abrir la interfaz web (default):

```bash
java -cp out ar.edu.uade.logistica.Main
```

Luego abrir `http://localhost:7070` en el navegador. Para usar otro puerto: `--port 8080`.

Abrir el modo consola:

```bash
java -cp out ar.edu.uade.logistica.Main --cli
```

## Uso de la interfaz web

- Boton `Cargar inventario`: carga `data/inventario.json` por default y popula las KPIs.
- Tarjeta `Centro de distribucion`: alta manual de paquetes y boton para mandar el siguiente al camion.
- Tarjeta `Camion (pila LIFO)`: ver carga actual (tope arriba), deshacer ultima carga y descargar.
- Tarjeta `Depositos (ABB)`: ejecutar auditoria post-orden y consultar depositos por nivel.
- Tarjeta `Rutas (grafo + Dijkstra)`: calcular distancia minima entre dos depositos.

Toda la logica y los calculos viven en el backend Java (`LogisticaService` + TDAs). El front-end solo hace `fetch()` contra `/api/*` y renderiza. Sin frameworks JS ni build step: Tailwind se carga por CDN.

## API REST

| Metodo | Ruta                                           | Descripcion                              |
|--------|------------------------------------------------|------------------------------------------|
| POST   | `/api/inventario/cargar`                       | Body: `{ "path": "data/inventario.json" }` |
| GET    | `/api/estado`                                  | KPIs (pendientes centro, camion, tope)   |
| POST   | `/api/centro/paquetes`                         | Alta manual                               |
| POST   | `/api/camion/cargar`                           | Procesa del centro y apila en el camion  |
| POST   | `/api/camion/deshacer`                         | Undo O(1)                                 |
| POST   | `/api/camion/descargar`                        | Pop del tope                              |
| GET    | `/api/camion`                                  | Carga actual                              |
| POST   | `/api/depositos/auditar`                       | Body opcional `{ "fechaReferencia": ISO }` |
| GET    | `/api/depositos/nivel/{n}`                     | Depositos del nivel N                     |
| GET    | `/api/depositos/{id}`                          | Buscar deposito                           |
| GET    | `/api/rutas/distancia?origen=X&destino=Y`      | Dijkstra                                  |

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
