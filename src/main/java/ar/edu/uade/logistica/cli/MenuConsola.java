package ar.edu.uade.logistica.cli;

import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.service.LogisticaService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class MenuConsola {
    private final LogisticaService service;
    private final Scanner scanner;

    public MenuConsola(LogisticaService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

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

    private void cargarInventario() throws IOException {
        System.out.print("Ruta del inventario JSON [data/inventario.json]: ");
        String input = scanner.nextLine().trim();
        Path path = input.isBlank() ? Path.of("data", "inventario.json") : Path.of(input);
        service.cargarInventario(path);
        System.out.println("Inventario cargado correctamente desde " + path.toAbsolutePath());
    }

    private void altaManualPaquete() {
        System.out.print("ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Peso: ");
        double peso = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Destino: ");
        String destino = scanner.nextLine().trim();
        System.out.print("Contenido: ");
        String contenido = scanner.nextLine().trim();
        System.out.print("Es urgente? (true/false): ");
        boolean urgente = Boolean.parseBoolean(scanner.nextLine().trim());

        Paquete<String> paquete = service.crearPaqueteManual(id, peso, destino, contenido, urgente);
        System.out.println("Paquete agregado al centro de distribucion: " + paquete);
    }

    private void procesarYCargarEnCamion() {
        Paquete<?> paquete = service.procesarSiguienteEnCentro();
        service.cargarEnCamion(paquete);
        System.out.println("Paquete procesado y cargado en camion: " + paquete);
    }

    private void deshacerUltimaCarga() {
        Paquete<?> paquete = service.deshacerUltimaCargaCamion();
        System.out.println("Se deshizo la ultima carga: " + paquete);
    }

    private void descargarDelCamion() {
        Paquete<?> paquete = service.descargarCamion();
        System.out.println("Paquete descargado: " + paquete);
    }

    private void verCargaCamion() {
        List<Paquete<?>> carga = service.verCargaCamion();
        if (carga.isEmpty()) {
            System.out.println("El camion no tiene paquetes cargados.");
            return;
        }
        System.out.println("Carga actual del camion (tope primero):");
        carga.forEach(System.out::println);
    }

    private void auditarDepositos() {
        List<Integer> ids = service.auditarDepositos(LocalDateTime.now());
        if (ids.isEmpty()) {
            System.out.println("No hay depositos pendientes de auditoria.");
            return;
        }
        System.out.println("Depositos marcados como visitados: " + ids);
    }

    private void verDepositosPorNivel() {
        System.out.print("Nivel a consultar: ");
        int nivel = Integer.parseInt(scanner.nextLine().trim());
        List<String> depositos = service.depositosPorNivel(nivel);
        if (depositos.isEmpty()) {
            System.out.println("No hay depositos en ese nivel.");
            return;
        }
        depositos.forEach(System.out::println);
    }

    private void calcularDistanciaMinima() {
        System.out.print("Deposito origen: ");
        int origen = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Deposito destino: ");
        int destino = Integer.parseInt(scanner.nextLine().trim());
        int distancia = service.distanciaMinimaEntreDepositos(origen, destino);
        System.out.println("Distancia minima: " + distancia + " km");
    }
}
