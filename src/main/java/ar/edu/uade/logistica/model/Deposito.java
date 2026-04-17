package ar.edu.uade.logistica.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio que representa un deposito logistico. Es el nodo que vive dentro del
 * ABB {@code ArbolDepositos} y tambien en el grafo de rutas.
 *
 * <p>A diferencia de {@link Paquete}, este tipo expone setters porque la auditoria necesita
 * actualizar {@code visitado} y {@code fechaUltimaAuditoria} al recorrer el arbol. El id y
 * el nombre, en cambio, son {@code final} porque identifican al deposito y no deberian cambiar
 * durante la vida del proceso.
 */
public class Deposito {
    private final int id;
    private final String nombre;
    private boolean visitado;
    private LocalDateTime fechaUltimaAuditoria;

    /**
     * Construye un deposito. Falla temprano si el nombre es vacio para evitar que la UI
     * termine mostrando un deposito sin etiqueta legible.
     */
    public Deposito(int id, String nombre, boolean visitado, LocalDateTime fechaUltimaAuditoria) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del deposito es obligatorio.");
        }
        this.id = id;
        this.nombre = nombre;
        this.visitado = visitado;
        this.fechaUltimaAuditoria = fechaUltimaAuditoria;
    }

    /** Identificador unico; es la clave de orden del ABB. */
    public int getId() {
        return id;
    }

    /** Nombre legible del deposito. */
    public String getNombre() {
        return nombre;
    }

    /**
     * Flag que marca si el deposito "requirio auditoria" en la ultima pasada.
     * La semantica es literal de la consigna: se pone en {@code true} a los que NO fueron
     * auditados en los ultimos 30 dias (ver {@code ArbolDepositos.auditarPostOrden}).
     */
    public boolean isVisitado() {
        return visitado;
    }

    /** Mutador usado por el recorrido de auditoria. */
    public void setVisitado(boolean visitado) {
        this.visitado = visitado;
    }

    /** Momento de la ultima auditoria. Puede ser {@code null} si el deposito nunca fue auditado. */
    public LocalDateTime getFechaUltimaAuditoria() {
        return fechaUltimaAuditoria;
    }

    /** Mutador previsto para actualizar la fecha al registrar una nueva auditoria. */
    public void setFechaUltimaAuditoria(LocalDateTime fechaUltimaAuditoria) {
        this.fechaUltimaAuditoria = fechaUltimaAuditoria;
    }

    @Override
    public String toString() {
        return "Deposito{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", visitado=" + visitado +
                ", fechaUltimaAuditoria=" + fechaUltimaAuditoria +
                '}';
    }

    /**
     * Igualdad por id: dos depositos con el mismo id son el mismo nodo logico, aunque
     * difieran en nombre o estado de visitado.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Deposito deposito)) {
            return false;
        }
        return id == deposito.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
