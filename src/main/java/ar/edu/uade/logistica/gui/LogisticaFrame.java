package ar.edu.uade.logistica.gui;

import ar.edu.uade.logistica.gui.ui.ModernButton;
import ar.edu.uade.logistica.gui.ui.ModernTextField;
import ar.edu.uade.logistica.gui.ui.PillTabs;
import ar.edu.uade.logistica.gui.ui.RoundedPanel;
import ar.edu.uade.logistica.gui.ui.Theme;
import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Inventario;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.service.LogisticaService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LogisticaFrame extends JFrame {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LogisticaService service;

    private final JLabel pendientesLabel = metricLabel();
    private final JLabel camionLabel = metricLabel();
    private final JLabel depositosLabel = metricLabel();
    private final JLabel rutasLabel = metricLabel();

    private final StatusPill estadoPill = new StatusPill("Esperando inventario o carga manual");
    private final JLabel ultimaActividadLabel = new JLabel("Sin actividad todavia.");
    private final JTextArea logArea = new JTextArea();

    private final ModernTextField idField = new ModernTextField("Ej. PKG-001");
    private final ModernTextField pesoField = new ModernTextField("Ej. 12.5");
    private final ModernTextField destinoField = new ModernTextField("Ej. Rosario");
    private final ModernTextField contenidoField = new ModernTextField("Ej. Electronica");
    private final JCheckBox urgenteCheck = new JCheckBox("Marcar como urgente");

    private final ModernTextField nivelField = new ModernTextField("Ej. 2");
    private final ModernTextField origenField = new ModernTextField("Ej. 50");
    private final ModernTextField destinoRutaField = new ModernTextField("Ej. 80");

    private boolean inventarioCargado;

    public LogisticaFrame(LogisticaService service) {
        super("Logi-UADE 2026");
        this.service = service;
        configurarVentana();
        configurarElementos();
        setContentPane(crearContenido());
        actualizarEstado();
    }

    public static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("ToolTip.background", Theme.TEXT);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", Theme.SMALL);
        SwingUtilities.invokeLater(() -> {
            LogisticaFrame frame = new LogisticaFrame(new LogisticaService());
            frame.setVisible(true);
        });
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1240, 820));
        setLocationRelativeTo(null);
    }

    private void configurarElementos() {
        urgenteCheck.setOpaque(false);
        urgenteCheck.setFont(Theme.BODY);
        urgenteCheck.setForeground(Theme.TEXT);
        urgenteCheck.setFocusPainted(false);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(Theme.MONO);
        logArea.setForeground(Theme.TEXT);
        logArea.setBackground(Theme.SURFACE_ALT);
        logArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        ultimaActividadLabel.setFont(Theme.BODY);
        ultimaActividadLabel.setForeground(Theme.TEXT_MUTED);
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        root.add(crearHeader(), BorderLayout.NORTH);
        root.add(crearCuerpo(), BorderLayout.CENTER);
        return root;
    }

    private JComponent crearHeader() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(24, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(new LogoDot(14));
        JLabel brandLabel = new JLabel("Logi-UADE 2026");
        brandLabel.setFont(Theme.SMALL_BOLD);
        brandLabel.setForeground(Theme.PRIMARY);
        brand.add(brandLabel);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Centro de Operaciones Logisticas");
        titulo.setFont(Theme.DISPLAY);
        titulo.setForeground(Theme.TEXT);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        titulo.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));

        JLabel descripcion = new JLabel("<html>Operacion unificada de paquetes, camion, depositos y rutas con metricas en vivo.</html>");
        descripcion.setFont(Theme.BODY);
        descripcion.setForeground(Theme.TEXT_MUTED);
        descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);

        estadoPill.setAlignmentX(Component.LEFT_ALIGNMENT);

        izquierda.add(brand);
        izquierda.add(titulo);
        izquierda.add(descripcion);
        izquierda.add(Box.createVerticalStrut(14));
        izquierda.add(estadoPill);

        JPanel acciones = new JPanel(new GridLayout(3, 1, 0, 10));
        acciones.setOpaque(false);

        ModernButton cargarInventarioBtn = new ModernButton("Cargar inventario JSON", ModernButton.Variant.PRIMARY);
        cargarInventarioBtn.addActionListener(e -> cargarInventarioDesdeArchivo());

        ModernButton demoBtn = new ModernButton("Cargar JSON local", ModernButton.Variant.SECONDARY);
        demoBtn.addActionListener(e -> cargarInventarioDemo());

        ModernButton limpiarBtn = new ModernButton("Limpiar actividad", ModernButton.Variant.GHOST);
        limpiarBtn.addActionListener(e -> limpiarActividad());

        acciones.add(cargarInventarioBtn);
        acciones.add(demoBtn);
        acciones.add(limpiarBtn);

        JPanel accionesWrapper = new JPanel(new BorderLayout());
        accionesWrapper.setOpaque(false);
        accionesWrapper.add(acciones, BorderLayout.NORTH);

        panel.add(izquierda, BorderLayout.CENTER);
        panel.add(accionesWrapper, BorderLayout.EAST);
        return panel;
    }

    private JComponent crearCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout(20, 0));
        cuerpo.setOpaque(false);
        cuerpo.add(crearPaneles(), BorderLayout.CENTER);
        cuerpo.add(crearSidebar(), BorderLayout.EAST);
        return cuerpo;
    }

    private JComponent crearPaneles() {
        PillTabs tabs = new PillTabs();
        tabs.addTab("Paquetes", crearPanelPaquetes());
        tabs.addTab("Camion", crearPanelCamion());
        tabs.addTab("Depositos", crearPanelDepositos());
        tabs.addTab("Rutas", crearPanelRutas());
        return tabs;
    }

    private JComponent crearPanelPaquetes() {
        JPanel contenedor = verticalContainer();

        RoundedPanel formulario = crearCard();
        formulario.setLayout(new BorderLayout(0, 18));
        formulario.add(headerSeccion("Alta manual de paquetes",
                "Carga paquetes al centro. Urgentes o mas de 50 kg suben con prioridad."), BorderLayout.NORTH);
        formulario.add(construirFormularioPaquete(), BorderLayout.CENTER);

        RoundedPanel flujo = crearCard();
        flujo.setLayout(new BorderLayout(0, 16));
        flujo.add(headerSeccion("Flujo del centro",
                "Procesa el siguiente paquete y cargalo al camion en un solo paso."), BorderLayout.NORTH);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        botones.setOpaque(false);
        ModernButton agregar = new ModernButton("Agregar al centro", ModernButton.Variant.PRIMARY);
        agregar.addActionListener(e -> agregarPaqueteManual());
        ModernButton procesar = new ModernButton("Procesar y cargar", ModernButton.Variant.SECONDARY);
        procesar.addActionListener(e -> procesarYCargar());
        botones.add(agregar);
        botones.add(procesar);

        flujo.add(botones, BorderLayout.CENTER);
        flujo.add(hintLabel("Secuencia sugerida: alta manual, verificar pendientes, procesar y cargar."),
                BorderLayout.SOUTH);

        contenedor.add(formulario);
        contenedor.add(Box.createVerticalStrut(18));
        contenedor.add(flujo);
        contenedor.add(Box.createVerticalGlue());
        return contenedor;
    }

    private JComponent construirFormularioPaquete() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 0, 8, 12);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"ID", "Peso", "Destino", "Contenido"};
        ModernTextField[] fields = {idField, pesoField, destinoField, contenidoField};
        for (int i = 0; i < labels.length; i++) {
            gc.gridy = i;
            gc.gridx = 0;
            gc.weightx = 0;
            panel.add(labelCampo(labels[i]), gc);
            gc.gridx = 1;
            gc.weightx = 1;
            panel.add(fields[i], gc);
        }
        gc.gridy = labels.length;
        gc.gridx = 0;
        gc.weightx = 0;
        panel.add(labelCampo("Prioridad"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panel.add(urgenteCheck, gc);
        return panel;
    }

    private JComponent crearPanelCamion() {
        JPanel contenedor = verticalContainer();

        RoundedPanel card = crearCard();
        card.setLayout(new BorderLayout(0, 18));
        card.add(headerSeccion("Operacion LIFO del camion",
                "El ultimo paquete en entrar es el primero en salir o deshacerse."), BorderLayout.NORTH);

        JPanel botones = new JPanel(new GridLayout(1, 3, 12, 0));
        botones.setOpaque(false);

        ModernButton ver = new ModernButton("Ver carga actual", ModernButton.Variant.PRIMARY);
        ver.addActionListener(e -> verCargaCamion());

        ModernButton deshacer = new ModernButton("Deshacer ultima carga", ModernButton.Variant.SECONDARY);
        deshacer.addActionListener(e -> ejecutarAccion("Ultima carga removida", service::deshacerUltimaCargaCamion));

        ModernButton descargar = new ModernButton("Descargar tope", ModernButton.Variant.SECONDARY);
        descargar.addActionListener(e -> ejecutarAccion("Paquete descargado", service::descargarCamion));

        botones.add(ver);
        botones.add(deshacer);
        botones.add(descargar);

        card.add(botones, BorderLayout.CENTER);
        card.add(hintLabel("Antes de descargar, revisa el orden en el panel de actividad."), BorderLayout.SOUTH);

        contenedor.add(card);
        contenedor.add(Box.createVerticalGlue());
        return contenedor;
    }

    private JComponent crearPanelDepositos() {
        JPanel contenedor = verticalContainer();

        RoundedPanel auditoria = crearCard();
        auditoria.setLayout(new BorderLayout(0, 16));
        auditoria.add(headerSeccion("Auditoria post-orden del ABB",
                "Marca como visitados los depositos sin auditoria en los ultimos 30 dias."), BorderLayout.NORTH);
        ModernButton auditarBtn = new ModernButton("Ejecutar auditoria", ModernButton.Variant.PRIMARY);
        auditarBtn.addActionListener(e -> auditarDepositos());
        JPanel leftAlign = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftAlign.setOpaque(false);
        leftAlign.add(auditarBtn);
        auditoria.add(leftAlign, BorderLayout.CENTER);

        RoundedPanel nivel = crearCard();
        nivel.setLayout(new BorderLayout(0, 16));
        nivel.add(headerSeccion("Consulta por nivel",
                "Lista los depositos en un nivel del arbol (raiz = nivel 0)."), BorderLayout.NORTH);

        JPanel nivelForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        nivelForm.setOpaque(false);
        nivelField.setPreferredSize(new Dimension(140, 40));
        ModernButton nivelBtn = new ModernButton("Consultar nivel", ModernButton.Variant.SECONDARY);
        nivelBtn.addActionListener(e -> consultarNivel());
        nivelForm.add(labelCampo("Nivel"));
        nivelForm.add(nivelField);
        nivelForm.add(nivelBtn);
        nivel.add(nivelForm, BorderLayout.CENTER);

        contenedor.add(auditoria);
        contenedor.add(Box.createVerticalStrut(18));
        contenedor.add(nivel);
        contenedor.add(Box.createVerticalGlue());
        return contenedor;
    }

    private JComponent crearPanelRutas() {
        JPanel contenedor = verticalContainer();

        RoundedPanel card = crearCard();
        card.setLayout(new BorderLayout(0, 18));
        card.add(headerSeccion("Distancia minima entre depositos",
                "Calcula la mejor ruta sobre el grafo usando Dijkstra."), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 0, 8, 12);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridy = 0;
        gc.gridx = 0;
        form.add(labelCampo("Deposito origen"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(origenField, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 0;
        form.add(labelCampo("Deposito destino"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(destinoRutaField, gc);

        ModernButton calcular = new ModernButton("Calcular distancia minima", ModernButton.Variant.PRIMARY);
        calcular.addActionListener(e -> calcularDistanciaMinima());
        JPanel botonWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botonWrap.setOpaque(false);
        botonWrap.add(calcular);

        card.add(form, BorderLayout.CENTER);
        card.add(botonWrap, BorderLayout.SOUTH);

        contenedor.add(card);
        contenedor.add(Box.createVerticalGlue());
        return contenedor;
    }

    private JComponent crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(360, 0));

        sidebar.add(crearResumen());
        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(crearAccionesRapidas());
        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(crearActividad());
        return sidebar;
    }

    private JComponent crearResumen() {
        RoundedPanel card = crearCard();
        card.setLayout(new BorderLayout(0, 18));
        card.add(headerSeccion("Resumen en vivo",
                "Contadores actualizados tras cada operacion."), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(metricCard("Pendientes", pendientesLabel, "Centro", Theme.PRIMARY));
        grid.add(metricCard("En camion", camionLabel, "LIFO", Theme.SUCCESS));
        grid.add(metricCard("Depositos", depositosLabel, "ABB", new Color(0x8B5CF6)));
        grid.add(metricCard("Rutas", rutasLabel, "Grafo", Theme.WARNING));

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        JLabel titleFooter = new JLabel("Ultima actividad");
        titleFooter.setFont(Theme.SMALL_BOLD);
        titleFooter.setForeground(Theme.TEXT_SUBTLE);
        footer.add(titleFooter);
        footer.add(Box.createVerticalStrut(4));
        footer.add(ultimaActividadLabel);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        card.add(grid, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JComponent crearAccionesRapidas() {
        RoundedPanel card = crearCard();
        card.setLayout(new BorderLayout(0, 14));
        card.add(headerSeccion("Acciones rapidas", "Atajos a las operaciones mas usadas."), BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 8));
        panel.setOpaque(false);

        ModernButton procesarBtn = new ModernButton("Procesar siguiente", ModernButton.Variant.PRIMARY);
        procesarBtn.addActionListener(e -> procesarYCargar());

        ModernButton verCargaBtn = new ModernButton("Ver carga camion", ModernButton.Variant.SECONDARY);
        verCargaBtn.addActionListener(e -> verCargaCamion());

        ModernButton auditarBtn = new ModernButton("Auditar depositos", ModernButton.Variant.SECONDARY);
        auditarBtn.addActionListener(e -> auditarDepositos());

        panel.add(procesarBtn);
        panel.add(verCargaBtn);
        panel.add(auditarBtn);

        card.add(panel, BorderLayout.CENTER);
        return card;
    }

    private JComponent crearActividad() {
        RoundedPanel card = crearCard();
        card.setLayout(new BorderLayout(0, 12));
        card.add(headerSeccion("Actividad reciente",
                "Historial cronologico de operaciones sobre el sistema."), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(logArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(logArea.getBackground());
        scroll.setPreferredSize(new Dimension(0, 260));
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel verticalContainer() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private RoundedPanel crearCard() {
        RoundedPanel panel = new RoundedPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        return panel;
    }

    private JComponent headerSeccion(String title, String description) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.H2);
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(Theme.BODY);
        descLabel.setForeground(Theme.TEXT_MUTED);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        panel.add(titleLabel);
        panel.add(descLabel);
        return panel;
    }

    private JLabel labelCampo(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.H3);
        label.setForeground(Theme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        return label;
    }

    private JLabel hintLabel(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.TEXT_SUBTLE);
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        return label;
    }

    private JLabel metricLabel() {
        JLabel label = new JLabel("0");
        label.setFont(Theme.METRIC);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private JComponent metricCard(String title, JLabel valueLabel, String chip, Color accent) {
        RoundedPanel card = new RoundedPanel(Theme.RADIUS_CARD, 0);
        card.setFillColor(Theme.SURFACE_ALT);
        card.setBorderColor(Theme.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.SMALL_BOLD);
        titleLabel.setForeground(Theme.TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLabel, BorderLayout.WEST);
        top.add(new ColorDot(accent, 8), BorderLayout.EAST);

        JLabel chipLabel = new JLabel(chip);
        chipLabel.setFont(Theme.SMALL_BOLD);
        chipLabel.setForeground(accent);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(chipLabel, BorderLayout.SOUTH);
        return card;
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
        appendLog("Carga actual del camion (tope primero):");
        for (Paquete<?> paquete : carga) {
            appendLog("  - " + paquete);
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
            List<Deposito> depositos = service.depositosPorNivel(nivel);
            if (depositos.isEmpty()) {
                appendLog("No hay depositos en el nivel " + nivel + ".");
                return;
            }
            appendLog("Depositos del nivel " + nivel + ":");
            for (Deposito deposito : depositos) {
                appendLog("  - " + deposito.getId() + " - " + deposito.getNombre());
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
            estadoPill.set("Centro con paquetes listos para procesar", Theme.PRIMARY, Theme.PRIMARY_SOFT);
        } else if (enCamion > 0) {
            estadoPill.set("Camion con carga lista para despacho", Theme.SUCCESS, Theme.SUCCESS_SOFT);
        } else if (inventarioCargado) {
            estadoPill.set("Inventario cargado. Sistema sin pendientes", Theme.SUCCESS, Theme.SUCCESS_SOFT);
        } else {
            estadoPill.set("Esperando inventario o carga manual", Theme.TEXT_MUTED, Theme.SURFACE_ALT);
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

    private static class StatusPill extends JPanel {
        private final ColorDot dot = new ColorDot(Theme.TEXT_MUTED, 8);
        private final JLabel label = new JLabel();
        private Color fillBackground = Theme.SURFACE_ALT;

        StatusPill(String initial) {
            super(new FlowLayout(FlowLayout.LEFT, 8, 6));
            setOpaque(false);
            label.setFont(Theme.SMALL_BOLD);
            label.setForeground(Theme.TEXT_MUTED);
            label.setText(initial);
            add(dot);
            add(label);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 14));
        }

        void set(String text, Color fg, Color bg) {
            label.setText(text);
            label.setForeground(fg);
            dot.setColor(fg);
            this.fillBackground = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillBackground);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }

    private static class ColorDot extends JComponent {
        private Color color;
        private final int size;

        ColorDot(Color color, int size) {
            this.color = color;
            this.size = size;
            setPreferredSize(new Dimension(size + 2, size + 2));
        }

        void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.fill(new Ellipse2D.Float(x, y, size, size));
            g2.dispose();
        }
    }

    private static class LogoDot extends JComponent {
        private final int size;

        LogoDot(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size + 2, size + 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMARY);
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.fillRoundRect(x, y, size, size, size, size);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x + 3, y + 3, size - 6, size - 6, size - 6, size - 6);
            g2.dispose();
        }
    }

    private static class SlimScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = Theme.BORDER_STRONG;
            this.trackColor = Theme.SURFACE_ALT;
        }

        @Override
        protected javax.swing.JButton createDecreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected javax.swing.JButton createIncreaseButton(int orientation) {
            return invisibleButton();
        }

        private javax.swing.JButton invisibleButton() {
            javax.swing.JButton b = new javax.swing.JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, javax.swing.JComponent c, java.awt.Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, javax.swing.JComponent c, java.awt.Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
}
