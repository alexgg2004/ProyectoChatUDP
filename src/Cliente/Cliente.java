package Cliente;

import Servidor.Servidor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class Cliente extends JFrame {

    private JTextArea textArea1;
    private JButton button1;
    private JTextField textField1;
    private JTextArea textArea2;
    private JPanel panel1;
    private String nombreUsuario;
    private MulticastSocket socket;
    private InetAddress grupo;
    public static List<String> nombresUsuarios = new ArrayList<>();
    private static List<String> mensajes = new ArrayList<>();

    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.crearInterfazInicio();
        cliente.start("225.0.0.1", 6001, cliente);
    }

    private void crearInterfazInicio() {
        boolean entradaValida = false;
        while (!entradaValida) {
            nombreUsuario = JOptionPane.showInputDialog("Introduce tu nombre de usuario:");

            if (nombreUsuario == null) {
                int respuesta = JOptionPane.showConfirmDialog(null, "¿Quieres salir del programa?", "Confirmar salida", JOptionPane.YES_NO_OPTION);
                if (respuesta == JOptionPane.YES_OPTION) {
                    System.exit(0);
                } else {
                    nombreUsuario = JOptionPane.showInputDialog("Introduce tu nombre de usuario:");
                }
            } else {
                if(nombreUsuario.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Debes ingresar un nombre de usuario. Inténtalo de nuevo.");
                    nombreUsuario = JOptionPane.showInputDialog("Introduce tu nombre de usuario:");
                } else {
                    entradaValida = true;
                }
            }
        }
    }

    private void start(String direccion, int puerto, Cliente cliente) {
        try {
            socket = new MulticastSocket(puerto);
            grupo = InetAddress.getByName(direccion);
            socket.joinGroup(grupo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Conectado con " + direccion + ": " + puerto);

        enviarMensaje("/nick" + nombreUsuario);

        crearInterfazChat(cliente);

        recibirMensajes(nombresUsuarios);
    }

    private void enviarMensaje(String mensaje) {
        try {
            byte[] buffer = mensaje.getBytes();
            DatagramPacket envio = new DatagramPacket(buffer, buffer.length, grupo, 6001);
            socket.send(envio);
        } catch (IOException e) {
            System.err.println("Error al enviar mensajes");
        }
    }

    private void crearInterfazChat(Cliente cliente) {
        cliente.setContentPane(cliente.panel1);
        cliente.setTitle("Chat TCP - (" + nombreUsuario + ")");
        cliente.setSize(875, 675);
        cliente.setVisible(true);
        cliente.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        button1 = new JButton("Enviar");

        textArea1.setEditable(false);
        textArea2.setEditable(false);
    }

    public Cliente() {
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarMensaje(nombreUsuario + ": " + textField1.getText());
                textField1.setText("");
            }
        });

        // En el caso de cerrar la aplicación enviar un mensaje de salida
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                nombresUsuarios.remove(nombreUsuario);
                enviarMensaje("/out" + nombreUsuario);
            }
        });
    }

    private void actualizarNombresUsuarios(List<String> nombreUsuarios) {
        textArea2.setText("");
        for(int i=0; i<nombreUsuarios.size(); i++) {
            textArea2.append(nombreUsuarios.get(i) + "\n");
        }
    }

    private void recibirMensajes(List<String> nombreUsuarios) {
        try {
            String mensaje = "";
            while(!mensaje.equalsIgnoreCase("/fail")) {
                byte[] buffer = new byte[1024];
                DatagramPacket recibe = new DatagramPacket(buffer, buffer.length);
                socket.receive(recibe);
                mensaje = new String(recibe.getData(), 0, recibe.getLength());
                if(mensaje.equalsIgnoreCase("/fail")) {
                    // Control de usuarios con el mismo nombre
                    JOptionPane.showMessageDialog(null, "El nombre de usuario ya está en uso");
                    socket.leaveGroup(grupo);
                    System.exit(0);
                } else {
                    if(mensaje.startsWith("/nick")) {
                        // Usuario aceptado
                        String nombre = mensaje.substring(5);
                        nombreUsuarios.add(nombre);
                    } else if(mensaje.startsWith("/usuarios")) {
                        // Lista de usuarios actualizada
                        String[] datos = mensaje.substring(9).split(" ");
                        nombreUsuarios.clear();
                        for(int i=0; i< datos.length; i++) {
                            if(!datos[i].isEmpty()) {
                                nombreUsuarios.add(datos[i]);
                            }
                        }
                        actualizarNombresUsuarios(nombreUsuarios);
                    } else if(mensaje.startsWith("/out")) {
                        // Cliente desconectado, eliminar de la lista
                        nombreUsuarios.remove(mensaje.substring(4));
                        actualizarNombresUsuarios(nombreUsuarios);
                    } else {
                        // Esto sirve para controlar que no se muestre el mismo mensaje a la vez dos veces
                        // Es un error que me ocurría al probar los clientes y usando esto se soluciona
                        String mensajeCliente = mensaje.substring(mensaje.indexOf(":") + 2);
                        if(!mensajes.contains(mensajeCliente)) {
                            textArea1.append(mensaje + "\n");
                            mensajes.add(mensajeCliente);

                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    mensajes.remove(mensajeCliente);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                    }
                }
            }
            socket.leaveGroup(grupo);
            socket.close();
        } catch(IOException e) {
            System.err.println("Errores al leer mensajes del servidor");
        }
    }
}
