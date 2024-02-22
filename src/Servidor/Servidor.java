package Servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Servidor {
    private MulticastSocket socket;
    private InetAddress grupo;
    private String nombre;
    public static List<String> nombresUsuarios;

    public Servidor() {
        nombresUsuarios = new ArrayList<>();
    }

    public static void main(String[] args) {
        new Servidor().start("225.0.0.1", 6001);
    }

    public void start(String direccion, int puerto) {
        try {
            socket = new MulticastSocket(puerto);
            grupo = InetAddress.getByName(direccion);
            socket.joinGroup(grupo);

            System.out.println("Servidor escuchando en el puerto " + puerto);

            String mensaje = "";
            while(true) {
                byte[] buffer = new byte[1024];
                DatagramPacket recibe = new DatagramPacket(buffer, buffer.length);
                socket.receive(recibe);
                mensaje = new String(recibe.getData(), 0, recibe.getLength());
                InetAddress direccionCliente = recibe.getAddress();
                int puertoCliente = recibe.getPort();

                if(mensaje.startsWith("/nick") && nombresUsuarios.contains(mensaje.substring(5))) {
                    // Comprobación de los clientes conectados
                    nombre = mensaje.substring(5);
                    System.out.println("El usuario " + nombre + " ya existe");
                    enviarMensaje("/fail", direccionCliente);
                } else {
                    if(!mensaje.startsWith("/nick") && !mensaje.startsWith("/out") && !mensaje.startsWith("/usuarios")) {
                        // Muestra solo mensajes
                        System.out.println("Mensaje recibido -> " + mensaje);
                    }
                    if(mensaje.startsWith("/nick")) {
                        // Conexión de un cliente
                        nombre = mensaje.substring(5);
                        System.out.println("El cliente " + nombre + " se ha conectado");
                        nombresUsuarios.add(nombre);
                        notificarConexionUsuarios();
                    } else if(mensaje.startsWith("/out")) {
                        // Desconexión de un cliente
                        nombre = mensaje.substring(4);
                        System.out.println("El cliente " + nombre + " se ha desconectado");
                        nombresUsuarios.remove(mensaje.substring(4));
                        notificarConexionUsuarios();
                    } else {
                        enviarMensaje(mensaje, direccionCliente);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer mensajes");
        }
    }

    private void enviarMensaje(String mensaje, InetAddress direccion) {
        byte[] buffer = mensaje.getBytes();
        DatagramPacket envio = new DatagramPacket(buffer, buffer.length, direccion, 6001);
        try {
            socket.send(envio);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void enviarMensajeGrupo(String mensaje) {
        byte[] buffer = mensaje.getBytes();
        DatagramPacket envio = new DatagramPacket(buffer, buffer.length, grupo, 6001);
        try {
            socket.send(envio);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void notificarConexionUsuarios() {
        // Método para actualizar la lista de usuarios en los clientes
        StringBuilder usuariosConectados = new StringBuilder();
        usuariosConectados.append("/usuarios");
        for(String nombreUsuario: nombresUsuarios) {
            usuariosConectados.append(" ").append(nombreUsuario);
        }
        enviarMensajeGrupo(usuariosConectados.toString());
    }
}
