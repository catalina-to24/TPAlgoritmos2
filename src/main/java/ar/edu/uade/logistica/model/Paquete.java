package ar.edu.uade.logistica.model;

import java.util.Objects;

public class Paquete<T> {
    private final String id;
    private final double peso;
    private final String destino;
    private final T contenido;
    private final boolean urgente;

    public Paquete(String id, double peso, String destino, T contenido, boolean urgente) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del paquete es obligatorio.");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("El destino del paquete es obligatorio.");
        }
        if (peso <= 0) {
            throw new IllegalArgumentException("El peso del paquete debe ser mayor a cero.");
        }
        this.id = id;
        this.peso = peso;
        this.destino = destino;
        this.contenido = contenido;
        this.urgente = urgente;
    }

    public String getId() {
        return id;
    }

    public double getPeso() {
        return peso;
    }

    public String getDestino() {
        return destino;
    }

    public T getContenido() {
        return contenido;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public boolean requierePrioridad() {
        return urgente || peso > 50.0;
    }

    @Override
    public String toString() {
        return "Paquete{" +
                "id='" + id + '\'' +
                ", peso=" + peso +
                ", destino='" + destino + '\'' +
                ", contenido=" + contenido +
                ", urgente=" + urgente +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Paquete<?> paquete)) {
            return false;
        }
        return Objects.equals(id, paquete.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
