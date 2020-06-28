package com.rendidor.mdns_testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Enumeration;


public class MainActivity extends AppCompatActivity {

    SatelinkFinder sf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void BuscarSatelink(View view) {
        sf = new SatelinkFinder();
        //try {sf.reestablecer();} catch (NullPointerException e){ sf = new SatelinkFinder(); }
            if (sf.running == false) {
                sf.start();
                Toast toast = Toast.makeText(getApplicationContext(), "Buscando servidor satelink ...", Toast.LENGTH_SHORT);
                toast.show();
            }
    }

    public class SatelinkFinder extends Thread {

        DatagramSocket socket; // para enviar y recivir informacion
        boolean running; // para indicar si el hilo esta o no corriendo
        byte[] buf = new byte[31];
        // String que se envia a satelink por medio de udp para indicarle que envie su direccion ip
        final byte[] SATELINK_IP_COMMAND = "satelink.ip".getBytes();
        InetAddress BROADCAST_IP; // direccion local de broadcast, depende de la subnet

        // para definir en que fase de la comunicacion se encuentra, como una maquina de estados
        // 0 > hacer broadcast. 1 > esperar ip del servidor. 2 > ip recivida fin del hilo
        public int estado = 0;


        public SatelinkFinder() {
            try {
                socket = new DatagramSocket(12100);
                // se determina la direccion ip de broadcast
                this.BROADCAST_IP = GetBroadcast();
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            }
        }

        // en este metodo se halla la direccion de broadcast ya que depende de la subnet
        public InetAddress GetBroadcast() throws SocketException, UnknownHostException {
            InetAddress broadcast = InetAddress.getByName("0.0.0.0");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback())
                    continue;    // Do not want to use the loopback interface.
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) continue;
                }
            }
            return broadcast;
        }

        public void run() {
            running = true;
            try {
                DatagramPacket data_send = new DatagramPacket( // satelink siempre escucha datagramas udp en el puerto 2100
                        this.SATELINK_IP_COMMAND, this.SATELINK_IP_COMMAND.length, this.BROADCAST_IP, 2100);
                socket.send(data_send);
                this.estado = 1; // se pasa a la fase de escucha
                while (estado == 1) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    Log.i("**********", received);
                    String[] res = received.split(":");
                    if (res[0].equals("satelink.ip.ans")) {// si al hacer split del string
                        // recivido el primer elemento es 'satelink.ip.ans' entonces el datagrama
                        // proviene de satelink
                        running = false;
                        this.estado = 2; // ya se ha encontrado la direccion ip del servidor
                        Log.i("+_+_+_+_+_+_+_", res[1]);
                        continue;
                    }
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void reestablecer(){
            this.estado=0;
            try {
                this.BROADCAST_IP = this.GetBroadcast();
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

}

