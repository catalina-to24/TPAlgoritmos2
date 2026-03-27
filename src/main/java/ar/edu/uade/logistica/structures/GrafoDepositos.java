package ar.edu.uade.logistica.structures;

import ar.edu.uade.logistica.model.Ruta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

public class GrafoDepositos {
    private final Map<Integer, List<Arista>> adyacencias = new HashMap<>();

    public void agregarDeposito(int id) {
        adyacencias.computeIfAbsent(id, ignored -> new ArrayList<>());
    }

    public void conectar(Ruta ruta) {
        agregarDeposito(ruta.origen());
        agregarDeposito(ruta.destino());
        adyacencias.get(ruta.origen()).add(new Arista(ruta.destino(), ruta.distanciaKm()));
        adyacencias.get(ruta.destino()).add(new Arista(ruta.origen(), ruta.distanciaKm()));
    }

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
            for (Arista arista : adyacencias.getOrDefault(actual.id, List.of())) {
                int nuevaDistancia = actual.distancia + arista.distancia;
                if (nuevaDistancia < distancias.getOrDefault(arista.destino, Integer.MAX_VALUE)) {
                    distancias.put(arista.destino, nuevaDistancia);
                    cola.offer(new NodoDistancia(arista.destino, nuevaDistancia));
                }
            }
        }

        throw new NoSuchElementException("No existe ruta entre los depositos indicados.");
    }

    private record Arista(int destino, int distancia) {
    }

    private record NodoDistancia(int id, int distancia) {
    }
}
