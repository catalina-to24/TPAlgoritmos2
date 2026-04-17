package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Deposito;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link ArbolDepositos}. Cubren los dos comportamientos criticos:
 * la semantica invertida de "visitado" en la auditoria y el recorrido por nivel con BFS.
 */
class ArbolDepositosTest {
    /**
     * Auditoria contra un arbol con tres depositos:
     * <ul>
     *     <li>raiz (id 50): auditado hace 70 dias -> pendiente, se marca visitado=true.</li>
     *     <li>izquierdo (id 20): auditado hace 10 dias -> OK, queda visitado=false.</li>
     *     <li>derecho (id 80): nunca auditado (fecha null) -> pendiente, visitado=true.</li>
     * </ul>
     *
     * El orden esperado de auditados es {@code [80, 50]}: post-orden visita izq, der, raiz.
     * El izquierdo no entra en la lista porque no requirio auditoria. Este test es el que
     * valida la redaccion literal de la consigna (ver nota en CLAUDE.md: visitado=true
     * significa "requiere auditoria").
     */
    @Test
    void auditaEnPostOrdenYMarcaSoloPendientes() {
        ArbolDepositos arbol = new ArbolDepositos();
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 27, 10, 0);
        Deposito raiz = new Deposito(50, "Central", false, ahora.minusDays(70));
        Deposito izq = new Deposito(20, "Cordoba", false, ahora.minusDays(10));
        Deposito der = new Deposito(80, "Mendoza", false, null);
        arbol.insertar(raiz);
        arbol.insertar(izq);
        arbol.insertar(der);

        List<Integer> auditados = arbol.auditarDepositosPendientes(ahora);

        assertEquals(List.of(80, 50), auditados);
        assertTrue(raiz.isVisitado());
        assertFalse(izq.isVisitado());
        assertTrue(der.isVisitado());
    }

    /**
     * Arma un arbol de 4 nodos y pide el nivel 1. El orden esperado es izq-der (20, 80)
     * porque BFS por niveles visita primero el hijo izquierdo de la raiz. El nivel 2
     * (donde esta id=10) queda fuera del resultado, lo que valida que el filtro funcione
     * y no devuelva nodos de niveles posteriores por error.
     */
    @Test
    void devuelveDepositosPorNivel() {
        ArbolDepositos arbol = new ArbolDepositos();
        arbol.insertar(new Deposito(50, "Central", false, null));
        arbol.insertar(new Deposito(20, "Cordoba", false, null));
        arbol.insertar(new Deposito(80, "Mendoza", false, null));
        arbol.insertar(new Deposito(10, "Salta", false, null));

        List<Deposito> nivelUno = arbol.obtenerNivel(1);

        assertEquals(List.of(
                new Deposito(20, "Cordoba", false, null),
                new Deposito(80, "Mendoza", false, null)
        ), nivelUno);
    }
}
