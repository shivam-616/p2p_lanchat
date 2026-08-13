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
    Queue<Socket> pending_connection = new java.util.concurrent.ConcurrentLinkedQueue<>();

    Map<String, Socket> active_connection = new HashMap<>();

    String connection_request = "CONNECT_REQ by ";
    String connection_ack = "CONNECTED";
    String connection_reject = "REJECTED";

    // Add these near the top of Peer.java
    public boolean inChatMode = false;
    public String currentChatPeer = "";


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
                    discovered_Peer.add(new String(reciving_packet.getData(), 0, reciving_packet.getLength()) + "  |  " + reciving_packet.getAddress() + "  |  " + reciving_packet.getPort());
                } else {
                    datagramSocket.send(sending_packet);
                }
            } catch (SocketException e) {
                System.out.println(ConsoleColors.SYS + "[sys] Closing the incoming_probes and outgoing_connection..... " + ConsoleColors.RESET);
                break;
            }
        }
    }


    public void find_peer() throws IOException {
        discovered_Peer.clear();
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
            System.out.println(ConsoleColors.SYS + "[sys] Closing the scan ..... " + ConsoleColors.RESET);
        }

        scanning = false;
        System.out.println(ConsoleColors.SYS + "[sys] Peer List: /connect to connect " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HIGHLIGHT + discovered_Peer.toString() + ConsoleColors.RESET);
    }

    public void acceptLoop() throws IOException {
        scanning_Socket = new ServerSocket(user_port);

        while (true) {
            Socket incoming = scanning_Socket.accept();
            String message = "";
            byte[] buffer = new byte[1024];

            InputStream inputStream = incoming.getInputStream();

            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) message = new String(buffer, 0, bytesRead);

            if (message.contains("CONNECT_REQ")) {
                pending_connection.add(incoming);
                System.out.print(ConsoleColors.SYS + message + ": /a or /r ...." + ConsoleColors.RESET);
            } else {
                incoming.close();
            }
        }
    }

    public void send_connection(String peer_name, String my_name, String peerIp, int peerport) throws IOException {

        Socket connection_socket = new Socket(peerIp, peerport);

        try {
            OutputStream Connection_out = connection_socket.getOutputStream();
            connection_request = connection_request + my_name;
            Connection_out.write(connection_request.getBytes(StandardCharsets.UTF_8));

            InputStream inputStream = connection_socket.getInputStream();
            String message = "";
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) message = new String(buffer, 0, bytesRead);

            if (message.equals("CONNECTED")) {
                System.out.println(ConsoleColors.SYS + "[sys] " + message + ". /chat to start a chat session" + ConsoleColors.RESET);
                active_connection.put(peer_name, connection_socket);

                Thread lisning_chat = new Thread(() -> {
                    try {
                        lisning_chat(peer_name);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                lisning_chat.start();
            } else if (message.equals("REJECTED")) {
                System.out.println(ConsoleColors.ERROR + "[sys] " + message + ConsoleColors.RESET);
                connection_socket.close();
            }
        } catch (SocketException e) {
            System.out.println(ConsoleColors.ERROR + "Error with the peer: " + e.getMessage() + ConsoleColors.RESET);
        }
    }


    public void reply_to_connection(String message, String peer_name) throws IOException {
        Socket current_socket = pending_connection.poll();
        try {
            OutputStream outputStream = current_socket.getOutputStream();

            if (message.equals("ack")) {
                System.out.println(ConsoleColors.SYS + "[sys] type /list to see the connection" + ConsoleColors.RESET);
                outputStream.write(connection_ack.getBytes(StandardCharsets.UTF_8));
                active_connection.put(peer_name, current_socket);

                Thread lisning_chat = new Thread(() -> {
                    try {
                        lisning_chat(peer_name);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                lisning_chat.start();
            } else if (message.equals("/reject")) {
                outputStream.write(connection_reject.getBytes(StandardCharsets.UTF_8));
                current_socket.close();
            }
        } catch (NullPointerException e) {
            System.out.println(ConsoleColors.ERROR + "No request exists for: " + peer_name + ConsoleColors.RESET);
        }
    }


    public void sending_chat(String sender_name, Scanner sc) throws IOException {
        Socket socket = active_connection.get(sender_name);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);


        inChatMode = true;
        currentChatPeer = sender_name;

        // 1. Print a distinct banner when entering chat mode
        System.out.println("\n" + ConsoleColors.CHAT_BANNER + " ======================================== " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CHAT_BANNER + "   ENTERED SECURE CHAT WITH: " + sender_name.toUpperCase() + "      " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CHAT_BANNER + " ======================================== " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.SYS + "Type your messages below. Type /leave to exit.\n" + ConsoleColors.RESET);

        while (true) {
            // 2. Add a persistent custom prompt for the chat mode
            System.out.print(ConsoleColors.CHAT_PROMPT + "[You -> " + sender_name + "]: " + ConsoleColors.RESET);
            String send_message = sc.nextLine();

            out.println(send_message.trim());

            if ("/leave".equalsIgnoreCase(send_message.trim())) {
                // 3. Print an exit banner when leaving
                System.out.println("\n" + ConsoleColors.SYS + "Leaving Chat mode with " + sender_name + "...." + ConsoleColors.RESET);
                System.out.println(ConsoleColors.SYS + "Returning to main dashboard...\n" + ConsoleColors.RESET);
                break;
            }
        }
        inChatMode = false;
        currentChatPeer = "";
    }

    public void lisning_chat(String peer_name) throws IOException {
        Socket socket = active_connection.get(peer_name);
        InputStream inputStream = socket.getInputStream();

        String message = "";
        byte[] buffer = new byte[1024];
        while (true) {
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) {
                System.out.println(ConsoleColors.ERROR + "\nPeer disconnected" + ConsoleColors.RESET);
                break;
            }

            message = new String(buffer, 0, bytesRead);

            if ("/leave".equalsIgnoreCase(message.trim())) {
                System.out.println(ConsoleColors.ERROR + "\nPeer disconnected" + ConsoleColors.RESET);
                break;
            }

            System.out.println("\n" + ConsoleColors.PEER_MSG + ">> " + peer_name + " says: " + ConsoleColors.RESET + message);


            if (inChatMode && currentChatPeer.equals(peer_name)) {
                System.out.print(ConsoleColors.CHAT_PROMPT + "[You -> " + peer_name + "]: " + ConsoleColors.RESET);
            } else if (!inChatMode) {
                System.out.print(ConsoleColors.PROMPT + "> " + ConsoleColors.RESET);
            }
        }
    }

    public void list_of_connection() {
        System.out.println(ConsoleColors.PROMPT + "Active Connections: " + active_connection.keySet() + ConsoleColors.RESET);
    }
}


