package ar.edu.uade.logistica.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Deposito {
    private final int id;
    private final String nombre;
    private boolean visitado;
    private LocalDateTime fechaUltimaAuditoria;

    public Deposito(int id, String nombre, boolean visitado, LocalDateTime fechaUltimaAuditoria) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del deposito es obligatorio.");
        }
        this.id = id;
        this.nombre = nombre;
        this.visitado = visitado;
        this.fechaUltimaAuditoria = fechaUltimaAuditoria;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isVisitado() {
        return visitado;
    }

    public void setVisitado(boolean visitado) {
        this.visitado = visitado;
    }

    public LocalDateTime getFechaUltimaAuditoria() {
        return fechaUltimaAuditoria;
    }

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
