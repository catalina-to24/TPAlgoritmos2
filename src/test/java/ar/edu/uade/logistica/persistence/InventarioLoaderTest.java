package ar.edu.uade.logistica.persistence;

import ar.edu.uade.logistica.model.Inventario;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test de integracion del {@link InventarioLoader} contra el archivo real
 * {@code data/inventario.json}. Sirve como smoke test: si alguien cambia la estructura
 * del JSON o el parser, este test falla.
 */
class InventarioLoaderTest {
    /**
     * Verifica que se carguen las 3 entidades (paquetes, depositos, rutas) con la
     * cardinalidad esperada segun el dataset actual.
     *
     * <p>Se evita aserciones campo-por-campo a proposito: el dataset es dato, no codigo,
     * y cambiarlo no deberia romper el test mientras las secciones esten presentes con
     * la cantidad correcta. Validar contenido exacto seria fragil.
     */
    @Test
    void cargaPaquetesDepositosYRutas() throws IOException {
        InventarioLoader loader = new InventarioLoader();

        Inventario inventario = loader.cargar(Path.of("data", "inventario.json"));

        assertEquals(3, inventario.getPaquetes().size());
        assertEquals(6, inventario.getDepositos().size());
        assertEquals(6, inventario.getRutas().size());
    }
}
