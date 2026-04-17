package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Ruta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de {@link GrafoDepositos}. Valida que Dijkstra elija el camino mas corto cuando
 * hay rutas alternativas con distintas distancias.
 */
class GrafoDepositosTest {
    /**
     * Topologia con dos caminos posibles de 50 a 30:
     * <ul>
     *     <li>50 -> 20 -> 30 = 700 + 300 = 1000 km</li>
     *     <li>50 -> 80 -> 30 = 1100 + 200 = 1300 km</li>
     * </ul>
     * El test fuerza que Dijkstra descarte el camino mas corto en saltos (ambos tienen
     * 2) y prefiera el de menor distancia acumulada, que es lo que exige la consigna
     * al pedir "distancia minima" en vez de "cantidad minima de paradas".
     */
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
