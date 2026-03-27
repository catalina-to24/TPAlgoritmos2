package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Deposito;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArbolDepositosTest {
    @Test
    void auditaEnPostOrdenYMarcaSoloPendientes() {
        ArbolDepositos arbol = new ArbolDepositos();
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 27, 10, 0);
        Deposito raiz = new Deposito(50, "Central", false, ahora.minusDays(70), List.of());
        Deposito izq = new Deposito(20, "Cordoba", false, ahora.minusDays(10), List.of());
        Deposito der = new Deposito(80, "Mendoza", false, null, List.of());
        arbol.insertar(raiz);
        arbol.insertar(izq);
        arbol.insertar(der);

        List<Integer> auditados = arbol.auditarDepositosPendientes(ahora);

        assertEquals(List.of(80, 50), auditados);
        assertTrue(raiz.isVisitado());
        assertFalse(izq.isVisitado());
        assertTrue(der.isVisitado());
    }

    @Test
    void devuelveDepositosPorNivel() {
        ArbolDepositos arbol = new ArbolDepositos();
        arbol.insertar(new Deposito(50, "Central", false, null, List.of()));
        arbol.insertar(new Deposito(20, "Cordoba", false, null, List.of()));
        arbol.insertar(new Deposito(80, "Mendoza", false, null, List.of()));
        arbol.insertar(new Deposito(10, "Salta", false, null, List.of()));

        List<Deposito> nivelUno = arbol.obtenerNivel(1);

        assertEquals(List.of(
                new Deposito(20, "Cordoba", false, null, List.of()),
                new Deposito(80, "Mendoza", false, null, List.of())
        ), nivelUno);
    }
}
