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
 */
public class GrafoDepositos {
    private final Map<Integer, List<Arista>> adyacencias = new HashMap<>();

    /**
     * Agrega un deposito aislado al grafo si no existe.
     * Complejidad: O(1) amortizado tiempo, O(1) espacio.
     */
    public void agregarDeposito(int id) {
        adyacencias.computeIfAbsent(id, ignored -> new ArrayList<>());
    }

    /**
     * Conecta dos depositos con una ruta bidireccional.
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

    private record Arista(int destino, int distancia) {
    }

    private record NodoDistancia(int id, int distancia) {
    }
}
