package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CamionTest {
    @Test
    void descargaEnOrdenLifoYPermiteDeshacer() {
        Camion camion = new Camion();
        Paquete<String> primero = new Paquete<>("A", 10, "BA", "Electronica", false);
        Paquete<String> segundo = new Paquete<>("B", 15, "CBA", "Fragil", true);

        camion.cargar(primero);
        camion.cargar(segundo);

        assertEquals(segundo, camion.deshacerUltimaCarga());
        assertEquals(primero, camion.descargar());
        assertThrows(RuntimeException.class, camion::descargar);
    }
}
