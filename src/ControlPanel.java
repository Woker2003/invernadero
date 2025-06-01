import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ControlPanel extends JFrame {
    private JLabel lblTemp = new JLabel("Temperatura: -- °C");
    private JLabel lblHum = new JLabel("Humedad: -- %");

    private JButton btnCalef = new JButton("Calefacción: OFF");
    private JButton btnVent = new JButton("Ventilador: OFF");
    private JButton btnRiego = new JButton("Riego: OFF");
    private JButton btnHumVent = new JButton("Humedad: OFF");
    private JButton btnGuardar = new JButton("Guardar Lecturas");
    private JButton btnMostrar = new JButton("Mostrar Lecturas");
    private JButton btnSalir = new JButton("Salir");

    private JButton btnAgregar = new JButton("Agregar Lectura");
    private JButton btnEditar = new JButton("Editar Lectura");
    private JButton btnEliminar = new JButton("Eliminar Lectura");

    private JToggleButton toggleControl = new JToggleButton("Modo Automático");

    private JTable tablaLecturas = new JTable();
    private DefaultTableModel modeloTabla;

    private SerialManager serial;

    private java.util.List<String[]> historial = new ArrayList<>();

    private boolean calefON = false, ventON = false, riegoON = false, humventON = false;

    public ControlPanel(SerialManager serial) {
        this.serial = serial;
        setTitle("Panel de Control Arduino");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior con información en tiempo real
        JPanel topPanel = new JPanel();
        topPanel.add(lblTemp);
        topPanel.add(lblHum);
        add(topPanel, BorderLayout.NORTH);

        // Panel inferior con controles generales
        JPanel controlPanel = new JPanel();
        for (JButton btn : new JButton[]{btnAgregar, btnEditar, btnEliminar, btnGuardar, btnMostrar, btnSalir}) {
            controlPanel.add(btn);
        }
        add(controlPanel, BorderLayout.SOUTH);

        // Panel CRUD separado para crear, editar y eliminar
        JPanel crudPanel = new JPanel();
        crudPanel.setLayout(new BoxLayout(crudPanel, BoxLayout.Y_AXIS));

        // Tamaño unificado para todos los botones
        Dimension buttonSize = new Dimension(150, 30); // ancho y alto

        // Aplicar márgenes y tamaño
        for (AbstractButton btn : new AbstractButton[]{toggleControl,btnCalef,btnVent,btnRiego,btnHumVent}) {
            btn.setMaximumSize(buttonSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            crudPanel.add(Box.createVerticalStrut(10)); // espacio entre botones
            crudPanel.add(btn);
        }
        crudPanel.add(Box.createVerticalGlue()); // empuja botones hacia arriba
        add(crudPanel, BorderLayout.WEST);

        // Tabla para mostrar lecturas
        modeloTabla = new DefaultTableModel(new Object[]{"Hora", "Temperatura", "Humedad"}, 0);
        tablaLecturas.setModel(modeloTabla);
        add(new JScrollPane(tablaLecturas), BorderLayout.CENTER);

        // Cambio de modo manual/automático
        toggleControl.addActionListener(e -> {
            boolean manual = toggleControl.isSelected();
            toggleControl.setText(manual ? "Modo Manual" : "Modo Automático");
            serial.setAutomatico(!manual);
            setModoManual(manual);
        });

        // Acciones de botones
        btnCalef.addActionListener(e -> toggleDispositivo("CALEF", btnCalef, calefON = !calefON));
        btnVent.addActionListener(e -> toggleDispositivo("VENT", btnVent, ventON = !ventON));
        btnRiego.addActionListener(e -> toggleDispositivo("RIEGO", btnRiego, riegoON = !riegoON));
        btnHumVent.addActionListener(e -> toggleDispositivo("HUMVENT", btnHumVent, humventON = !humventON));

        // Botones de almacenamiento y lectura
        btnGuardar.addActionListener(e -> guardarLecturas());
        btnMostrar.addActionListener(e -> mostrarLecturas());
        btnSalir.addActionListener(e -> System.exit(0));

        // CRUD manual
        btnAgregar.addActionListener(e -> agregarLecturaManual());
        btnEditar.addActionListener(e -> editarLectura());
        btnEliminar.addActionListener(e -> eliminarLectura());

        // Lectura de datos del Arduino en tiempo real
        serial.recibirDatos(data -> {
            if (data.startsWith("T:")) {
                String[] partes = data.split(",");
                float temp = Float.parseFloat(partes[0].split(":" )[1]);
                float hum = Float.parseFloat(partes[1].split(":" )[1]);
                String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

                SwingUtilities.invokeLater(() -> {
                    lblTemp.setText("Temperatura: " + temp + " °C");
                    lblHum.setText("Humedad: " + hum + " %");
                    modeloTabla.addRow(new Object[]{hora, temp, hum});
                    historial.add(new String[]{hora, String.valueOf(temp), String.valueOf(hum)});
                });

                // En modo automático, activar dispositivos según condiciones
                if (serial.esAutomatico()) {
                    calefON = temp < 20;
                    ventON = temp > 35;
                    riegoON = hum < 30;
                    humventON = hum > 70;

                    serial.enviar(calefON ? "CALEF_ON" : "CALEF_OFF");
                    serial.enviar(ventON ? "VENT_ON" : "VENT_OFF");
                    serial.enviar(riegoON ? "RIEGO_ON" : "RIEGO_OFF");
                    serial.enviar(humventON ? "HUMVENT_ON" : "HUMVENT_OFF");

                    SwingUtilities.invokeLater(() -> {
                        actualizarTextoBoton(btnCalef, "Calefacción", calefON);
                        actualizarTextoBoton(btnVent, "Ventilador", ventON);
                        actualizarTextoBoton(btnRiego, "Riego", riegoON);
                        actualizarTextoBoton(btnHumVent, "Extraccion Humedad", humventON);
                        mostrarAlerta(temp, hum);
                    });
                }
            }
        });

        // Desactiva botones manuales si está en automático
        setModoManual(false);
    }

    // Activa o desactiva botones dependiendo del modo manual
    private void setModoManual(boolean manual) {
        btnCalef.setEnabled(manual);
        btnVent.setEnabled(manual);
        btnRiego.setEnabled(manual);
        btnHumVent.setEnabled(manual);
    }

    // Envío de comando a Arduino manualmente y actualización del texto del botón
    private void toggleDispositivo(String nombre, JButton boton, boolean estado) {
        serial.enviar(nombre + (estado ? "_ON" : "_OFF"));//envio de comando
        actualizarTextoBoton(boton, obtenerEtiqueta(nombre), estado);
    }

    //actualizar texto de boton
    private void actualizarTextoBoton(JButton boton, String etiqueta, boolean estado) {
        boton.setText(etiqueta + ": " + (estado ? "ON" : "OFF"));
    }

    private String obtenerEtiqueta(String nombre) {
        return switch (nombre) {
            case "CALEF" -> "Calefacción";
            case "VENT" -> "Ventilador";
            case "RIEGO" -> "Riego";
            case "HUMVENT" -> "Humedad";
            default -> nombre;
        };
    }

    // Guarda todo el historial actual en un archivo de texto
    private void guardarLecturas() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("lecturas.txt"))) {
            for (String[] fila : historial) {
                writer.println("Hora: " + fila[0] + ", Temperatura: " + fila[1] + ", Humedad: " + fila[2]);
            }
            JOptionPane.showMessageDialog(this, "Lecturas guardadas correctamente.", "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lee desde el archivo lecturas.txt y las carga a la tabla y la lista historial
    private void mostrarLecturas() {
        modeloTabla.setRowCount(0);
        historial.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("lecturas.txt"))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                // Esperado: "Hora: hh:mm:ss, Temperatura: xx, Humedad: yy"
                try {
                    String[] partes = linea.split(", ");
                    String hora = partes[0].split(": ")[1];
                    String temp = partes[1].split(": ")[1];
                    String hum = partes[2].split(": ")[1];

                    String[] datos = new String[]{hora, temp, hum};
                    modeloTabla.addRow(datos);
                    historial.add(datos);
                } catch (Exception e) {
                    System.out.println("Línea con formato inválido: " + linea);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Permite al usuario agregar una lectura manual
    private void agregarLecturaManual() {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String temp = JOptionPane.showInputDialog("Temperatura:");
        String hum = JOptionPane.showInputDialog("Humedad:");
        if (temp != null && hum != null) {
            modeloTabla.addRow(new Object[]{hora, temp, hum});
            historial.add(new String[]{hora, temp, hum});
        }
    }

    // Permite editar una fila seleccionada en la tabla
    private void editarLectura() {
        int fila = tablaLecturas.getSelectedRow();
        if (fila >= 0) {
            String hora = (String) modeloTabla.getValueAt(fila, 0);
            String temp = JOptionPane.showInputDialog("Temperatura:", modeloTabla.getValueAt(fila, 1));
            String hum = JOptionPane.showInputDialog("Humedad:", modeloTabla.getValueAt(fila, 2));
            if (temp != null && hum != null) {
                modeloTabla.setValueAt(temp, fila, 1);
                modeloTabla.setValueAt(hum, fila, 2);
                historial.set(fila, new String[]{hora, temp, hum});
            }
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para editar.");
        }
    }

    // Permite eliminar una fila seleccionada de la tabla
    private void eliminarLectura() {
        int fila = tablaLecturas.getSelectedRow();
        if (fila >= 0) {
            modeloTabla.removeRow(fila);
            historial.remove(fila);
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar.");
        }
    }

    // Muestra una alerta en ventana emergente si hay dispositivos activos en automático
    private void mostrarAlerta(float temp, float hum) {
        StringBuilder alerta = new StringBuilder();
        if (calefON) alerta.append("Calefacción activa (LED Amarillo)\n");
        if (ventON) alerta.append("Ventilador por exceso de calor (Ventilador)\n");
        if (riegoON) alerta.append("Riego activo (LED Rojo)\n");
        if (humventON) alerta.append("Ventilación por exceso de humedad (LED Azul, Ventilador)\n");

        if (!alerta.isEmpty()) {
            JOptionPane.showMessageDialog(this, alerta.toString(), "Alerta", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}