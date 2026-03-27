package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Paquete;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class CentroDistribucion {
    private final AtomicLong secuencia = new AtomicLong();
    private final Queue<Entrada> cola = new PriorityQueue<>(Comparator
            .comparing(Entrada::prioritario)
            .reversed()
            .thenComparingLong(Entrada::ordenLlegada));

    public void recibir(Paquete<?> paquete) {
        cola.offer(new Entrada(paquete, paquete.requierePrioridad(), secuencia.getAndIncrement()));
    }

    public Paquete<?> procesarSiguiente() {
        Entrada entrada = cola.poll();
        if (entrada == null) {
            throw new NoSuchElementException("No hay paquetes pendientes.");
        }
        return entrada.paquete();
    }

    public int cantidadPendientes() {
        return cola.size();
    }

    private record Entrada(Paquete<?> paquete, boolean prioritario, long ordenLlegada) {
    }
}
