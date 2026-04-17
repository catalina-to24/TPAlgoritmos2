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
 *
 * <p>Por que {@link PriorityQueue} con {@link Comparator} compuesto: la consigna pide
 * "cola prioritaria", y la JDK provee un heap binario listo para usar. El comparator
 * ordena primero por {@code prioritario} descendente y desempata por un contador monotonico
 * ({@code ordenLlegada}) ascendente, lo que emula FIFO estable dentro de cada nivel de
 * prioridad sin depender del orden no especificado de la heap.
 *
 * <p>Por que {@link AtomicLong}: el WebServer puede serializar llamadas con un lock,
 * pero el contador se hizo atomico para que no dependa del lock externo. Asi, incluso si
 * el dia de manana se accediera sin {@code synchronized}, cada paquete recibe un numero
 * de secuencia unico y creciente.
 */
public class CentroDistribucion {
    private final AtomicLong secuencia = new AtomicLong();
    private final Queue<Entrada> cola = new PriorityQueue<>(Comparator
            .comparing(Entrada::prioritario)
            .reversed()
            .thenComparingLong(Entrada::ordenLlegada));

    /**
     * Encola un paquete calculando su prioridad en el momento de ingreso.
     *
     * <p>Congelar {@code prioritario} al recibir evita que un cambio posterior en las
     * reglas (por ejemplo, si se mutara el peso) reubique elementos ya encolados: una
     * heap binaria no soporta reordenamiento barato.
     *
     * Complejidad: O(log n) tiempo, O(1) espacio.
     */
    public void recibir(Paquete<?> paquete) {
        cola.offer(new Entrada(paquete, paquete.requierePrioridad(), secuencia.getAndIncrement()));
    }

    /**
     * Extrae el paquete de mayor prioridad.
     *
     * <p>Se lanza {@link NoSuchElementException} cuando no hay pendientes para que el
     * WebServer lo traduzca a 400 (error semantico del dominio) en lugar de 500.
     *
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
     *
     * <p>Se drena una copia de la heap para garantizar el orden: iterar {@link PriorityQueue}
     * directamente NO da orden de prioridad (solo el polling sucesivo lo hace). Sin esta
     * copia, la UI veria los paquetes en orden interno del heap, que es confuso.
     *
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

    /**
     * Wrapper interno que asocia cada paquete a su prioridad y orden de llegada.
     * Es un {@code record} porque no tiene comportamiento y solo agrupa datos.
     */
    private record Entrada(Paquete<?> paquete, boolean prioritario, long ordenLlegada) {
    }
}
