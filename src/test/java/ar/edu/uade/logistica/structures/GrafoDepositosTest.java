package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Ruta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrafoDepositosTest {
    @Test
    void encuentraLaRutaMasCorta() {
        GrafoDepositos grafo = new GrafoDepositos();
        grafo.conectar(new Ruta(50, 20, 700));
        grafo.conectar(new Ruta(20, 30, 300));
        grafo.conectar(new Ruta(50, 80, 1100));
        grafo.conectar(new Ruta(80, 30, 200));

        assertEquals(1000, grafo.distanciaMinima(50, 30));
    }
}
