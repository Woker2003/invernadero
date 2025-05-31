import com.fazecast.jSerialComm.SerialPort;

// Clase encargada de manejar la conexión serial con el Arduino
public class SerialManager {
    private SerialPort puerto; // Puerto serial al que está conectado Arduino
    private boolean automatico = true; // Bandera para saber si el sistema está en modo automático
    // Devuelve una lista de todos los puertos COM disponibles en el sistema
    public SerialPort[] listarPuertos() {
        return SerialPort.getCommPorts();
    }
    // recibe el nombre del puerto a conectar
    public boolean conectar(String nombrePuerto) {
        puerto = SerialPort.getCommPort(nombrePuerto);//selecciona ese puerto para conectarse
        puerto.setBaudRate(9600);//seleccionamos la velocidad de comunicacion
        return puerto.openPort(); // Abre el puerto y devuelve true si tuvo éxito
    }
    // Cierra el puerto si está abierto
    public void cerrarConexion() {
        if (puerto != null && puerto.isOpen()) {
            puerto.closePort();
        }
    }
    // Envía una cadena de texto al Arduino terminada en salto de línea
    public void enviar(String mensaje) {
        if (puerto != null && puerto.isOpen()) {
            puerto.writeBytes((mensaje + "\n").getBytes(), (mensaje + "\n").length());
        }
    }
    // Escucha continuamente los datos que llegan desde Arduino
    // Llama a la función onData cada vez que llega una línea nueva
    public void recibirDatos(SerialDataListener listener) {
        new Thread(() -> {//crea un hilo que escucha al arduino y a la vez muestra los datos del panel
            byte[] buffer = new byte[1024];//espacio temporal en memoria para guardar datos antes de procesarlos, guardamos bytes
            StringBuilder sb = new StringBuilder();//permite construir cadenas de texto letra por letra

            try {
                while (true) {
                    if (puerto.bytesAvailable() > 0) {
                        int leidos = puerto.readBytes(buffer, buffer.length);//Esto llena el buffer con datos y devuelve cuántos bytes se leyeron.
                        for (int i = 0; i < leidos; i++) {
                            char c = (char) buffer[i];
                            if (c == '\n') {
                                //El metodo .trim() elimina los espacios en blanco, saltos de línea o tabulaciones al inicio o final de un String
                                String linea = sb.toString().trim();
                                sb.setLength(0); // limpia el buffer
                                listener.onData(linea);// llama al listener
                            } else {
                                sb.append(c); // sigue construyendo la línea
                            }
                        }
                    }
                    Thread.sleep(100); // Espera 100ms para evitar sobrecarga
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    // Cambia el modo entre automático y manual
    public void setAutomatico(boolean valor) {
        automatico = valor;
    }
    // Devuelve si está en modo automático
    public boolean esAutomatico() {
        return automatico;
    }
    // Interfaz que se usa para recibir datos (callback)
    public interface SerialDataListener {
        void onData(String data);
    }
}