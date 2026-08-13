import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;

public class Peer {

    DatagramSocket datagramSocket;
    ServerSocket scanning_Socket;


    String user_name;
    boolean scanning;
    int user_port;
    int send_port;

    List<String> discovered_Peer = Collections.synchronizedList(new ArrayList<>());
    Queue<Socket> pending_connection = new LinkedList<>();

    Map<String, Socket> active_connection = new HashMap<>();


    String connection_request = "CONNECT_REQ by ";
    String connection_ack = "CONNECTED";
    String connection_reject = "REJECTED";


    public void lisning_scan() throws IOException {
        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");
        String reply = user_name;

        byte[] buf;
        buf = new byte[256];

        DatagramPacket reciving_packet;
        reciving_packet = new DatagramPacket(buf, buf.length, group, user_port);


        while (true) {
            try {
                datagramSocket.receive(reciving_packet);
                DatagramPacket sending_packet;
                buf = reply.getBytes(StandardCharsets.UTF_8);
                sending_packet = new DatagramPacket(buf, buf.length, reciving_packet.getAddress(), reciving_packet.getPort());
                if (scanning) {
                    discovered_Peer.add("User_Name: " + new String(reciving_packet.getData(), 0, reciving_packet.getLength()) + "  User address:  " + reciving_packet.getAddress() + "  User port:  " + reciving_packet.getPort());
                } else {
                    datagramSocket.send(sending_packet);
                }
            } catch (SocketException e) {
                System.out.println("Closing the incoming_probes and outgoing_connection..... ");
                break;
            }
        }

    }


    public void find_peer() throws IOException {
        scanning = true;
        String probe = "ping";

        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");

        byte[] buf;
        buf = probe.getBytes(StandardCharsets.UTF_8);

        datagramSocket.setBroadcast(true);

        DatagramPacket sending_packet;
        sending_packet = new DatagramPacket(buf, buf.length, group, send_port);
        try {
            datagramSocket.send(sending_packet);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            System.out.println("Closing the scan ..... ");
        }

        scanning = false;
        System.out.println(discovered_Peer.toString());
        // should print the ls here
    }

    public void acceptLoop() throws IOException {
        // same like udp lisner which listen for the coming tcp connection request
        // should have a sperate thread as running full time
        // one some one type connect we need the port and user address and then
        // we need to start the socket where we can start a chat connection

        scanning_Socket = new ServerSocket(user_port);


        while (true) {
            // blocking feature later on
            Socket incoming = scanning_Socket.accept();

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

    public void send_connection(String peer_name, String my_name, String peerIp, int peerport) throws IOException {

        Socket connection_socket = new Socket(peerIp, peerport);

        OutputStream Connection_out = connection_socket.getOutputStream();
        connection_request = connection_request + my_name;
        Connection_out.write(connection_request.getBytes(StandardCharsets.UTF_8)); // CONNECT_REQ by {my_name}

        InputStream inputStream = connection_socket.getInputStream();
        String message = "";
        byte[] buffer = new byte[1024]; // 1 KB buffer size
        int bytesRead = inputStream.read(buffer);
        if (bytesRead != -1) message = new String(buffer, 0, bytesRead); // message from the peer_name


        if (message.equals("CONNECTED")) {
            System.out.println("User accepted the connection..." + message);
            active_connection.put(peer_name, connection_socket);
            // staert  a thread per Peer and  continusly listen to there message and print there message
            Thread lisning_chat = new Thread(() -> {
                try { lisning_chat(peer_name); } catch (IOException e) { e.printStackTrace(); }
            });
            lisning_chat.start();
        } else if (message.equals("REJECTED")) {
            System.out.println("User declined the connection... " + message);
            connection_socket.close();
        }
    }


    public void reply_to_connection(String message, String peer_name) throws IOException {
        Socket current_socket = pending_connection.poll();
        assert current_socket != null;
        OutputStream outputStream = current_socket.getOutputStream();

        if (message.equals("ack")) {
            outputStream.write(connection_ack.getBytes(StandardCharsets.UTF_8));
            active_connection.put(peer_name, current_socket);
            // start a thread for each per you have accepeted a connection where we have a loop that listen to all incoming message and we print that

            Thread lisning_chat = new Thread(() -> {
                try { lisning_chat(peer_name); } catch (IOException e) { e.printStackTrace(); }
            });
            lisning_chat.start();
        } else if (message.equals("/reject")) {
            outputStream.write(connection_reject.getBytes(StandardCharsets.UTF_8));
        }
    }


    // a function which send a chat meesage
    // param - peer_name whom to send
    // find the socket from the list send the message to them
    // start a differnt chat interface in the console where we have a different things


    public void sending_chat(String sender_name, Scanner sc) throws IOException {
        Socket socket = active_connection.get(sender_name);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String send_message = sc.nextLine();
            out.println(send_message);
            if ("/leave".equalsIgnoreCase(send_message)){
                System.out.println("Leaving Chat mode....");
                break;
            }
        }
    }



    // should always have a thread independent and redirect to /chat
    public void lisning_chat(String peer_name) throws IOException {

        Socket socket = active_connection.get(peer_name);
        InputStream inputStream = socket.getInputStream();

        String message = "";
        byte[] buffer = new byte[1024];
        while (true){
            int bytesRead = inputStream.read(buffer);
            message = new String(buffer, 0, bytesRead);

            if(bytesRead == -1 || message.equalsIgnoreCase("/leave") ){
                System.out.println("Peer disconnected");
                break;
            }
            System.out.println(peer_name + ": " + message);
        }
    }









    public void list_of_connection() {
        // no freimds till now
        System.out.println(active_connection.keySet());
    }
}
