package ejercicio.servidor;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainServidor {
    private static final int PORT = 1235; // Puerto para la comunicación del servidor
    private static final List<String> historialMensajes = new ArrayList<>(); // Almacena el historial de mensajes del chat
    private static final Set<SocketAddress> listaUsuarios = new HashSet<>(); // Lista de direcciones de clientes conectados
    private static final Set<String> nicknames = new HashSet<>(); // Lista de nicknames de usuarios conectados
    private static DatagramSocket datagramSocket; // Socket para comunicación UDP
    private static JTextArea jTextArea; // Área de texto para mostrar mensajes en el JFrame

    public static void main(String[] args) {
        //Creo la conexión del servidor:
        try {
            datagramSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        //Creo un JFrame para poder ver los mensajes que recibe el servidor de una forma más estética:
        JFrame frame = new JFrame("ejercicio.Cliente.Servidor del Chat");
        frame.setSize(400, 500);

        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        frame.add(new JScrollPane(jTextArea));
        //No se puede modificar:
        frame.setVisible(true);

        //Inicio el servidor:
        iniciarServidor();
    }

    /**
     * Iniciar el servidor para poder empezar a utilizar el chat, recibe las peticiones y mensajes de todos los clientes
     */
    private static void iniciarServidor() {
        jTextArea.append("ejercicio.Cliente.Servidor en línea en el puerto " + PORT + "\n");

        //Creo un hilo para poder atender varias solicitudes al mismo tiempo:
        Thread servidorThread = new Thread(() -> {
            while (true) {
                try {
                    //Recibir petición del cliente:
                    byte[] bufferEntrada = new byte[1024];
                    DatagramPacket paqueteEntrada = new DatagramPacket(bufferEntrada, bufferEntrada.length);
                    datagramSocket.receive(paqueteEntrada);

                    String mensajeRecibido = new String(paqueteEntrada.getData(), 0, paqueteEntrada.getLength());
                    //Añadir el mensaje al área del servidor:
                    jTextArea.append("Mensaje recibido: " + mensajeRecibido + "\n");

                    //Diferenciar entre un inicio de sesión y un mensaje:
                    //Sí empieza por [USUARIO] entonces comprobaremos si ya existe el usuario o no:
                    if (mensajeRecibido.startsWith("[USUARIO]")) {
                        //Divido el mensaje para obtener el nombre del usuario:
                        String usuario = mensajeRecibido.substring(9);
                        //Si ya existe el usuario se manda un error:
                        if (nicknames.contains(usuario)) {
                            enviarMensaje("[ERROR] El usuario '" + usuario + "' ya está en uso.", paqueteEntrada.getSocketAddress());
                        }
                        //En caso contrario se añade a la lista de nicknames y a la lista de clientes del servidor:
                        else {
                            nicknames.add(usuario);
                            listaUsuarios.add(paqueteEntrada.getSocketAddress());

                            //Enviamos el historial completo de mensajes al nuevo usuario del chat:
                            for (String msg : historialMensajes) {
                                enviarMensaje(msg, paqueteEntrada.getSocketAddress());
                            }
                            //Le damos también la bienvenida después de enviarle el historial para saber a partir de donde se ha unido:
                            enviarMensaje("[INFO] Bienvenido, " + usuario, paqueteEntrada.getSocketAddress());
                        }
                    }
                    //En caso contrario, será un mensaje por lo que lo añadiremos al historial y lo enviaremos a todos los participantes del chat:
                    else {
                        //añadir el mensaje al historial y enviárselo a todos los clientes:
                        historialMensajes.add(mensajeRecibido);
                        for (SocketAddress cliente : listaUsuarios) {
                            enviarMensaje(mensajeRecibido, cliente);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        servidorThread.start();
    }

    /**
     * Enviar un mensaje a un cliente del chat
     * @param mensaje
     * Contenido del mensaje
     * @param destinatario
     * Socket del cliente
     */
    private static void enviarMensaje(String mensaje, SocketAddress destinatario) {
        try {
            byte[] bufferSalida = mensaje.getBytes();
            DatagramPacket paqueteSalida = new DatagramPacket(bufferSalida, bufferSalida.length, destinatario);
            datagramSocket.send(paqueteSalida);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
