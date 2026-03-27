package ar.edu.uade.logistica.persistence;

import ar.edu.uade.logistica.model.Inventario;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventarioLoaderTest {
    @Test
    void cargaPaquetesDepositosYRutas() throws IOException {
        InventarioLoader loader = new InventarioLoader();

        Inventario inventario = loader.cargar(Path.of("data", "inventario.json"));

        assertEquals(3, inventario.getPaquetes().size());
        assertEquals(6, inventario.getDepositos().size());
        assertEquals(6, inventario.getRutas().size());
    }
}
