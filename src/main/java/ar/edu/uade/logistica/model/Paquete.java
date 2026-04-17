package ar.edu.uade.logistica.model;

import java.util.Objects;

/**
 * TDA generico de paquete logistico. El parametro de tipo {@code T} permite transportar
 * distintos tipos de contenido (Electronica, Alimentos, Fragiles, ...). Todas las
 * operaciones son O(1) en tiempo y espacio.
 *
 * <p>Por que generico: la consigna del TPO exige que {@code Paquete<T>} sea generico para
 * que el tipo de contenido se conozca en tiempo de compilacion y no se pierda informacion
 * al manipular colecciones de paquetes. Usar un {@code Object contenido} rompia ese requisito.
 *
 * <p>Por que inmutable (todos los campos {@code final}): los paquetes son datos de dominio
 * que circulan por varias estructuras (cola prioritaria, pila del camion). La inmutabilidad
 * evita que una estructura modifique accidentalmente el estado observado por otra.
 */
public class Paquete<T> {
    private final String id;
    private final double peso;
    private final String destino;
    private final T contenido;
    private final boolean urgente;

    /**
     * Construye un paquete validando las invariantes del dominio.
     *
     * <p>Las validaciones fallan temprano (fail-fast) porque un paquete invalido envenena
     * el resto del pipeline: encolarlo, apilarlo y despues descubrir que el peso es cero
     * complica la trazabilidad del error.
     *
     * @throws IllegalArgumentException si el id o destino estan vacios, o el peso no es positivo.
     */
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

    /** Identificador unico del paquete; usado como clave de igualdad. */
    public String getId() {
        return id;
    }

    /** Peso en kilogramos. Participa del calculo de prioridad ({@code > 50 kg}). */
    public double getPeso() {
        return peso;
    }

    /** Destino logico del paquete (p.ej. nombre de la ciudad). */
    public String getDestino() {
        return destino;
    }

    /** Contenido tipado por el parametro {@code T}. */
    public T getContenido() {
        return contenido;
    }

    /** Indica si el paquete fue marcado como urgente al crearse. */
    public boolean isUrgente() {
        return urgente;
    }

    /**
     * Indica si el paquete debe tratarse como prioritario en el centro de distribucion.
     * Por consigna, son prioritarios los urgentes o los que pesan mas de 50 kg.
     *
     * <p>Se calcula aca (y no en {@link ar.edu.uade.logistica.structures.CentroDistribucion})
     * para que la regla de negocio viva con la entidad y no se duplique en cada consumidor.
     */
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

    /**
     * Dos paquetes se consideran iguales si comparten id. Esto evita duplicados logicos
     * aunque cambie el resto del payload (por si se rehidratan desde otra fuente).
     */
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
