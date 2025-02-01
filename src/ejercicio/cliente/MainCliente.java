package ejercicio.cliente;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;

public class MainCliente {
    private static final int PORT = 1235; // Puerto para la comunicación UDP con el servidor
    private static DatagramSocket socketCliente; // Socket para enviar y recibir datos
    private static InetAddress servidorIP; // Dirección IP del servidor
    private static String nickname; // Nickname de usuario para el chat
    private static JTextArea chatArea; // Área de texto para mostrar los mensajes del chat
    private static JTextField mensajeField; // Área de texto para escribir mensajes

    public static void main(String[] args) {
        //Creo la conexión del cliente al servidor:
        try {
            socketCliente = new DatagramSocket();
            servidorIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException | SocketException e) {
            throw new RuntimeException(e);
        }

        //Creo el JFrame del chat para poder verlo de una forma más estética:
        JFrame frame = new JFrame("Chat");
        frame.setSize(400, 500);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea));

        //Agrego el área para escribir el mensaje:
        mensajeField = new JTextField();
        //Agrego el botón de enviar:
        JButton enviarButton = new JButton("Enviar");
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(mensajeField, BorderLayout.CENTER);
        inputPanel.add(enviarButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        //Enviar el mensaje clicando el botón de enviar
        enviarButton.addActionListener(e -> enviarMensaje());
        //Mandar mensaje al pulsar intro, sin necesidad de clicar en el botón enviar
        mensajeField.addActionListener(e -> enviarMensaje());

        frame.setVisible(true);
        solicitarUsuario();
        iniciarEscuchaMensajes();
    }

    /**
     * Comprobar si el usuario esta duplicado o no para darlo de alta en el sistema
     */
    private static void solicitarUsuario() {
        boolean usuarioAceptado = false;
        //Repetir el inicio de sesión hasta que sea correcto
        while (!usuarioAceptado) {
            //Mensaje emergente para introducir el nombre del usuario al iniciar el chat:
            nickname = JOptionPane.showInputDialog("Introduce tu usuario:");

            //Si el usuario está vacío empezar el bucle while de nuevo hasta introducir un valor válido:
            if (nickname == null || nickname.trim().isEmpty()){continue;}

            //Enviar al servidor una solicitud de inicio de sesión con el formato [USUARIO] + nickname para poder diferenciarlo de un mensaje y tratarlo de diferente manera:
            String solicitudUsuario = "[USUARIO]" + nickname;
            enviarDatos(solicitudUsuario);

            //Recibir la respuesta del servidor al iniciar sesión (usuario ya en uso o disponible):
            String respuestaServidor = recibirMensaje();
            if (respuestaServidor.startsWith("[ERROR]")) {
                //Mostrar por pantalla un error, ya que la respuesta del servidor empieza con [ERROR]
                JOptionPane.showMessageDialog(null, respuestaServidor, "ERROR", JOptionPane.ERROR_MESSAGE);
            } else {
                //Agregar la respuesta del servidor a tu chat en caso de poder acceder correctamente:
                chatArea.append(respuestaServidor + "\n");
                //Salir del bucle;
                usuarioAceptado = true;
            }
        }
    }

    /**
     * Empezar un hilo para poder recibir los mensajes de los usuarios continuamente:
     */
    private static void iniciarEscuchaMensajes() {
        //Inicio un hilo para poder empezar a escuchar todos los mensajes del chat:
        Thread escuchaMensajes = new Thread(() -> {
            while (true) {
                //Recibir los mensajes:
                String mensajeRecibido = recibirMensaje();
                if (!mensajeRecibido.isEmpty()) {
                    //Agregar el mensaje al chat para poder verlo:
                    chatArea.append(mensajeRecibido + "\n");
                }
            }
        });
        escuchaMensajes.start();
    }

    /**
     * Enviar mensajes del texto escrito en el área del mensaje
     */
    private static void enviarMensaje() {
        //Creo un String del mensaje que hay escrito en el área:
        String mensaje = mensajeField.getText().trim();
        if (!mensaje.isEmpty()) {
            //Envío el mensaje con el formato: usuario + ":" + (mensaje escrito)
            String mensajeAEnviar = nickname + ": " + mensaje;

            //Envío el mensaje al servidor:
            enviarDatos(mensajeAEnviar);
            //Limpiar el área:
            mensajeField.setText("");
        }
    }

    /**
     * Enviar los mensajes al servidor
     * @param mensaje
     * Texto a enviar
     */
    private static void enviarDatos(String mensaje) {
        //Enviar el mensaje indicado al servidor:
        try {
            byte[] buffer = mensaje.getBytes();
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, servidorIP, PORT);
            socketCliente.send(paquete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recibir las respuestas del servidor
     * @return
     * Devuelve la respuesta del servidor
     */
    private static String recibirMensaje() {
        //Recibir la respuesta del cliente como String:
        try {
            byte[] bufferRespuesta = new byte[1024];
            DatagramPacket respuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
            socketCliente.receive(respuesta);
            return new String(respuesta.getData(), 0, respuesta.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}