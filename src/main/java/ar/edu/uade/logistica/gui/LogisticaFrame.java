package ar.edu.uade.logistica.gui;

import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.service.LogisticaService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class LogisticaFrame extends JFrame {
    private final LogisticaService service;
    private final JLabel pendientesLabel = new JLabel("Pendientes en centro: 0");
    private final JLabel camionLabel = new JLabel("Paquetes en camion: 0");
    private final JTextArea logArea = new JTextArea();

    private final JTextField idField = new JTextField();
    private final JTextField pesoField = new JTextField();
    private final JTextField destinoField = new JTextField();
    private final JTextField contenidoField = new JTextField();
    private final JCheckBox urgenteCheck = new JCheckBox("Urgente");

    private final JTextField nivelField = new JTextField();
    private final JTextField origenField = new JTextField();
    private final JTextField destinoRutaField = new JTextField();

    public LogisticaFrame(LogisticaService service) {
        super("Logi-UADE 2026");
        this.service = service;
        configurarVentana();
        setContentPane(crearContenido());
        actualizarEstado();
    }

    public static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            LogisticaFrame frame = new LogisticaFrame(new LogisticaService());
            frame.setVisible(true);
        });
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));
        setLocationRelativeTo(null);
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(crearHeader(), BorderLayout.NORTH);
        root.add(crearTabs(), BorderLayout.CENTER);
        root.add(crearLogPanel(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel crearHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("controlShadow")),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JPanel estado = new JPanel(new GridLayout(2, 1, 4, 4));
        estado.add(pendientesLabel);
        estado.add(camionLabel);

        JButton cargarInventarioButton = new JButton("Cargar inventario JSON");
        cargarInventarioButton.addActionListener(e -> cargarInventarioDesdeArchivo());

        panel.add(new JLabel("Sistema de Gestion Logistica"), BorderLayout.WEST);
        panel.add(estado, BorderLayout.CENTER);
        panel.add(cargarInventarioButton, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane crearTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Paquetes", crearPanelPaquetes());
        tabs.addTab("Camion", crearPanelCamion());
        tabs.addTab("Depositos", crearPanelDepositos());
        tabs.addTab("Rutas", crearPanelRutas());
        return tabs;
    }

    private JPanel crearPanelPaquetes() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formulario = new JPanel(new GridLayout(5, 2, 8, 8));
        formulario.add(new JLabel("ID"));
        formulario.add(idField);
        formulario.add(new JLabel("Peso"));
        formulario.add(pesoField);
        formulario.add(new JLabel("Destino"));
        formulario.add(destinoField);
        formulario.add(new JLabel("Contenido"));
        formulario.add(contenidoField);
        formulario.add(new JLabel("Prioridad"));
        formulario.add(urgenteCheck);

        JButton altaButton = new JButton("Agregar paquete al centro");
        altaButton.addActionListener(e -> agregarPaqueteManual());

        JButton procesarButton = new JButton("Procesar y cargar en camion");
        procesarButton.addActionListener(e -> procesarYCargar());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.add(altaButton);
        acciones.add(procesarButton);

        panel.add(formulario, BorderLayout.NORTH);
        panel.add(acciones, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCamion() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton verCargaButton = new JButton("Ver carga actual");
        verCargaButton.addActionListener(e -> verCargaCamion());

        JButton deshacerButton = new JButton("Deshacer ultima carga");
        deshacerButton.addActionListener(e -> ejecutarAccion("Ultima carga removida", service::deshacerUltimaCargaCamion));

        JButton descargarButton = new JButton("Descargar tope del camion");
        descargarButton.addActionListener(e -> ejecutarAccion("Paquete descargado", service::descargarCamion));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.add(verCargaButton);
        acciones.add(deshacerButton);
        acciones.add(descargarButton);

        panel.add(new JLabel("Operaciones LIFO sobre la pila del camion."), BorderLayout.NORTH);
        panel.add(acciones, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelDepositos() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton auditarButton = new JButton("Ejecutar auditoria");
        auditarButton.addActionListener(e -> auditarDepositos());

        JPanel nivelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nivelField.setPreferredSize(new Dimension(80, 28));
        JButton nivelButton = new JButton("Consultar nivel");
        nivelButton.addActionListener(e -> consultarNivel());
        nivelPanel.add(new JLabel("Nivel"));
        nivelPanel.add(nivelField);
        nivelPanel.add(nivelButton);

        JPanel acciones = new JPanel(new GridLayout(2, 1, 8, 8));
        acciones.add(auditarButton);
        acciones.add(nivelPanel);

        panel.add(new JLabel("ABB manual de depositos y auditoria postorden."), BorderLayout.NORTH);
        panel.add(acciones, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelRutas() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formulario = new JPanel(new GridLayout(3, 2, 8, 8));
        formulario.add(new JLabel("Deposito origen"));
        formulario.add(origenField);
        formulario.add(new JLabel("Deposito destino"));
        formulario.add(destinoRutaField);

        JButton calcularButton = new JButton("Calcular distancia minima");
        calcularButton.addActionListener(e -> calcularDistanciaMinima());
        formulario.add(new JLabel(""));
        formulario.add(calcularButton);

        panel.add(new JLabel("Red de rutas con Dijkstra sobre lista de adyacencia."), BorderLayout.NORTH);
        panel.add(formulario, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane crearLogPanel() {
        logArea.setEditable(false);
        logArea.setRows(12);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Actividad"));
        return scrollPane;
    }

    private void cargarInventarioDesdeArchivo() {
        JFileChooser chooser = new JFileChooser(Path.of("data").toAbsolutePath().toFile());
        chooser.setDialogTitle("Seleccionar inventario JSON");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path path = chooser.getSelectedFile().toPath();
            service.cargarInventario(path);
            appendLog("Inventario cargado desde: " + path.toAbsolutePath());
            actualizarEstado();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void agregarPaqueteManual() {
        try {
            Paquete<String> paquete = service.crearPaqueteManual(
                    idField.getText().trim(),
                    Double.parseDouble(pesoField.getText().trim()),
                    destinoField.getText().trim(),
                    contenidoField.getText().trim(),
                    urgenteCheck.isSelected()
            );
            appendLog("Paquete agregado al centro: " + paquete);
            limpiarFormularioPaquete();
            actualizarEstado();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void procesarYCargar() {
        try {
            Paquete<?> paquete = service.procesarSiguienteEnCentro();
            service.cargarEnCamion(paquete);
            appendLog("Paquete procesado y cargado en camion: " + paquete);
            actualizarEstado();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void verCargaCamion() {
        List<Paquete<?>> carga = service.verCargaCamion();
        if (carga.isEmpty()) {
            appendLog("El camion no tiene paquetes cargados.");
            return;
        }
        appendLog("Carga actual del camion:");
        for (Paquete<?> paquete : carga) {
            appendLog(" - " + paquete);
        }
        actualizarEstado();
    }

    private void auditarDepositos() {
        try {
            List<Integer> ids = service.auditarDepositos(LocalDateTime.now());
            if (ids.isEmpty()) {
                appendLog("No hay depositos pendientes de auditoria.");
            } else {
                appendLog("Depositos marcados como visitados: " + ids);
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void consultarNivel() {
        try {
            int nivel = Integer.parseInt(nivelField.getText().trim());
            List<String> depositos = service.depositosPorNivel(nivel);
            if (depositos.isEmpty()) {
                appendLog("No hay depositos en el nivel " + nivel + ".");
                return;
            }
            appendLog("Depositos del nivel " + nivel + ":");
            for (String deposito : depositos) {
                appendLog(" - " + deposito);
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void calcularDistanciaMinima() {
        try {
            int origen = Integer.parseInt(origenField.getText().trim());
            int destino = Integer.parseInt(destinoRutaField.getText().trim());
            int distancia = service.distanciaMinimaEntreDepositos(origen, destino);
            appendLog("Distancia minima entre " + origen + " y " + destino + ": " + distancia + " km");
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void ejecutarAccion(String prefijo, AccionConResultado accion) {
        try {
            Object resultado = accion.ejecutar();
            appendLog(prefijo + ": " + resultado);
            actualizarEstado();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void actualizarEstado() {
        pendientesLabel.setText("Pendientes en centro: " + service.cantidadPendientesCentro());
        camionLabel.setText("Paquetes en camion: " + service.cantidadPaquetesEnCamion());
    }

    private void limpiarFormularioPaquete() {
        idField.setText("");
        pesoField.setText("");
        destinoField.setText("");
        contenidoField.setText("");
        urgenteCheck.setSelected(false);
    }

    private void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface AccionConResultado {
        Object ejecutar();
    }
}
