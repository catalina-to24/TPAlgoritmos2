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
 *
 * <p>Por que implementado a mano y no {@code TreeMap}: la consigna lo pide explicitamente.
 * Usar {@code TreeMap} seria mas corto pero incumple el requisito academico del TPO.
 *
 * <p>Por que sin balanceo (no AVL/RB): la consigna no pide balanceo y el dataset de
 * depositos es chico. Agregar balanceo complicaria el codigo sin beneficio perceptible
 * dentro del alcance del TPO.
 */
public class ArbolDepositos {
    private Nodo raiz;

    /**
     * Inserta un deposito manteniendo la propiedad de orden del ABB.
     *
     * <p>Lanza {@link IllegalArgumentException} si el id ya existe (via
     * {@link #insertarRecursivo}) para evitar duplicados silenciosos, que romperian
     * la propiedad del ABB.
     *
     * Complejidad: O(log n) promedio, O(n) peor caso; O(h) espacio de pila.
     */
    public void insertar(Deposito deposito) {
        raiz = insertarRecursivo(raiz, deposito);
    }

    /**
     * Busca un deposito por id.
     *
     * <p>Se implementa iterativo en vez de recursivo para no consumir stack proporcional
     * a la altura: la busqueda es el camino caliente que expone el endpoint
     * {@code GET /api/depositos/{id}}.
     *
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
     *
     * <p>La semantica de "visitado = true para los NO auditados recientemente" es literal
     * de la consigna y parece invertida a primera vista (ver nota en CLAUDE.md):
     * {@code visitado} aqui funciona como una marca "este deposito requiere auditoria".
     *
     * <p>Por que post-orden y no in/pre-orden: lo pide la consigna. Ademas, conceptualmente
     * tiene sentido auditar primero los hijos y despues el padre (como "cerrar" niveles
     * antes de subir).
     *
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
     *
     * <p>Por que BFS en vez de recursion con contador de profundidad: la consigna pide
     * "reporte por nivel N", y BFS modela ese recorrido de forma natural. Ademas deja
     * el orden izquierda-a-derecha dentro de cada nivel, que es el esperado por la UI.
     *
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
            // Truco clasico de BFS por niveles: snapshot del tamano de la cola al entrar
            // al nivel para saber cuantos nodos componen el nivel actual.
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
     *
     * <p>In-order sobre un ABB entrega los elementos ordenados por clave, que es lo que
     * la UI necesita para mostrar el listado general sin tener que reordenar en JS.
     *
     * Complejidad: O(n) tiempo, O(h) espacio por pila de recursion.
     */
    public List<Deposito> listarTodos() {
        List<Deposito> resultado = new ArrayList<>();
        enOrden(raiz, resultado);
        return resultado;
    }

    /** Recorrido in-order recursivo: izq -> nodo -> der. */
    private void enOrden(Nodo nodo, List<Deposito> resultado) {
        if (nodo == null) {
            return;
        }
        enOrden(nodo.izquierdo, resultado);
        resultado.add(nodo.deposito);
        enOrden(nodo.derecho, resultado);
    }

    /**
     * Inserta recursivamente respetando la propiedad del ABB.
     * Rechazar el id duplicado evita tener dos nodos con la misma clave, caso que haria
     * ambigua la busqueda y la auditoria.
     */
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

    /**
     * Nucleo del post-orden: primero izq, luego der y por ultimo el propio nodo.
     * Marca {@code visitado = true} cuando el deposito no tiene fecha de auditoria o
     * es anterior al limite ({@code fechaCorte - 30 dias}). La consigna es literal aca.
     */
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

    /**
     * Nodo interno del ABB. Se modela como clase estatica anidada para no atar cada nodo
     * a una instancia del arbol (ahorra un puntero implicito por nodo).
     */
    private static final class Nodo {
        private final Deposito deposito;
        private Nodo izquierdo;
        private Nodo derecho;

        private Nodo(Deposito deposito) {
            this.deposito = deposito;
        }
    }
}
