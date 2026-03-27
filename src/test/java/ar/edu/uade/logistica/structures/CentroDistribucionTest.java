package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentroDistribucionTest {
    @Test
    void procesaPrimeroUrgentesOPesados() {
        CentroDistribucion centro = new CentroDistribucion();
        Paquete<String> estandar = new Paquete<>("A", 10, "BA", "Libros", false);
        Paquete<String> pesado = new Paquete<>("B", 65, "CBA", "Herramientas", false);
        Paquete<String> urgente = new Paquete<>("C", 5, "ROS", "Alimentos", true);

        centro.recibir(estandar);
        centro.recibir(pesado);
        centro.recibir(urgente);

        assertEquals(pesado, centro.procesarSiguiente());
        assertEquals(urgente, centro.procesarSiguiente());
        assertEquals(estandar, centro.procesarSiguiente());
    }
}
