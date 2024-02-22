package Hilos;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.List;

public class HiloCliente implements Runnable {
    private MulticastSocket socket;
    private JTextArea ta1;
    private JTextArea ta2;
    List<String> nombreUsuarios;

    public HiloCliente(MulticastSocket socket, JTextArea ta1, JTextArea ta2, List<String> nombreUsuarios) {
        this.socket = socket;
        this.ta1 = ta1;
        this.ta2 = ta2;
        this.nombreUsuarios = nombreUsuarios;
    }

    private void actualizarNombresUsuarios() {
        ta2.setText("");
        for(int i=0; i<nombreUsuarios.size(); i++) {
            ta2.append(nombreUsuarios.get(i) + "\n");
        }
    }

    @Override
    public void run() {
        try {
            String mensaje = "";
            while(!mensaje.equalsIgnoreCase("/fail")) {
                byte[] buffer = new byte[1024];
                DatagramPacket recibe = new DatagramPacket(buffer, buffer.length);
                socket.receive(recibe);
                mensaje = new String(recibe.getData(), 0, recibe.getLength());
                if(mensaje.equalsIgnoreCase("/fail")) {
                    JOptionPane.showMessageDialog(null, "El nombre de usuario ya está en uso");
                } else {
                    if(mensaje.startsWith("/nick")) {
                        String nombre = mensaje.substring(5);
                        nombreUsuarios.add(nombre);
                    } else if(mensaje.startsWith("/usuarios")) {
                        String[] datos = mensaje.substring(9).split(" ");
                        nombreUsuarios.clear();
                        for(int i=0; i< datos.length; i++) {
                            if(!datos[i].isEmpty()) {
                                nombreUsuarios.add(datos[i]);
                            }
                        }
                        actualizarNombresUsuarios();
                    } else {
                        ta1.append(mensaje + "\n");
                    }
                }
            }
            System.exit(0);
        } catch(IOException e) {
            System.err.println("Errores al leer mensajes del servidor");
        }
    }
}
