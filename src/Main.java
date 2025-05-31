import com.fazecast.jSerialComm.SerialPort;

public class Main {
    public static void main(String[] args) {
        SerialPort puerto = SerialPort.getCommPort("COM4");
        puerto.setBaudRate(9600);

        if (!puerto.openPort()) {
            System.out.println("No se pudo abrir el puerto.");
            return;
        }

        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();

        try {
            while (true) {
                if (puerto.bytesAvailable() > 0) {
                    int leidos = puerto.readBytes(buffer, buffer.length);
                    for (int i = 0; i < leidos; i++) {
                        char c = (char) buffer[i];
                        if (c == '\n') {
                            String linea = sb.toString().trim();
                            sb.setLength(0);

                            if (linea.startsWith("T:")) {
                                float temperatura = Float.parseFloat(linea.split(",")[0].split(":")[1]);
                                float humedad = Float.parseFloat(linea.split(",")[1].split(":")[1]);

                                System.out.println("Temperatura = " + temperatura + " °C | Humedad = " + humedad + " %");

                                // Lógica de control enviada al Arduino
                                if (temperatura < 20)
                                    puerto.writeBytes("CALEF_ON\n".getBytes(), "CALEF_ON\n".length());
                                else
                                    puerto.writeBytes("CALEF_OFF\n".getBytes(), "CALEF_OFF\n".length());

                                if (temperatura > 35)
                                    puerto.writeBytes("VENT_ON\n".getBytes(), "VENT_ON\n".length());
                                else
                                    puerto.writeBytes("VENT_OFF\n".getBytes(), "VENT_OFF\n".length());

                                if (humedad < 30)
                                    puerto.writeBytes("RIEGO_ON\n".getBytes(), "RIEGO_ON\n".length());
                                else
                                    puerto.writeBytes("RIEGO_OFF\n".getBytes(), "RIEGO_OFF\n".length());

                                if (humedad > 70)
                                    puerto.writeBytes("HUMVENT_ON\n".getBytes(), "HUMVENT_ON\n".length());
                                else
                                    puerto.writeBytes("HUMVENT_OFF\n".getBytes(), "HUMVENT_OFF\n".length());
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            puerto.closePort();
            System.out.println("Puerto cerrado.");
        }
    }
}

