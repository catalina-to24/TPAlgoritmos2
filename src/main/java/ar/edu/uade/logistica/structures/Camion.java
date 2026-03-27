package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

public class Camion {
    private final Deque<Paquete<?>> carga = new ArrayDeque<>();

    public void cargar(Paquete<?> paquete) {
        carga.push(paquete);
    }

    public Paquete<?> descargar() {
        if (carga.isEmpty()) {
            throw new NoSuchElementException("No hay paquetes para descargar.");
        }
        return carga.pop();
    }

    public Paquete<?> deshacerUltimaCarga() {
        return descargar();
    }

    public boolean estaVacio() {
        return carga.isEmpty();
    }

    public int cantidadPaquetes() {
        return carga.size();
    }

    public List<Paquete<?>> verCargaActual() {
        return new ArrayList<>(carga);
    }
}
