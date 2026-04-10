package ar.edu.uade.logistica;

import ar.edu.uade.logistica.cli.MenuConsola;
import ar.edu.uade.logistica.service.LogisticaService;
import ar.edu.uade.logistica.web.WebServer;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            LogisticaService service = new LogisticaService();
            MenuConsola menu = new MenuConsola(service, new Scanner(System.in));
            menu.iniciar();
            return;
        }

        int port = 7070;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equalsIgnoreCase(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            }
        }
        WebServer.start(port);
    }
}
