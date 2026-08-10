import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;

public class Peer {

    DatagramSocket socket;
    ServerSocket serverSocket;


    String user_name;
    boolean scanning;
    int user_port;
    int send_port;

    List<String> discovered = Collections.synchronizedList(new ArrayList<>());
    Queue<Socket> pending_connection = new LinkedList<>();

    Map<String, Socket> active_connection = new HashMap<>();


    String connection_request = "CONNECT_REQ by ";
    String connection_ack = "CONNECTED";
    String connection_reject = "REJECTED";


    public void lisner() throws IOException {
        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");
        String reply = user_name;

        byte[] buf;
        buf = new byte[256];

        DatagramPacket reciving_packet;
        reciving_packet = new DatagramPacket(buf, buf.length, group, user_port);


        while (true) {
            try {
                socket.receive(reciving_packet);
                DatagramPacket sending_packet;
                buf = reply.getBytes(StandardCharsets.UTF_8);
                sending_packet = new DatagramPacket(buf, buf.length, reciving_packet.getAddress(), reciving_packet.getPort());
                if (scanning) {
                    discovered.add("User_Name: " + new String(reciving_packet.getData(), 0, reciving_packet.getLength()) + "  User address:  " + reciving_packet.getAddress() + "  User port:  " + reciving_packet.getPort());
                } else {
                    socket.send(sending_packet);
                }
            } catch (SocketException e) {
                System.out.println("Closing the incoming_probes and outgoing_connection..... ");
                break;
            }
        }

    }


    public void scan() throws IOException {
        scanning = true;
        String probe = "ping";

        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");

        byte[] buf;
        buf = probe.getBytes(StandardCharsets.UTF_8);

        socket.setBroadcast(true);

        DatagramPacket sending_packet;
        sending_packet = new DatagramPacket(buf, buf.length, group, send_port);
        try {
            socket.send(sending_packet);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            System.out.println("Closing the scan ..... ");
        }

        scanning = false;
        System.out.println(discovered.toString());
        // should print the ls here
    }

    public void acceptLoop() throws IOException {
        // same like udp lisner which listen for the coming tcp connection request
        // should have a sperate thread as running full time
        // one some one type connect we need the port and user address and then
        // we need to start the socket where we can start a chat connection

        serverSocket = new ServerSocket(user_port);


        while (true) {
            // blocking feature later on
            Socket incoming = serverSocket.accept();

            InputStream inputStream = incoming.getInputStream();
            String message = "";
            byte[] buffer = new byte[1024]; // 1 KB buffer size
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) message = new String(buffer, 0, bytesRead);


            if (message.contains("CONNECT_REQ")) {
                pending_connection.add(incoming);
                System.out.println(message + " type /accept to connect or /reject to decline");
            }
        }


    }

    public void connect_to(String peer_name, String my_name, String peerIp, int peerport) throws IOException {

        Socket connection_socket = new Socket(peerIp, peerport);

        OutputStream Connection_out = connection_socket.getOutputStream();
        connection_request = connection_request + my_name;
        Connection_out.write(connection_request.getBytes(StandardCharsets.UTF_8));

        InputStream inputStream = connection_socket.getInputStream();
        String message = "";
        byte[] buffer = new byte[1024]; // 1 KB buffer size
        int bytesRead = inputStream.read(buffer);
        if (bytesRead != -1) message = new String(buffer, 0, bytesRead);


        if (message.equals("CONNECTED")) {
            System.out.println("User accepted the connection..." + message);
            active_connection.put(peer_name , connection_socket);

        } else if (message.equals("REJECTED")) {
            System.out.println("User declined the connection... " + message);
            connection_socket.close();
        }
    }


    public void reply_to_connection(String message , String peer_name) throws IOException {
        Socket current_socket = pending_connection.poll();
        assert current_socket != null;
        OutputStream outputStream = current_socket.getOutputStream();

        if (message.equals("ack")) {
            outputStream.write(connection_ack.getBytes(StandardCharsets.UTF_8));
            active_connection.put(peer_name , current_socket);
        } else if (message.equals("/reject")) {
            outputStream.write(connection_reject.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void list_of_connection(){
        // no freimds till now
        System.out.println(active_connection.keySet());
    }
}
