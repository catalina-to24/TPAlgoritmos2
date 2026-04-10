package ar.edu.uade.logistica.model;

import java.util.List;

public class Inventario {
    private final List<Paquete<String>> paquetes;
    private final List<Deposito> depositos;
    private final List<Ruta> rutas;

    public Inventario(List<Paquete<String>> paquetes, List<Deposito> depositos, List<Ruta> rutas) {
        this.paquetes = paquetes == null ? List.of() : List.copyOf(paquetes);
        this.depositos = depositos == null ? List.of() : List.copyOf(depositos);
        this.rutas = rutas == null ? List.of() : List.copyOf(rutas);
    }

    public List<Paquete<String>> getPaquetes() {
        return paquetes;
    }

    public List<Deposito> getDepositos() {
        return depositos;
    }

    public List<Ruta> getRutas() {
        return rutas;
    }
}
