package ar.edu.uade.logistica.model;

/**
 * Tupla inmutable que representa una ruta del grafo entre dos depositos.
 *
 * <p>Se modela como {@code record} porque el dominio la describe como un trio inmutable
 * (origen, destino, distancia) sin comportamiento propio: {@code record} genera
 * automaticamente constructor canonico, {@code equals}, {@code hashCode} y accesores,
 * evitando boilerplate.
 *
 * <p>La ruta es logicamente no dirigida (ver {@code GrafoDepositos#conectar}), pero se
 * guarda con un origen y un destino fijos para que el JSON de entrada siga teniendo un
 * formato determinista. El grafo se encarga de duplicarla en ambos sentidos.
 */
public record Ruta(int origen, int destino, int distanciaKm) {
    /**
     * Validacion compacta del record: la distancia debe ser positiva para que Dijkstra
     * tenga sentido (aristas no negativas). Si viniera {@code <= 0} desde el JSON lo
     * consideramos dato corrupto y falla la carga de inventario.
     */
    public Ruta {
        if (distanciaKm <= 0) {
            throw new IllegalArgumentException("La distancia debe ser mayor a cero.");
        }
    }
}
