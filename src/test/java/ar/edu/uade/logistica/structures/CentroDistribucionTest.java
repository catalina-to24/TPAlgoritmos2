package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de {@link CentroDistribucion}. El objetivo es asegurar la regla de prioridad de
 * la consigna: "urgentes o peso > 50 kg" pasan adelante, con FIFO como desempate.
 */
class CentroDistribucionTest {
    /**
     * Mix intencional de los tres casos:
     * <ul>
     *     <li>{@code estandar}: ni urgente ni pesado -> baja prioridad.</li>
     *     <li>{@code pesado}: 65 kg -> pasa por {@code peso > 50 kg}.</li>
     *     <li>{@code urgente}: flag urgente = true -> tambien pasa al frente.</li>
     * </ul>
     * El orden esperado es pesado -> urgente -> estandar: entre los dos prioritarios
     * desempata el orden de llegada (pesado se encolo primero).
     */
    @Test
    void procesaPrimeroUrgentesOPesados() {
        CentroDistribucion centro = new CentroDistribucion();
        Paquete<String> estandar = new Paquete<>("A", 10, "BA", "Libros", false, 0);
        Paquete<String> pesado = new Paquete<>("B", 65, "CBA", "Herramientas", false, 0);
        Paquete<String> urgente = new Paquete<>("C", 5, "ROS", "Alimentos", true, 0);

        centro.recibir(estandar);
        centro.recibir(pesado);
        centro.recibir(urgente);

        assertEquals(pesado, centro.procesarSiguiente());
        assertEquals(urgente, centro.procesarSiguiente());
        assertEquals(estandar, centro.procesarSiguiente());
    }

    @Test
    void verDemoradosFiltraCorrectamente() {
        CentroDistribucion centro = new CentroDistribucion();
        Paquete<String> rapido = new Paquete<>("A", 10, "BA", "Libros", false, 15);
        Paquete<String> demorado1 = new Paquete<>("B", 20, "CBA", "Electronica", false, 45);
        Paquete<String> demorado2 = new Paquete<>("C", 5, "ROS", "Alimentos", true, 60);
        Paquete<String> justo = new Paquete<>("D", 8, "MDZ", "Fragil", false, 30);

        centro.recibir(rapido);
        centro.recibir(demorado1);
        centro.recibir(demorado2);
        centro.recibir(justo);

        List<Paquete<?>> demorados = centro.verDemorados();
        assertEquals(2, demorados.size());
        assertEquals(demorado2, demorados.get(0)); // urgente, sale primero por prioridad
        assertEquals(demorado1, demorados.get(1));
    }
}
