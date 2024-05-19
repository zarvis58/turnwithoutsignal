package com.safeai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class User2 {
    public static void main(String[] args) throws Throwable {
        try {
            StreamClient client = new StreamClient(18888, "text");
            client.initialize("user2");
            client.exchangeSdpWithPeer("user1");
            client.startConnect();
            final DatagramSocket socket = client.getDatagramSocket();
            final SocketAddress remoteAddress = client
                    .obtainRemoteSocketAddress();
            System.out.println(socket.toString());
            System.out.println("============== Please enter message ==============");
            new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        try {
                            byte[] buf = new byte[1024];
                            DatagramPacket packet = new DatagramPacket(buf,
                                    buf.length);
                            socket.receive(packet);
                            System.out.println();
                            System.out.println();
                            System.out.println("============== Received Message ==============");
                            System.out.println(packet.getAddress() + ":" + packet.getPort() + " says: "
                                    + new String(packet.getData(), 0, packet.getLength()));
                            System.out.println("============== Enter Message ==============");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            new Thread(new Runnable() {

                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                        String line;
                        System.out.println("============== Enter Message ==============");

                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.length() == 0) {
                                break;
                            }
                            byte[] buf = (line).getBytes();
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            packet.setSocketAddress(remoteAddress);
                            socket.send(packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}