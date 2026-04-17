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
 *
 * <p>Por que una pila y no una lista / cola: la consigna exige explicitamente estructura
 * LIFO para que la operacion "deshacer" sea {@code O(1)}. Una lista con remove-last tambien
 * es O(1), pero no transmite la semantica ni cumple el requisito de usar el TDA correcto.
 *
 * <p>Por que {@link ArrayDeque}: la JDK la recomienda como pila eficiente en lugar de
 * {@code java.util.Stack} (que es sincronizada y hereda de {@code Vector}).
 * Sigue respetando la restriccion de "usar el TDA pila" porque se lo utiliza
 * exclusivamente a traves de push/pop.
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
     *
     * <p>Lanza {@link NoSuchElementException} en lugar de devolver {@code null} para
     * que un error del usuario (descargar vacio) sea visible y se traduzca a 400 en
     * la capa web.
     *
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
     *
     * <p>Se expone como metodo separado para que la API del dominio refleje la accion
     * "undo" de la consigna, aunque internamente haga exactamente lo mismo.
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
     *
     * <p>Se calcula bajo demanda (en vez de mantener un acumulador) porque la operacion
     * es relativamente infrecuente y evita mantener dos fuentes de verdad que podrian
     * quedar desincronizadas.
     *
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
     *
     * <p>Copia para evitar exponer la estructura interna: el consumidor (UI) puede
     * iterar/renderizar sin riesgo de modificar la pila.
     *
     * Complejidad: O(n) tiempo, O(n) espacio.
     */
    public List<Paquete<?>> verCargaActual() {
        return new ArrayList<>(carga);
    }
}
