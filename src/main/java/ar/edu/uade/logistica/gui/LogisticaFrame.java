package ar.edu.uade.logistica.gui;

import ar.edu.uade.logistica.model.Inventario;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.service.LogisticaService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LogisticaFrame extends JFrame {
    private static final Color FONDO = new Color(240, 244, 248);
    private static final Color TARJETA = Color.WHITE;
    private static final Color BORDE = new Color(210, 218, 230);
    private static final Color PRIMARIO = new Color(13, 71, 161);
    private static final Color PRIMARIO_SUAVE = new Color(232, 240, 254);
    private static final Color SECUNDARIO = new Color(0, 137, 123);
    private static final Color BOTON_SECUNDARIO = Color.WHITE;
    private static final Color TEXTO = new Color(28, 41, 56);
    private static final Color TEXTO_SUAVE = new Color(92, 107, 128);
    private static final Font TITULO_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font SECCION_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font METRICA_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LogisticaService service;
    private final JLabel pendientesLabel = new JLabel("0");
    private final JLabel camionLabel = new JLabel("0");
    private final JLabel depositosLabel = new JLabel("0");
    private final JLabel rutasLabel = new JLabel("0");
    private final JLabel estadoLabel = new JLabel("Esperando inventario o carga manual.");
    private final JLabel ultimaActividadLabel = new JLabel("Sin actividad todavia.");
    private final JTextArea logArea = new JTextArea();

    private final JTextField idField = new JTextField();
    private final JTextField pesoField = new JTextField();
    private final JTextField destinoField = new JTextField();
    private final JTextField contenidoField = new JTextField();
    private final JCheckBox urgenteCheck = new JCheckBox("Urgente");

    private final JTextField nivelField = new JTextField();
    private final JTextField origenField = new JTextField();
    private final JTextField destinoRutaField = new JTextField();
    private boolean inventarioCargado;

    public LogisticaFrame(LogisticaService service) {
        super("Logi-UADE 2026");
        this.service = service;
        configurarVentana();
        configurarEstilosBase();
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
        setMinimumSize(new Dimension(1180, 780));
        setLocationRelativeTo(null);
    }

    private void configurarEstilosBase() {
        getContentPane().setBackground(FONDO);

        configurarMetrica(pendientesLabel);
        configurarMetrica(camionLabel);
        configurarMetrica(depositosLabel);
        configurarMetrica(rutasLabel);

        estadoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        estadoLabel.setForeground(PRIMARIO.darker());
        ultimaActividadLabel.setFont(BODY_FONT);
        ultimaActividadLabel.setForeground(TEXTO_SUAVE);

        configurarCampo(idField, "Ej. PKG-001");
        configurarCampo(pesoField, "Ej. 12.5");
        configurarCampo(destinoField, "Ej. Rosario");
        configurarCampo(contenidoField, "Ej. Electronica");
        configurarCampo(nivelField, "Ej. 2");
        configurarCampo(origenField, "Ej. 50");
        configurarCampo(destinoRutaField, "Ej. 80");

        urgenteCheck.setOpaque(false);
        urgenteCheck.setFont(BODY_FONT);
        urgenteCheck.setForeground(TEXTO);
        urgenteCheck.setText("Marcar como urgente");

        logArea.setEditable(false);
        logArea.setRows(14);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(247, 250, 252));
        logArea.setForeground(TEXTO);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(FONDO);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.add(crearHeader(), BorderLayout.NORTH);
        root.add(crearCentro(), BorderLayout.CENTER);
        return root;
    }

    private JPanel crearHeader() {
        JPanel panel = crearTarjeta(new BorderLayout(18, 0));
        panel.setBackground(PRIMARIO_SUAVE);

        JPanel textoPanel = new JPanel();
        textoPanel.setOpaque(false);
        textoPanel.setLayout(new BoxLayout(textoPanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Centro de Operaciones Logisticas");
        titulo.setFont(TITULO_FONT);
        titulo.setForeground(PRIMARIO.darker());

        JLabel descripcion = new JLabel("<html>Una vista mas clara para operar paquetes, camion, depositos y rutas desde la misma pantalla.</html>");
        descripcion.setFont(BODY_FONT);
        descripcion.setForeground(TEXTO_SUAVE);

        JPanel estado = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        estado.setOpaque(false);
        estado.add(crearChip("Estado"));
        estado.add(estadoLabel);

        textoPanel.add(titulo);
        textoPanel.add(Box.createVerticalStrut(6));
        textoPanel.add(descripcion);
        textoPanel.add(Box.createVerticalStrut(14));
        textoPanel.add(estado);

        JPanel acciones = new JPanel(new GridLayout(3, 1, 0, 10));
        acciones.setOpaque(false);

        JButton cargarInventarioButton = crearBotonPrimario("Cargar inventario JSON");
        cargarInventarioButton.addActionListener(e -> cargarInventarioDesdeArchivo());

        JButton demoButton = crearBotonSecundario("Cargar JSON local");
        demoButton.addActionListener(e -> cargarInventarioDemo());

        JButton limpiarLogButton = crearBotonSecundario("Limpiar actividad");
        limpiarLogButton.addActionListener(e -> limpiarActividad());

        acciones.add(cargarInventarioButton);
        acciones.add(demoButton);
        acciones.add(limpiarLogButton);

        panel.add(textoPanel, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane crearCentro() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, crearTabs(), crearSidebar());
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setBackground(FONDO);
        return splitPane;
    }

    private JTabbedPane crearTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Paquetes", crearPanelPaquetes());
        tabs.addTab("Camion", crearPanelCamion());
        tabs.addTab("Depositos", crearPanelDepositos());
        tabs.addTab("Rutas", crearPanelRutas());
        return tabs;
    }

    private JPanel crearPanelPaquetes() {
        JPanel panel = crearPanelTab();

        JPanel formulario = crearTarjeta(new BorderLayout(0, 12));
        formulario.add(crearDescripcionSeccion("Alta manual de paquetes", "Carga paquetes al centro. Los urgentes o de mas de 50 kg suben con prioridad."), BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(5, 2, 10, 10));
        campos.setOpaque(false);
        campos.add(crearLabelCampo("ID"));
        campos.add(idField);
        campos.add(crearLabelCampo("Peso"));
        campos.add(pesoField);
        campos.add(crearLabelCampo("Destino"));
        campos.add(destinoField);
        campos.add(crearLabelCampo("Contenido"));
        campos.add(contenidoField);
        campos.add(crearLabelCampo("Prioridad"));
        campos.add(urgenteCheck);
        formulario.add(campos, BorderLayout.CENTER);

        JPanel acciones = crearTarjeta(new BorderLayout(0, 12));
        acciones.add(crearDescripcionSeccion("Flujo del centro", "Procesa el siguiente paquete y cargalo al camion con un solo paso."), BorderLayout.NORTH);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);

        JButton altaButton = crearBotonPrimario("Agregar al centro");
        altaButton.addActionListener(e -> agregarPaqueteManual());

        JButton procesarButton = crearBotonSecundario("Procesar y cargar");
        procesarButton.addActionListener(e -> procesarYCargar());

        botones.add(altaButton);
        botones.add(procesarButton);

        acciones.add(botones, BorderLayout.CENTER);
        acciones.add(crearTextoSecundario("Secuencia recomendada: alta manual, validacion del contador de pendientes y procesamiento al camion."), BorderLayout.SOUTH);

        panel.add(formulario);
        panel.add(Box.createVerticalStrut(14));
        panel.add(acciones);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel crearPanelCamion() {
        JPanel panel = crearPanelTab();

        JPanel acciones = crearTarjeta(new BorderLayout(0, 12));
        acciones.add(crearDescripcionSeccion("Operacion LIFO del camion", "El ultimo paquete en entrar es el primero en salir o deshacerse."), BorderLayout.NORTH);

        JPanel botones = new JPanel(new GridLayout(3, 1, 0, 10));
        botones.setOpaque(false);

        JButton verCargaButton = crearBotonPrimario("Ver carga actual");
        verCargaButton.addActionListener(e -> verCargaCamion());

        JButton deshacerButton = crearBotonSecundario("Deshacer ultima carga");
        deshacerButton.addActionListener(e -> ejecutarAccion("Ultima carga removida", service::deshacerUltimaCargaCamion));

        JButton descargarButton = crearBotonSecundario("Descargar tope");
        descargarButton.addActionListener(e -> ejecutarAccion("Paquete descargado", service::descargarCamion));

        botones.add(verCargaButton);
        botones.add(deshacerButton);
        botones.add(descargarButton);

        acciones.add(botones, BorderLayout.CENTER);
        acciones.add(crearTextoSecundario("Antes de descargar, revisa el orden actual desde el panel de actividad para evitar errores de despacho."), BorderLayout.SOUTH);

        panel.add(acciones);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel crearPanelDepositos() {
        JPanel panel = crearPanelTab();

        JPanel acciones = crearTarjeta(new BorderLayout(0, 12));
        acciones.add(crearDescripcionSeccion("ABB y auditoria postorden", "Audita depositos pendientes y consulta nodos por nivel del arbol."), BorderLayout.NORTH);

        JButton auditarButton = crearBotonPrimario("Ejecutar auditoria");
        auditarButton.addActionListener(e -> auditarDepositos());

        JPanel nivelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        nivelPanel.setOpaque(false);
        nivelField.setPreferredSize(new Dimension(140, 34));

        JButton nivelButton = crearBotonSecundario("Consultar nivel");
        nivelButton.addActionListener(e -> consultarNivel());

        nivelPanel.add(crearLabelCampo("Nivel"));
        nivelPanel.add(nivelField);
        nivelPanel.add(nivelButton);

        acciones.add(auditarButton, BorderLayout.CENTER);
        acciones.add(nivelPanel, BorderLayout.SOUTH);

        panel.add(acciones);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel crearPanelRutas() {
        JPanel panel = crearPanelTab();

        JPanel formulario = crearTarjeta(new BorderLayout(0, 12));
        formulario.add(crearDescripcionSeccion("Distancia minima", "Consulta la mejor ruta entre dos depositos cargados en el grafo."), BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(3, 2, 10, 10));
        campos.setOpaque(false);
        campos.add(crearLabelCampo("Deposito origen"));
        campos.add(origenField);
        campos.add(crearLabelCampo("Deposito destino"));
        campos.add(destinoRutaField);

        JButton calcularButton = crearBotonPrimario("Calcular distancia minima");
        calcularButton.addActionListener(e -> calcularDistanciaMinima());

        campos.add(new JLabel(""));
        campos.add(calcularButton);

        formulario.add(campos, BorderLayout.CENTER);
        panel.add(formulario);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.add(crearPanelMetricas());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(crearPanelAccionesRapidas());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(crearPanelActividad());
        return sidebar;
    }

    private JPanel crearPanelMetricas() {
        JPanel panel = crearTarjeta(new BorderLayout(0, 14));
        panel.add(crearTituloSeccion("Resumen en vivo"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.add(crearMetricaCard("Pendientes", pendientesLabel, "Centro"));
        grid.add(crearMetricaCard("En camion", camionLabel, "LIFO"));
        grid.add(crearMetricaCard("Depositos", depositosLabel, "ABB"));
        grid.add(crearMetricaCard("Rutas", rutasLabel, "Grafo"));

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(crearTextoSecundario("Ultima actividad"));
        footer.add(Box.createVerticalStrut(4));
        footer.add(ultimaActividadLabel);

        panel.add(grid, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelAccionesRapidas() {
        JPanel panel = crearTarjeta(new BorderLayout(0, 12));
        panel.add(crearTituloSeccion("Acciones rapidas"), BorderLayout.NORTH);

        JPanel acciones = new JPanel(new GridLayout(3, 1, 0, 8));
        acciones.setOpaque(false);

        JButton procesarButton = crearBotonPrimario("Procesar siguiente");
        procesarButton.addActionListener(e -> procesarYCargar());

        JButton verCargaButton = crearBotonSecundario("Ver carga camion");
        verCargaButton.addActionListener(e -> verCargaCamion());

        JButton auditarButton = crearBotonSecundario("Auditar depositos");
        auditarButton.addActionListener(e -> auditarDepositos());

        acciones.add(procesarButton);
        acciones.add(verCargaButton);
        acciones.add(auditarButton);

        panel.add(acciones, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelActividad() {
        JPanel panel = crearTarjeta(new BorderLayout(0, 12));
        panel.add(crearTituloSeccion("Actividad reciente"), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDE));
        scrollPane.getViewport().setBackground(logArea.getBackground());

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(crearTextoSecundario("El historial del costado te deja seguir cada accion sin cambiar de pestana."), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelTab() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return panel;
    }

    private JPanel crearTarjeta(BorderLayout layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        return panel;
    }

    private void configurarCampo(JTextField field, String tooltip) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXTO);
        field.setBackground(Color.WHITE);
        field.setToolTipText(tooltip);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void configurarMetrica(JLabel label) {
        label.setFont(METRICA_FONT);
        label.setForeground(PRIMARIO.darker());
    }

    private JLabel crearTituloSeccion(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SECCION_FONT);
        label.setForeground(TEXTO);
        return label;
    }

    private JComponent crearDescripcionSeccion(String title, String description) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(crearTituloSeccion(title));
        panel.add(Box.createVerticalStrut(6));
        panel.add(crearTextoSecundario(description));
        return panel;
    }

    private JLabel crearTextoSecundario(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(BODY_FONT);
        label.setForeground(TEXTO_SUAVE);
        return label;
    }

    private JLabel crearLabelCampo(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXTO);
        return label;
    }

    private JLabel crearChip(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(SECUNDARIO.darker());
        label.setBackground(new Color(223, 245, 239));
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private JPanel crearMetricaCard(String title, JLabel valueLabel, String chip) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(true);
        panel.setBackground(new Color(247, 250, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(LABEL_FONT);
        titleLabel.setForeground(TEXTO_SUAVE);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(crearChip(chip));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JButton crearBotonPrimario(String text) {
        JButton button = new JButton(text);
        configurarBoton(button, PRIMARIO, Color.WHITE);
        return button;
    }

    private JButton crearBotonSecundario(String text) {
        JButton button = new JButton(text);
        configurarBoton(button, BOTON_SECUNDARIO, PRIMARIO.darker());
        return button;
    }

    private void configurarBoton(JButton button, Color fondo, Color texto) {
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(fondo);
        button.setForeground(texto);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARIO.darker()),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
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
            Inventario inventario = service.cargarInventario(path);
            aplicarInventarioCargado(path, inventario);
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void cargarInventarioDemo() {
        try {
            Path path = Path.of("data", "inventario-local.json");
            if (!Files.exists(path)) {
                throw new IllegalStateException("No existe el archivo data/inventario-local.json.");
            }
            Inventario inventario = service.cargarInventario(path);
            aplicarInventarioCargado(path, inventario);
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void aplicarInventarioCargado(Path path, Inventario inventario) {
        inventarioCargado = true;
        depositosLabel.setText(String.valueOf(inventario.getDepositos().size()));
        rutasLabel.setText(String.valueOf(inventario.getRutas().size()));
        appendLog("Inventario cargado desde " + path.toAbsolutePath()
                + " | paquetes=" + inventario.getPaquetes().size()
                + ", depositos=" + inventario.getDepositos().size()
                + ", rutas=" + inventario.getRutas().size());
        actualizarEstado();
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
            actualizarEstado();
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
            actualizarEstado();
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
        int pendientes = service.cantidadPendientesCentro();
        int enCamion = service.cantidadPaquetesEnCamion();

        pendientesLabel.setText(String.valueOf(pendientes));
        camionLabel.setText(String.valueOf(enCamion));

        if (!inventarioCargado) {
            depositosLabel.setText("0");
            rutasLabel.setText("0");
        }

        if (pendientes > 0) {
            estadoLabel.setText("Centro con paquetes listos para procesar.");
        } else if (enCamion > 0) {
            estadoLabel.setText("Camion con carga lista para despacho.");
        } else if (inventarioCargado) {
            estadoLabel.setText("Inventario cargado. Sistema sin pendientes.");
        } else {
            estadoLabel.setText("Esperando inventario o carga manual.");
        }
    }

    private void limpiarFormularioPaquete() {
        idField.setText("");
        pesoField.setText("");
        destinoField.setText("");
        contenidoField.setText("");
        urgenteCheck.setSelected(false);
    }

    private void appendLog(String message) {
        String linea = "[" + LocalTime.now().format(HORA_FORMATTER) + "] " + message;
        logArea.append(linea + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
        ultimaActividadLabel.setText(message);
    }

    private void mostrarError(Exception ex) {
        appendLog("Error: " + ex.getMessage());
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void limpiarActividad() {
        logArea.setText("");
        ultimaActividadLabel.setText("Actividad limpiada manualmente.");
    }

    @FunctionalInterface
    private interface AccionConResultado {
        Object ejecutar();
    }
}
