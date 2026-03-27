package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Deposito;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class ArbolDepositos {
    private Nodo raiz;

    public void insertar(Deposito deposito) {
        raiz = insertarRecursivo(raiz, deposito);
    }

    public Deposito buscar(int id) {
        Nodo actual = raiz;
        while (actual != null) {
            if (id == actual.deposito.getId()) {
                return actual.deposito;
            }
            actual = id < actual.deposito.getId() ? actual.izquierdo : actual.derecho;
        }
        throw new NoSuchElementException("No existe un deposito con ID " + id);
    }

    public List<Integer> auditarDepositosPendientes(LocalDateTime fechaCorte) {
        List<Integer> auditados = new ArrayList<>();
        auditarPostOrden(raiz, fechaCorte.minusDays(30), auditados);
        return auditados;
    }

    public List<Deposito> obtenerNivel(int nivelObjetivo) {
        List<Deposito> resultado = new ArrayList<>();
        if (raiz == null || nivelObjetivo < 0) {
            return resultado;
        }
        Queue<NodoNivel> cola = new ArrayDeque<>();
        cola.offer(new NodoNivel(raiz, 0));
        while (!cola.isEmpty()) {
            NodoNivel actual = cola.poll();
            if (actual.nivel == nivelObjetivo) {
                resultado.add(actual.nodo.deposito);
            }
            if (actual.nivel > nivelObjetivo) {
                break;
            }
            if (actual.nodo.izquierdo != null) {
                cola.offer(new NodoNivel(actual.nodo.izquierdo, actual.nivel + 1));
            }
            if (actual.nodo.derecho != null) {
                cola.offer(new NodoNivel(actual.nodo.derecho, actual.nivel + 1));
            }
        }
        return resultado;
    }

    private Nodo insertarRecursivo(Nodo actual, Deposito deposito) {
        if (actual == null) {
            return new Nodo(deposito);
        }
        if (deposito.getId() < actual.deposito.getId()) {
            actual.izquierdo = insertarRecursivo(actual.izquierdo, deposito);
        } else if (deposito.getId() > actual.deposito.getId()) {
            actual.derecho = insertarRecursivo(actual.derecho, deposito);
        } else {
            throw new IllegalArgumentException("Ya existe un deposito con ID " + deposito.getId());
        }
        return actual;
    }

    private void auditarPostOrden(Nodo nodo, LocalDateTime limite, List<Integer> auditados) {
        if (nodo == null) {
            return;
        }
        auditarPostOrden(nodo.izquierdo, limite, auditados);
        auditarPostOrden(nodo.derecho, limite, auditados);
        LocalDateTime fechaUltimaAuditoria = nodo.deposito.getFechaUltimaAuditoria();
        boolean requiereAuditoria = fechaUltimaAuditoria == null || fechaUltimaAuditoria.isBefore(limite);
        nodo.deposito.setVisitado(requiereAuditoria);
        if (requiereAuditoria) {
            auditados.add(nodo.deposito.getId());
        }
    }

    private static final class Nodo {
        private final Deposito deposito;
        private Nodo izquierdo;
        private Nodo derecho;

        private Nodo(Deposito deposito) {
            this.deposito = deposito;
        }
    }

    private record NodoNivel(Nodo nodo, int nivel) {
    }
}
