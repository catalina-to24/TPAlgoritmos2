package ar.edu.uade.logistica;

import ar.edu.uade.logistica.cli.MenuConsola;
import ar.edu.uade.logistica.service.LogisticaService;
import ar.edu.uade.logistica.web.WebServer;

import java.io.IOException;
import java.util.Scanner;

/**
 * Punto de entrada. Elige entre modo CLI y modo web segun argumentos.
 *
 * <p>Convenciones:
 * <ul>
 *     <li>{@code --cli} arranca el menu por consola (sin servidor).</li>
 *     <li>{@code --port N} (default 7070) configura el puerto del WebServer.</li>
 *     <li>Sin argumentos: arranca el WebServer en el puerto 7070.</li>
 * </ul>
 *
 * <p>Por que un solo entry-point: simplifica los scripts de lanzamiento y deja claro en
 * el {@code README} que hay una sola clase {@code Main} para el usuario final.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // Modo consola: corre el menu interactivo y termina cuando el usuario elige "0".
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            LogisticaService service = new LogisticaService();
            MenuConsola menu = new MenuConsola(service, new Scanner(System.in));
            menu.iniciar();
            return;
        }

        // Modo web (default). Recorre los argumentos buscando "--port N".
        int port = 7070;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equalsIgnoreCase(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            }
        }
        WebServer.start(port);
    }
}
