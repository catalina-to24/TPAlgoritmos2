package ar.edu.uade.logistica.cli;

import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.service.LogisticaService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * Modo consola del sistema. Se arranca con {@code java -cp out ... Main --cli} y expone
 * los mismos flujos que la web pero via texto.
 *
 * <p>Por que se mantiene aunque exista la GUI web: es mas rapido para demos de catedra y
 * funciona sin navegador / sin X server. Ademas prueba que toda la logica vive en
 * {@link LogisticaService}: la CLI y la web se intercambian sin duplicar reglas.
 */
public class MenuConsola {
    private final LogisticaService service;
    private final Scanner scanner;

    /** Se inyecta el scanner para permitir tests con entrada controlada (no solo {@code System.in}). */
    public MenuConsola(LogisticaService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    /**
     * Loop principal del menu.
     *
     * <p>El {@code try/catch} amplio alrededor del dispatch impide que un error del usuario
     * (p.ej. pedir descargar un camion vacio) mate el proceso: se muestra y se vuelve al
     * menu. Es el equivalente CLI de los 400 que devuelve el WebServer.
     */
    public void iniciar() {
        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();
            try {
                switch (opcion) {
                    case "1" -> cargarInventario();
                    case "2" -> altaManualPaquete();
                    case "3" -> procesarYCargarEnCamion();
                    case "4" -> deshacerUltimaCarga();
                    case "5" -> descargarDelCamion();
                    case "6" -> verCargaCamion();
                    case "7" -> auditarDepositos();
                    case "8" -> verDepositosPorNivel();
                    case "9" -> calcularDistanciaMinima();
                    case "0" -> salir = true;
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (RuntimeException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    /** Imprime el menu principal con las opciones numeradas. */
    private void mostrarMenu() {
        System.out.println("=== Logi-UADE 2026 ===");
        System.out.println("1. Cargar inventario desde JSON");
        System.out.println("2. Alta manual de paquete");
        System.out.println("3. Procesar siguiente paquete y cargarlo en camion");
        System.out.println("4. Deshacer ultima carga del camion");
        System.out.println("5. Descargar paquete del camion");
        System.out.println("6. Ver carga actual del camion");
        System.out.println("7. Ejecutar auditoria de depositos");
        System.out.println("8. Mostrar depositos por nivel");
        System.out.println("9. Calcular distancia minima entre depositos");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opcion: ");
    }

    /**
     * Pide la ruta del inventario (default {@code data/inventario.json}) y delega la
     * carga al servicio. El {@code toAbsolutePath()} en el mensaje ayuda a diagnosticar
     * rutas relativas que apuntan a cualquier otro lado.
     */
    private void cargarInventario() throws IOException {
        System.out.print("Ruta del inventario JSON [data/inventario.json]: ");
        String input = scanner.nextLine().trim();
        Path path = input.isBlank() ? Path.of("data", "inventario.json") : Path.of(input);
        service.cargarInventario(path);
        System.out.println("Inventario cargado correctamente desde " + path.toAbsolutePath());
    }

    /** Alta manual de un paquete pidiendo campo por campo y validando tipos. */
    private void altaManualPaquete() {
        System.out.print("ID: ");
        String id = scanner.nextLine().trim();
        double peso = leerDouble("Peso: ");
        System.out.print("Destino: ");
        String destino = scanner.nextLine().trim();
        System.out.print("Contenido: ");
        String contenido = scanner.nextLine().trim();
        boolean urgente = leerBoolean("Es urgente? (s/n): ");

        Paquete<String> paquete = service.crearPaqueteManual(id, peso, destino, contenido, urgente);
        System.out.println("Paquete agregado al centro de distribucion: " + paquete);
    }

    /**
     * Operacion combinada: saca del centro de distribucion y apila en el camion.
     * Replica el flujo mas comun del dominio y ahorra dos opciones de menu separadas.
     */
    private void procesarYCargarEnCamion() {
        Paquete<?> paquete = service.procesarSiguienteEnCentro();
        service.cargarEnCamion(paquete);
        System.out.println("Paquete procesado y cargado en camion: " + paquete);
    }

    /** Undo de la ultima carga (pop del camion). */
    private void deshacerUltimaCarga() {
        Paquete<?> paquete = service.deshacerUltimaCargaCamion();
        System.out.println("Se deshizo la ultima carga: " + paquete);
    }

    /** Entrega en destino (pop del camion, semantica distinta al undo). */
    private void descargarDelCamion() {
        Paquete<?> paquete = service.descargarCamion();
        System.out.println("Paquete descargado: " + paquete);
    }

    /** Muestra la carga actual del camion con el tope primero. */
    private void verCargaCamion() {
        List<Paquete<?>> carga = service.verCargaCamion();
        if (carga.isEmpty()) {
            System.out.println("El camion no tiene paquetes cargados.");
            return;
        }
        System.out.println("Carga actual del camion (tope primero):");
        carga.forEach(System.out::println);
    }

    /**
     * Ejecuta la auditoria post-orden usando {@code now()} como fecha de corte.
     * Se usa "now" aqui para que desde la CLI sea trivial auditar contra el presente;
     * la web permite pasar una fecha arbitraria.
     */
    private void auditarDepositos() {
        List<Integer> ids = service.auditarDepositos(LocalDateTime.now());
        if (ids.isEmpty()) {
            System.out.println("No hay depositos pendientes de auditoria.");
            return;
        }
        System.out.println("Depositos marcados como visitados: " + ids);
    }

    /** Reporte BFS del nivel pedido. */
    private void verDepositosPorNivel() {
        int nivel = leerEntero("Nivel a consultar: ");
        List<Deposito> depositos = service.depositosPorNivel(nivel);
        if (depositos.isEmpty()) {
            System.out.println("No hay depositos en ese nivel.");
            return;
        }
        depositos.forEach(d -> System.out.println(d.getId() + " - " + d.getNombre()));
    }

    /** Dijkstra entre los dos depositos que pida el usuario. */
    private void calcularDistanciaMinima() {
        int origen = leerEntero("Deposito origen: ");
        int destino = leerEntero("Deposito destino: ");
        int distancia = service.distanciaMinimaEntreDepositos(origen, destino);
        System.out.println("Distancia minima: " + distancia + " km");
    }

    /**
     * Lee un entero validando el input. Se reintenta hasta que el usuario acierta para
     * que un tipeo malo no haga perder el contexto del menu.
     */
    private int leerEntero(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }

    /** Idem {@link #leerEntero} pero para decimales. */
    private double leerDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero valido.");
            }
        }
    }

    /**
     * Parseo permisivo de booleano: acepta s/n/si/no/true/false para que la CLI se sienta
     * natural en castellano sin renegar con "true/false" estricto.
     */
    private boolean leerBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("s") || input.equals("si") || input.equals("true")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("false")) {
                return false;
            }
            System.out.println("Responda con s/n.");
        }
    }
}
