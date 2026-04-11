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

public class LogisticaService {
    private Camion camion = new Camion();
    private CentroDistribucion centroDistribucion = new CentroDistribucion();
    private ArbolDepositos arbolDepositos = new ArbolDepositos();
    private GrafoDepositos grafoDepositos = new GrafoDepositos();
    private final InventarioLoader inventarioLoader = new InventarioLoader();

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

    public Paquete<String> crearPaqueteManual(String id, double peso, String destino, String contenido, boolean urgente) {
        Paquete<String> paquete = new Paquete<>(id, peso, destino, contenido, urgente);
        centroDistribucion.recibir(paquete);
        return paquete;
    }

    public Paquete<?> procesarSiguienteEnCentro() {
        return centroDistribucion.procesarSiguiente();
    }

    public void cargarEnCamion(Paquete<?> paquete) {
        camion.cargar(paquete);
    }

    public Paquete<?> deshacerUltimaCargaCamion() {
        return camion.deshacerUltimaCarga();
    }

    public Paquete<?> descargarCamion() {
        return camion.descargar();
    }

    public List<Paquete<?>> verCargaCamion() {
        return camion.verCargaActual();
    }

    public List<Paquete<?>> verPendientesCentro() {
        return centroDistribucion.verPendientes();
    }

    public List<Deposito> listarDepositos() {
        return arbolDepositos.listarTodos();
    }

    public List<Ruta> listarRutas() {
        return grafoDepositos.listarRutas();
    }

    public int cantidadPaquetesEnCamion() {
        return camion.cantidadPaquetes();
    }

    public double pesoTotalCamion() {
        return camion.pesoTotal();
    }

    public int cantidadPendientesCentro() {
        return centroDistribucion.cantidadPendientes();
    }

    public int distanciaMinimaEntreDepositos(int origen, int destino) {
        return grafoDepositos.distanciaMinima(origen, destino);
    }

    public List<Integer> auditarDepositos(LocalDateTime fechaReferencia) {
        return arbolDepositos.auditarDepositosPendientes(fechaReferencia);
    }

    public List<Deposito> depositosPorNivel(int nivel) {
        return arbolDepositos.obtenerNivel(nivel);
    }

    public Deposito buscarDeposito(int id) {
        return arbolDepositos.buscar(id);
    }

    private void reiniciarEstado() {
        camion = new Camion();
        centroDistribucion = new CentroDistribucion();
        arbolDepositos = new ArbolDepositos();
        grafoDepositos = new GrafoDepositos();
    }
}
