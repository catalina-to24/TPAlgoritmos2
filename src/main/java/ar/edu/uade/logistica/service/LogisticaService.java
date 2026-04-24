package ar.edu.uade.logistica.service;

import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Inventario;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.model.Ruta;
import ar.edu.uade.logistica.persistence.InventarioLoader;
import ar.edu.uade.logistica.structures.ArbolDepositos;
import ar.edu.uade.logistica.structures.Camion;
import ar.edu.uade.logistica.structures.CentroDistribucion;
import ar.edu.uade.logistica.structures.GrafoDepositos;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fachada (Facade pattern) que orquesta los cuatro TDA del sistema: pila del camion,
 * cola prioritaria del centro, ABB de depositos y grafo de rutas.
 *
 * <p>Por que una unica fachada: tanto la CLI como el WebServer necesitan la misma logica
 * de negocio. Centralizarla aqui evita duplicar reglas (p.ej. "procesar y cargar en camion"
 * o "reiniciar estado al cargar un inventario nuevo"). Es el unico punto que conoce
 * los TDA internos; las capas externas solo hablan con este servicio.
 *
 * <p>Thread-safety: este servicio NO es thread-safe por si mismo. El {@code WebServer}
 * lo sincroniza externamente con {@code synchronized (service)} porque el
 * {@code HttpServer} de la JDK puede atender requests en paralelo.
 */
public class LogisticaService {
    private Camion camion = new Camion();
    private CentroDistribucion centroDistribucion = new CentroDistribucion();
    private ArbolDepositos arbolDepositos = new ArbolDepositos();
    private GrafoDepositos grafoDepositos = new GrafoDepositos();
    private final InventarioLoader inventarioLoader = new InventarioLoader();

    /**
     * Carga un inventario desde archivo y puebla los cuatro TDA.
     *
     * <p>Antes de cargar se reinician las estructuras: la consigna asume un unico dataset
     * activo, asi que re-cargar debe descartar el anterior para no mezclar paquetes y
     * depositos de corridas distintas.
     */
    public Inventario cargarInventario(Path path) throws IOException {
        reiniciarEstado();
        Inventario inventario = inventarioLoader.cargar(path);
        inventario.getPaquetes().forEach(centroDistribucion::recibir);
        for (Deposito deposito : inventario.getDepositos()) {
            arbolDepositos.insertar(deposito);
            grafoDepositos.agregarDeposito(deposito.getId());
        }
        for (Ruta ruta : inventario.getRutas()) {
            grafoDepositos.conectar(ruta);
        }
        return inventario;
    }

    /**
     * Alta manual de paquete (no viene del JSON). Se encola directo en el centro para
     * que entre al mismo flujo de prioridad que los cargados desde inventario.
     */
    public Paquete<String> crearPaqueteManual(String id, double peso, String destino, String contenido, boolean urgente, int minutosIngreso) {
        Paquete<String> paquete = new Paquete<>(id, peso, destino, contenido, urgente, minutosIngreso);
        centroDistribucion.recibir(paquete);
        return paquete;
    }

    /** Extrae el proximo paquete del centro respetando prioridad y FIFO de desempate. */
    public Paquete<?> procesarSiguienteEnCentro() {
        return centroDistribucion.procesarSiguiente();
    }

    /** Apila un paquete previamente sacado del centro en el camion. */
    public void cargarEnCamion(Paquete<?> paquete) {
        camion.cargar(paquete);
    }

    /** Devuelve el tope del camion para corregir un error de destino. */
    public Paquete<?> deshacerUltimaCargaCamion() {
        return camion.deshacerUltimaCarga();
    }

    /** Descarga el tope del camion (entrega en destino). */
    public Paquete<?> descargarCamion() {
        return camion.descargar();
    }

    /** Snapshot de la carga del camion para la UI (copia, no referencia interna). */
    public List<Paquete<?>> verCargaCamion() {
        return camion.verCargaActual();
    }

    /** Snapshot de los pendientes del centro ordenados por prioridad y llegada. */
    public List<Paquete<?>> verPendientesCentro() {
        return centroDistribucion.verPendientes();
    }

    /** Paquetes pendientes demorados (mas de 30 minutos en espera). */
    public List<Paquete<?>> verDemoradosCentro() {
        return centroDistribucion.verDemorados();
    }

    /** Listado in-order de depositos (ordenados ascendente por id). */
    public List<Deposito> listarDepositos() {
        return arbolDepositos.listarTodos();
    }

    /** Listado de rutas sin duplicar aristas (el grafo es no dirigido). */
    public List<Ruta> listarRutas() {
        return grafoDepositos.listarRutas();
    }

    /** KPI usado por el endpoint de estado. */
    public int cantidadPaquetesEnCamion() {
        return camion.cantidadPaquetes();
    }

    /** KPI usado por el endpoint de estado. */
    public double pesoTotalCamion() {
        return camion.pesoTotal();
    }

    /** KPI usado por el endpoint de estado. */
    public int cantidadPendientesCentro() {
        return centroDistribucion.cantidadPendientes();
    }

    /** Dijkstra sobre el grafo de rutas (km). */
    public int distanciaMinimaEntreDepositos(int origen, int destino) {
        return grafoDepositos.distanciaMinima(origen, destino);
    }

    /** Recorrido post-orden de auditoria (ver {@link ArbolDepositos#auditarDepositosPendientes}). */
    public List<Integer> auditarDepositos(LocalDateTime fechaReferencia) {
        return arbolDepositos.auditarDepositosPendientes(fechaReferencia);
    }

    /** BFS por nivel del ABB. La raiz es el nivel 0. */
    public List<Deposito> depositosPorNivel(int nivel) {
        return arbolDepositos.obtenerNivel(nivel);
    }

    /** Busqueda puntual por id en el ABB. Lanza {@code NoSuchElementException} si no existe. */
    public Deposito buscarDeposito(int id) {
        return arbolDepositos.buscar(id);
    }

    /**
     * Reconstruye los cuatro TDA desde cero.
     *
     * <p>Se prefiere esto a metodos {@code clear()} en cada estructura: instanciar nuevas
     * tiene el mismo costo O(1) conceptual y garantiza que no quede estado colgado (por
     * ejemplo, el contador de secuencia del centro que seguiria creciendo tras un clear).
     */
    private void reiniciarEstado() {
        camion = new Camion();
        centroDistribucion = new CentroDistribucion();
        arbolDepositos = new ArbolDepositos();
        grafoDepositos = new GrafoDepositos();
    }
}
