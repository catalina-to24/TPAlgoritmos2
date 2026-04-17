package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios de {@link Camion}. Validan la propiedad LIFO exigida por consigna
 * (deshacer devuelve el ultimo cargado) y el contrato de fallo al descargar vacio.
 */
class CamionTest {
    /**
     * Caso canonico: cargamos dos paquetes y verificamos que la descarga/deshacer devuelva
     * primero el de mas arriba (LIFO) y despues el de abajo. Al vaciar la pila, una nueva
     * descarga debe fallar con excepcion — esto asegura que el camion nunca devuelva
     * {@code null}, contrato importante para la capa web (traduce a 400 en vez de NPE).
     */
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
