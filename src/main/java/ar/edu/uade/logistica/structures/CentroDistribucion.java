package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cola de prioridad que procesa primero los paquetes urgentes o con peso mayor a 50 kg,
 * usando orden de llegada como desempate (FIFO dentro de cada prioridad).
 */
public class CentroDistribucion {
    private final AtomicLong secuencia = new AtomicLong();
    private final Queue<Entrada> cola = new PriorityQueue<>(Comparator
            .comparing(Entrada::prioritario)
            .reversed()
            .thenComparingLong(Entrada::ordenLlegada));

    /**
     * Encola un paquete calculando su prioridad en el momento de ingreso.
     * Complejidad: O(log n) tiempo, O(1) espacio.
     */
    public void recibir(Paquete<?> paquete) {
        cola.offer(new Entrada(paquete, paquete.requierePrioridad(), secuencia.getAndIncrement()));
    }

    /**
     * Extrae el paquete de mayor prioridad.
     * Complejidad: O(log n) tiempo, O(1) espacio.
     */
    public Paquete<?> procesarSiguiente() {
        Entrada entrada = cola.poll();
        if (entrada == null) {
            throw new NoSuchElementException("No hay paquetes pendientes.");
        }
        return entrada.paquete();
    }

    /** Complejidad: O(1) tiempo, O(1) espacio. */
    public int cantidadPendientes() {
        return cola.size();
    }

    /**
     * Devuelve una copia ordenada por prioridad de los paquetes pendientes sin
     * alterar la cola original.
     * Complejidad: O(n log n) tiempo, O(n) espacio.
     */
    public List<Paquete<?>> verPendientes() {
        Queue<Entrada> copia = new PriorityQueue<>(cola);
        List<Paquete<?>> resultado = new ArrayList<>(copia.size());
        while (!copia.isEmpty()) {
            resultado.add(copia.poll().paquete());
        }
        return resultado;
    }

    private record Entrada(Paquete<?> paquete, boolean prioritario, long ordenLlegada) {
    }
}
