package ar.edu.uade.logistica.model;

public record Ruta(int origen, int destino, int distanciaKm) {
    public Ruta {
        if (distanciaKm <= 0) {
            throw new IllegalArgumentException("La distancia debe ser mayor a cero.");
        }
    }
}
