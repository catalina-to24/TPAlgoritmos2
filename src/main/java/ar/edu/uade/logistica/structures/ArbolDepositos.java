package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Deposito;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Arbol Binario de Busqueda (ABB) implementado desde cero, ordenado por id de deposito.
 * No aplica balanceo: asume una distribucion razonable de ids. En el peor caso (ids ordenados)
 * las operaciones degradan a O(n), pero se mantienen las cotas promedio de un ABB.
 */
public class ArbolDepositos {
    private Nodo raiz;

    /**
     * Inserta un deposito manteniendo la propiedad de orden del ABB.
     * Complejidad: O(log n) promedio, O(n) peor caso; O(h) espacio de pila.
     */
    public void insertar(Deposito deposito) {
        raiz = insertarRecursivo(raiz, deposito);
    }

    /**
     * Busca un deposito por id.
     * Complejidad: O(log n) promedio, O(n) peor caso; O(1) espacio.
     */
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

    /**
     * Recorre el arbol en post-orden y marca como "visitados" los depositos cuya ultima
     * auditoria sea nula o anterior a {@code fechaCorte - 30 dias}. Devuelve los ids
     * visitados en el orden del recorrido.
     * Complejidad: O(n) tiempo, O(h) espacio por pila de recursion.
     */
    public List<Integer> auditarDepositosPendientes(LocalDateTime fechaCorte) {
        List<Integer> auditados = new ArrayList<>();
        auditarPostOrden(raiz, fechaCorte.minusDays(30), auditados);
        return auditados;
    }

    /**
     * Devuelve los depositos ubicados en el nivel indicado (raiz = nivel 0) mediante BFS
     * por niveles.
     * Complejidad: O(n) tiempo en el peor caso, O(w) espacio donde w es el ancho maximo.
     */
    public List<Deposito> obtenerNivel(int nivelObjetivo) {
        List<Deposito> resultado = new ArrayList<>();
        if (raiz == null || nivelObjetivo < 0) {
            return resultado;
        }
        Queue<Nodo> cola = new ArrayDeque<>();
        cola.offer(raiz);
        int nivelActual = 0;
        while (!cola.isEmpty()) {
            int tamanoNivel = cola.size();
            if (nivelActual == nivelObjetivo) {
                for (int i = 0; i < tamanoNivel; i++) {
                    resultado.add(cola.poll().deposito);
                }
                return resultado;
            }
            for (int i = 0; i < tamanoNivel; i++) {
                Nodo actual = cola.poll();
                if (actual.izquierdo != null) {
                    cola.offer(actual.izquierdo);
                }
                if (actual.derecho != null) {
                    cola.offer(actual.derecho);
                }
            }
            nivelActual++;
        }
        return resultado;
    }

    /**
     * Devuelve todos los depositos en orden ascendente por id (recorrido in-order).
     * Complejidad: O(n) tiempo, O(h) espacio por pila de recursion.
     */
    public List<Deposito> listarTodos() {
        List<Deposito> resultado = new ArrayList<>();
        enOrden(raiz, resultado);
        return resultado;
    }

    private void enOrden(Nodo nodo, List<Deposito> resultado) {
        if (nodo == null) {
            return;
        }
        enOrden(nodo.izquierdo, resultado);
        resultado.add(nodo.deposito);
        enOrden(nodo.derecho, resultado);
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
}
