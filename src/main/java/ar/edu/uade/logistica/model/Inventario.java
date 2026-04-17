package ar.edu.uade.logistica.model;

import java.util.List;

/**
 * Agregado inmutable del dataset cargado desde {@code data/inventario.json}:
 * paquetes iniciales, depositos y rutas. Actua como DTO entre la capa de persistencia
 * ({@link ar.edu.uade.logistica.persistence.InventarioLoader}) y el servicio de dominio.
 *
 * <p>Por que copias inmutables via {@link List#copyOf(java.util.Collection)}: quien
 * construye el inventario pasa listas mutables recien parseadas. Al copiarlas aca, el
 * objeto queda sellado y los getters pueden devolverlas directamente sin temor a que
 * un consumidor las modifique. Es el compromiso entre seguridad e ignorar copias defensivas
 * en cada getter (ver CLAUDE.md).
 */
public class Inventario {
    private final List<Paquete<String>> paquetes;
    private final List<Deposito> depositos;
    private final List<Ruta> rutas;

    /**
     * Construye el inventario tolerando listas {@code null} (se interpretan como vacias)
     * para simplificar el parser: un JSON sin la seccion "rutas" sigue siendo valido.
     */
    public Inventario(List<Paquete<String>> paquetes, List<Deposito> depositos, List<Ruta> rutas) {
        this.paquetes = paquetes == null ? List.of() : List.copyOf(paquetes);
        this.depositos = depositos == null ? List.of() : List.copyOf(depositos);
        this.rutas = rutas == null ? List.of() : List.copyOf(rutas);
    }

    /** Paquetes iniciales a encolar en el centro de distribucion. */
    public List<Paquete<String>> getPaquetes() {
        return paquetes;
    }

    /** Depositos a cargar en el ABB y como nodos del grafo. */
    public List<Deposito> getDepositos() {
        return depositos;
    }

    /** Rutas a agregar como aristas del grafo (bidireccionales). */
    public List<Ruta> getRutas() {
        return rutas;
    }
}
