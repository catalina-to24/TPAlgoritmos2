package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Ruta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * Grafo NO dirigido de depositos implementado como lista de adyacencia. Cada ruta se
 * registra en ambas direcciones. La distancia minima entre dos depositos se calcula con
 * Dijkstra sobre los kilometros declarados de cada arista.
 *
 * <p>Por que lista de adyacencia y no matriz: la consigna lo pide explicitamente y, en
 * grafos dispersos como este (pocas conexiones por nodo), la lista de adyacencia usa
 * O(V + E) memoria en lugar de O(V^2).
 *
 * <p>Por que Dijkstra y no BFS simple: el JSON trae distancias en km (aristas con peso),
 * por lo que BFS daria el camino con menos saltos, no el mas corto en km. Dijkstra con
 * min-heap es O((V+E) log V) y es el algoritmo correcto cuando todos los pesos son
 * no-negativos (que es el caso, por la validacion de {@link Ruta}).
 */
public class GrafoDepositos {
    private final Map<Integer, List<Arista>> adyacencias = new HashMap<>();

    /**
     * Agrega un deposito aislado al grafo si no existe.
     *
     * <p>Se usa {@code computeIfAbsent} para no sobrescribir la lista de adyacencia de
     * un deposito que ya tenia aristas (idempotente).
     *
     * Complejidad: O(1) amortizado tiempo, O(1) espacio.
     */
    public void agregarDeposito(int id) {
        adyacencias.computeIfAbsent(id, ignored -> new ArrayList<>());
    }

    /**
     * Conecta dos depositos con una ruta bidireccional.
     *
     * <p>Se agregan ambos extremos primero por si uno de los dos depositos aun no existia
     * en el mapa (p.ej. un JSON que define rutas sin haber declarado el deposito aislado).
     *
     * Complejidad: O(1) amortizado tiempo, O(1) espacio.
     */
    public void conectar(Ruta ruta) {
        agregarDeposito(ruta.origen());
        agregarDeposito(ruta.destino());
        adyacencias.get(ruta.origen()).add(new Arista(ruta.destino(), ruta.distanciaKm()));
        adyacencias.get(ruta.destino()).add(new Arista(ruta.origen(), ruta.distanciaKm()));
    }

    /**
     * Calcula la distancia minima en km entre dos depositos usando Dijkstra.
     *
     * <p>Patron "lazy Dijkstra": en lugar de mantener la cola actualizada (decrease-key),
     * se insertan entradas nuevas cada vez que se encuentra una distancia menor y se
     * descartan las obsoletas al sacarlas ({@code actual.distancia > distancias[actual.id]}).
     * Es la variante estandar cuando se usa {@link PriorityQueue} de la JDK, que no expone
     * decrease-key.
     *
     * <p>Se cortocircuita en cuanto se saca el nodo destino del heap porque Dijkstra
     * garantiza que en ese momento su distancia ya es la minima definitiva.
     *
     * Complejidad: O((V + E) log V) tiempo, O(V) espacio.
     */
    public int distanciaMinima(int origen, int destino) {
        if (!adyacencias.containsKey(origen) || !adyacencias.containsKey(destino)) {
            throw new NoSuchElementException("Origen o destino inexistente en el grafo.");
        }

        Map<Integer, Integer> distancias = new HashMap<>();
        PriorityQueue<NodoDistancia> cola = new PriorityQueue<>(Comparator.comparingInt(NodoDistancia::distancia));
        cola.offer(new NodoDistancia(origen, 0));
        distancias.put(origen, 0);

        while (!cola.isEmpty()) {
            NodoDistancia actual = cola.poll();
            if (actual.id == destino) {
                return actual.distancia;
            }
            // Entrada obsoleta (encontramos un camino mas corto despues de insertarla): la ignoramos.
            if (actual.distancia > distancias.getOrDefault(actual.id, Integer.MAX_VALUE)) {
                continue;
            }
            for (Arista arista : adyacencias.get(actual.id)) {
                int nuevaDistancia = actual.distancia + arista.distancia;
                if (nuevaDistancia < distancias.getOrDefault(arista.destino, Integer.MAX_VALUE)) {
                    distancias.put(arista.destino, nuevaDistancia);
                    cola.offer(new NodoDistancia(arista.destino, nuevaDistancia));
                }
            }
        }

        throw new NoSuchElementException("No existe ruta entre los depositos indicados.");
    }

    /**
     * Devuelve todas las rutas registradas sin duplicar aristas (por tratarse de un grafo
     * no dirigido).
     *
     * <p>Al guardarse la ruta en ambos sentidos en la lista de adyacencia, aparece dos
     * veces en el recorrido. El filtro {@code origen < arista.destino} garantiza que
     * cada arista no dirigida aparezca exactamente una vez en el listado que ve la UI.
     *
     * Complejidad: O(V + E) tiempo, O(E) espacio.
     */
    public List<Ruta> listarRutas() {
        List<Ruta> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<Arista>> entry : adyacencias.entrySet()) {
            int origen = entry.getKey();
            for (Arista arista : entry.getValue()) {
                if (origen < arista.destino) {
                    resultado.add(new Ruta(origen, arista.destino, arista.distancia));
                }
            }
        }
        return resultado;
    }

    /** Arista del grafo: destino vecino + peso en km. */
    private record Arista(int destino, int distancia) {
    }

    /** Entrada de la heap de Dijkstra: id de nodo + distancia acumulada desde origen. */
    private record NodoDistancia(int id, int distancia) {
    }
}
