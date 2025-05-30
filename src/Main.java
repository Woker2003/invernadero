import com.fazecast.jSerialComm.SerialPort;

public class Main {
    public static void main(String[] args) {
        SerialPort serialPort = SerialPort.getCommPort("COM4"); // Cambia "COM4" si es otro puerto
        serialPort.setBaudRate(9600);

        if (serialPort.openPort()) {
            System.out.println("Puerto serial abierto correctamente.");
        } else {
            System.out.println("No se pudo abrir el puerto serial.");
            return;
        }

        byte[] buffer = new byte[1024];
        StringBuilder dataBuffer = new StringBuilder();

        while (true) {
            if (serialPort.bytesAvailable() > 0) {
                int numRead = serialPort.readBytes(buffer, buffer.length);
                for (int i = 0; i < numRead; i++) {
                    char c = (char) buffer[i];
                    if (c == '\n') {
                        String line = dataBuffer.toString().trim();
                        if (!line.isEmpty()) {
                            System.out.println("Dato recibido: " + line);
                        }
                        dataBuffer.setLength(0);
                    } else {
                        dataBuffer.append(c);
                    }
                }
            }

            try {
                Thread.sleep(100); // Evita uso excesivo de CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        serialPort.closePort();
    }
}
