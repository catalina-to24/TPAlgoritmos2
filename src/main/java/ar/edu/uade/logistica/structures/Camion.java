package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Pila (LIFO) de paquetes que modela la carga de un camion. El ultimo paquete cargado
 * es el primero en descargarse, por lo que deshacer la ultima carga equivale a descargar.
 */
public class Camion {
    private final Deque<Paquete<?>> carga = new ArrayDeque<>();

    /**
     * Apila un paquete en el camion.
     * Complejidad: O(1) amortizado tiempo, O(1) espacio.
     */
    public void cargar(Paquete<?> paquete) {
        carga.push(paquete);
    }

    /**
     * Desapila y devuelve el paquete del tope.
     * Complejidad: O(1) tiempo, O(1) espacio.
     */
    public Paquete<?> descargar() {
        if (carga.isEmpty()) {
            throw new NoSuchElementException("No hay paquetes para descargar.");
        }
        return carga.pop();
    }

    /**
     * Deshace la ultima carga en caso de error de destino. Alias semantico de
     * {@link #descargar()} dado que la pila es LIFO.
     * Complejidad: O(1) tiempo, O(1) espacio.
     */
    public Paquete<?> deshacerUltimaCarga() {
        return descargar();
    }

    /** Complejidad: O(1) tiempo, O(1) espacio. */
    public boolean estaVacio() {
        return carga.isEmpty();
    }

    /** Complejidad: O(1) tiempo, O(1) espacio. */
    public int cantidadPaquetes() {
        return carga.size();
    }

    /**
     * Suma el peso de todos los paquetes cargados.
     * Complejidad: O(n) tiempo, O(1) espacio.
     */
    public double pesoTotal() {
        double total = 0;
        for (Paquete<?> p : carga) {
            total += p.getPeso();
        }
        return total;
    }

    /**
     * Devuelve una copia de la carga actual, con el tope como primer elemento.
     * Complejidad: O(n) tiempo, O(n) espacio.
     */
    public List<Paquete<?>> verCargaActual() {
        return new ArrayList<>(carga);
    }
}
