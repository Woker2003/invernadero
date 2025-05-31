import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ControlPanel extends JFrame {
    private JPanel buttonPanel = new JPanel();
    private JLabel lblTemp = new JLabel("Temperatura: -- °C");
    private JLabel lblHum = new JLabel("Humedad: -- %");
    private JButton btnCalef = new JButton("Calefacción: OFF");
    private JButton btnVent = new JButton("Ventilador: OFF");
    private JButton btnRiego = new JButton("Riego: OFF");
    private JButton btnHumVent = new JButton("Humedad Vent.: OFF");
    private JButton btnSalir = new JButton("Salir");
    private JButton btnGuardar = new JButton("Guardar Datos");
    private JToggleButton toggleControl = new JToggleButton("Modo Manual");

    private SerialManager serial;

    // Estados de los dispositivos
    private boolean calefON = false;
    private boolean ventON = false;
    private boolean riegoON = false;
    private boolean humventON = false;

    // Lista para almacenar las lecturas con timestamp
    private ArrayList<String> historialLecturas = new ArrayList<>();

    public ControlPanel(SerialManager serial) {
        this.serial = serial;

        setTitle("Panel de Control Arduino");
        setSize(800, 400); // Dimensiones de la ventana.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Cierra la aplicación al cerrar la ventana.
        setLayout(new BorderLayout()); // Usa un layout de tipo BorderLayout.

        // Etiquetas de temperatura y humedad
        JPanel topPanel = new JPanel();
        topPanel.add(lblTemp);
        topPanel.add(lblHum);
        add(topPanel, BorderLayout.CENTER);

        // Panel de botones
        buttonPanel.add(toggleControl);
        buttonPanel.add(btnCalef);
        buttonPanel.add(btnVent);
        buttonPanel.add(btnRiego);
        buttonPanel.add(btnHumVent);
        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnSalir);
        add(buttonPanel, BorderLayout.SOUTH);

        // Acciones de botones manuales
        btnCalef.addActionListener(e -> toggleDispositivo("CALEF", btnCalef, calefON = !calefON));
        btnVent.addActionListener(e -> toggleDispositivo("VENT", btnVent, ventON = !ventON));
        btnRiego.addActionListener(e -> toggleDispositivo("RIEGO", btnRiego, riegoON = !riegoON));
        btnHumVent.addActionListener(e -> toggleDispositivo("HUMVENT", btnHumVent, humventON = !humventON));

        btnSalir.addActionListener(e -> System.exit(0));

        // Botón para guardar los datos recopilados
        btnGuardar.addActionListener(e -> guardarHistorialEnArchivo());

        // Cambiar entre automático y manual
        toggleControl.addActionListener(e -> {
            boolean manual = toggleControl.isSelected();
            toggleControl.setText(manual ? "Modo Manual" : "Modo Automático");
            serial.setAutomatico(!manual);
            setModoManual(manual);
        });

        // Recibir datos del Arduino
        serial.recibirDatos(data -> {
            if (data.startsWith("T:")) {
                String[] partes = data.split(",");
                float temp = Float.parseFloat(partes[0].split(":")[1]);
                float hum = Float.parseFloat(partes[1].split(":")[1]);

                SwingUtilities.invokeLater(() -> {
                    lblTemp.setText("Temperatura: " + temp + " °C");
                    lblHum.setText("Humedad: " + hum + " %");
                });

                // Guardar lectura con timestamp
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String linea = timestamp + " | Temperatura: " + temp + " °C | Humedad: " + hum + " %";
                historialLecturas.add(linea);

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
                        actualizarTextoBoton(btnHumVent, "Humedad Vent.", humventON);
                    });
                }
            }
        });

        setModoManual(false); // Arranca en modo automático
    }

    // Activa o desactiva botones según el modo
    private void setModoManual(boolean manual) {
        btnCalef.setEnabled(manual);
        btnVent.setEnabled(manual);
        btnRiego.setEnabled(manual);
        btnHumVent.setEnabled(manual);
    }

    // Enviar comando y actualizar texto del botón
    private void toggleDispositivo(String nombre, JButton boton, boolean estado) {
        serial.enviar(nombre + (estado ? "_ON" : "_OFF"));
        actualizarTextoBoton(boton, obtenerEtiqueta(nombre), estado);
    }

    // Cambia el texto del botón según estado
    private void actualizarTextoBoton(JButton boton, String etiqueta, boolean estado) {
        boton.setText(etiqueta + ": " + (estado ? "ON" : "OFF"));
    }

    // Asocia nombre interno con texto legible
    private String obtenerEtiqueta(String nombre) {
        return switch (nombre) {
            case "CALEF" -> "Calefacción";
            case "VENT" -> "Ventilador";
            case "RIEGO" -> "Riego";
            case "HUMVENT" -> "Humedad Vent.";
            default -> nombre;
        };
    }

    // Guarda todas las lecturas en un archivo, agrupadas en un "chunk"
    private void guardarHistorialEnArchivo() {
        if (historialLecturas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos para guardar.");
            return;
        }

        String nombreArchivo = "registro_datos.txt";
        try (FileWriter writer = new FileWriter(nombreArchivo, true)) {
            writer.write("==== CHUNK: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " ====\n");

            for (String linea : historialLecturas) {
                writer.write(linea + "\n");
            }

            writer.write("==== FIN CHUNK ====\n\n");
            JOptionPane.showMessageDialog(this, "Datos guardados en " + nombreArchivo);

            historialLecturas.clear(); // Limpiar para el próximo chunk

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar archivo: " + e.getMessage());
        }
    }
}
