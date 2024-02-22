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
    private Map<String, InetAddress> usuarios;
    public static List<String> nombresUsuarios = new ArrayList<>();

    public Servidor() {
        usuarios = new HashMap<>();
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

                if(mensaje.startsWith("/nick") && usuarios.containsKey(mensaje.substring(5))) {
                    nombre = mensaje.substring(5);
                    System.out.println("El usuario " + nombre + " ya existe");
                    enviarMensaje("/fail", direccionCliente);
                } else {
                    System.out.println("Mensaje recibido -> " + mensaje);
                    if(mensaje.startsWith("/nick")) {
                        nombre = mensaje.substring(5);
                        usuarios.put(nombre, direccionCliente);
                        nombresUsuarios.add(nombre);
                        notificarConexionUsuarios();
                    } else if(mensaje.equals("/out")) {
                        usuarios.remove(mensaje.substring(4));
                        nombresUsuarios.remove(mensaje.substring(4));
                        notificarConexionUsuarios();
                    } else {
                        enviarMensaje(mensaje, direccionCliente);
                    }
                }
            }
        } catch (IOException e) {
            if (nombre != null) {
                System.err.println("El cliente " + nombre + " se ha desconectado");
                usuarios.remove(nombre);
                notificarConexionUsuarios();
            } else {
                System.err.println("Se ha producido un error al recibir mensajes");
                socket.close();
                e.printStackTrace();
            }
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
        StringBuilder usuariosConectados = new StringBuilder();
        usuariosConectados.append("/usuarios");
        for(String nombreUsuario: usuarios.keySet()) {
            usuariosConectados.append(" ").append(nombreUsuario);
            System.out.println(nombreUsuario);
        }
        enviarMensajeGrupo(usuariosConectados.toString());
        /* for(InetAddress direccion: usuarios.values()) {
            enviarMensaje(usuariosConectados.toString(), direccion);
            System.out.println(direccion);
        } */
    }
}
