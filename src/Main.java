import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SerialManager serial = new SerialManager();

            SerialPort[] puertos = serial.listarPuertos();
            String[] nombres = new String[puertos.length];
            for (int i = 0; i < puertos.length; i++) {
                nombres[i] = puertos[i].getSystemPortName();
            }

            String seleccion = (String) JOptionPane.showInputDialog(
                    null,
                    "Selecciona el puerto:",
                    "Conexión Arduino",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    nombres,
                    nombres.length > 0 ? nombres[0] : null
            );

            if (seleccion != null && serial.conectar(seleccion)) {
                ControlPanel panel = new ControlPanel(serial);
                panel.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "No se pudo conectar al puerto.");
            }
        });
    }
}
