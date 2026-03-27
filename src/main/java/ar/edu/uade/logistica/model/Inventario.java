package ar.edu.uade.logistica.model;

import java.util.ArrayList;
import java.util.List;

public class Inventario {
    private final List<Paquete<String>> paquetes;
    private final List<Deposito> depositos;
    private final List<Ruta> rutas;

    public Inventario(List<Paquete<String>> paquetes, List<Deposito> depositos, List<Ruta> rutas) {
        this.paquetes = new ArrayList<>(paquetes == null ? List.of() : paquetes);
        this.depositos = new ArrayList<>(depositos == null ? List.of() : depositos);
        this.rutas = new ArrayList<>(rutas == null ? List.of() : rutas);
    }

    public List<Paquete<String>> getPaquetes() {
        return new ArrayList<>(paquetes);
    }

    public List<Deposito> getDepositos() {
        return new ArrayList<>(depositos);
    }

    public List<Ruta> getRutas() {
        return new ArrayList<>(rutas);
    }
}
