package ar.edu.uade.logistica;

import ar.edu.uade.logistica.cli.MenuConsola;
import ar.edu.uade.logistica.gui.LogisticaFrame;
import ar.edu.uade.logistica.service.LogisticaService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            LogisticaService service = new LogisticaService();
            MenuConsola menu = new MenuConsola(service, new Scanner(System.in));
            menu.iniciar();
            return;
        }

        LogisticaFrame.launch();
    }
}
