import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Peer {

    DatagramSocket datagramSocket;
    ServerSocket scanning_Socket;

    String user_name;
    boolean scanning;

    // Standardized Ports for True LAN P2P
    public final int UDP_PORT = 8888;
    public final int TCP_PORT = 8889;

    // State Trackers to fix command prompt visual bugs
    public boolean inChatMode = false;
    public String currentChatPeer = "";

    List<String> discovered_Peer = Collections.synchronizedList(new ArrayList<>());

    // Thread-safe Queue prevents crashes when getting/resolving requests rapidly
    Queue<Socket> pending_connection = new java.util.concurrent.ConcurrentLinkedQueue<>();
    Map<String, Socket> active_connection = new HashMap<>();

    String connection_request = "CONNECT_REQ by ";
    String connection_ack = "CONNECTED";
    String connection_reject = "REJECTED";

    public void lisning_scan() throws IOException {
        byte[] buf = new byte[256];
        DatagramPacket reciving_packet = new DatagramPacket(buf, buf.length);

        while (true) {
            try {
                datagramSocket.receive(reciving_packet);

                // Read what the packet actually says
                String receivedData = new String(reciving_packet.getData(), 0, reciving_packet.getLength());

                if (receivedData.equals("ping")) {
                    // 1. Someone is scanning. Send our username back!
                    byte[] replyBuf = user_name.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket sending_packet = new DatagramPacket(replyBuf, replyBuf.length, reciving_packet.getAddress(), reciving_packet.getPort());
                    datagramSocket.send(sending_packet);

                } else {
                    // 2. It's not a ping. It must be a peer replying with their name!
                    if (scanning) {
                        String peerInfo = receivedData + "  |  " + reciving_packet.getAddress().getHostAddress() + "  |  " + reciving_packet.getPort();

                        // Prevent adding duplicates
                        if (!discovered_Peer.contains(peerInfo)) {
                            discovered_Peer.add(peerInfo);
                        }
                    }
                }
            } catch (SocketException e) {
                System.out.println(ConsoleColors.SYS + "\n[sys] Closing the incoming_probes and outgoing_connection..... " + ConsoleColors.RESET);
                break;
            }
        }
    }
    public void find_peer() throws IOException {
        discovered_Peer.clear(); // Reset list so it doesn't duplicate old scans
        scanning = true;
        String probe = "ping";

        InetAddress group = InetAddress.getByName("255.255.255.255");

        byte[] buf = probe.getBytes(StandardCharsets.UTF_8);

        datagramSocket.setBroadcast(true);

        DatagramPacket sending_packet = new DatagramPacket(buf, buf.length, group, UDP_PORT);
        try {
            datagramSocket.send(sending_packet);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            System.out.println(ConsoleColors.SYS + "[sys] Closing the scan ..... " + ConsoleColors.RESET);
        }

        scanning = false;
        System.out.println(ConsoleColors.SYS + "\n[sys] Peer List: type /connect to establish a connection" + ConsoleColors.RESET);
        for(String peer : discovered_Peer) {
            System.out.println(ConsoleColors.HIGHLIGHT + " - " + peer + ConsoleColors.RESET);
        }
    }

    public void acceptLoop() throws IOException {
        scanning_Socket = new ServerSocket(TCP_PORT);

        while (true) {
            Socket incoming = scanning_Socket.accept();
            String message = "";
            byte[] buffer = new byte[1024];

            InputStream inputStream = incoming.getInputStream();

            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) message = new String(buffer, 0, bytesRead);

            if (message.contains("CONNECT_REQ")) {
                pending_connection.add(incoming);
                System.out.print("\n" + ConsoleColors.SYS + "[sys] Incoming: " + message + ". Type /a to accept or /r to reject ....\n" + ConsoleColors.PROMPT + "> " + ConsoleColors.RESET);
            } else {
                incoming.close();
            }
        }
    }

    public void send_connection(String peer_name, String my_name, String peerIp) throws IOException {
        Socket connection_socket = new Socket(peerIp, TCP_PORT);

        try {
            OutputStream Connection_out = connection_socket.getOutputStream();
            String fullRequest = connection_request + my_name;
            Connection_out.write(fullRequest.getBytes(StandardCharsets.UTF_8));

            InputStream inputStream = connection_socket.getInputStream();
            String message = "";
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) message = new String(buffer, 0, bytesRead);

            if (message.equals("CONNECTED")) {
                System.out.println(ConsoleColors.SYS + "[sys] " + message + ". Type /chat to start a chat session" + ConsoleColors.RESET);
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
        } catch (SocketException e){
            System.out.println(ConsoleColors.ERROR + "Error with the peer: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    public void reply_to_connection(String message, String peer_name) throws IOException {
        Socket current_socket = pending_connection.poll();
        try {
            if (current_socket == null) {
                System.out.println(ConsoleColors.ERROR + "No pending connection request found." + ConsoleColors.RESET);
                return;
            }

            OutputStream outputStream = current_socket.getOutputStream();

            if (message.equals("ack")) {
                System.out.println(ConsoleColors.SYS + "[sys] Connection accepted! Type /list to see active connections." + ConsoleColors.RESET);
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
                System.out.println(ConsoleColors.SYS + "[sys] Connection rejected." + ConsoleColors.RESET);
                current_socket.close();
            }
        } catch (NullPointerException e ){
            System.out.println(ConsoleColors.ERROR + "No request exists for: " + peer_name + ConsoleColors.RESET);
        }
    }

    public void sending_chat(String sender_name, Scanner sc) throws IOException {
        if(!active_connection.containsKey(sender_name)) {
            System.out.println(ConsoleColors.ERROR + "No active connection with: " + sender_name + ConsoleColors.RESET);
            return;
        }

        Socket socket = active_connection.get(sender_name);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        inChatMode = true;
        currentChatPeer = sender_name;

        System.out.println("\n" + ConsoleColors.CHAT_BANNER + " ======================================== " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CHAT_BANNER + "   ENTERED SECURE CHAT WITH: " + sender_name.toUpperCase() + "      " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CHAT_BANNER + " ======================================== " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.SYS + "Type your messages below. Type /leave to exit.\n" + ConsoleColors.RESET);

        while (true) {
            System.out.print(ConsoleColors.CHAT_PROMPT + "[You -> " + sender_name + "]: " + ConsoleColors.RESET);
            String send_message = sc.nextLine();

            out.println(send_message.trim());

            if ("/leave".equalsIgnoreCase(send_message.trim())) {
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
                System.out.println(ConsoleColors.ERROR + "\n[sys] Peer " + peer_name + " disconnected." + ConsoleColors.RESET);
                active_connection.remove(peer_name);
                if(inChatMode && currentChatPeer.equals(peer_name)) {
                    inChatMode = false;
                    currentChatPeer = "";
                }
                break;
            }

            message = new String(buffer, 0, bytesRead);

            if ("/leave".equalsIgnoreCase(message.trim())) {
                System.out.println(ConsoleColors.ERROR + "\n[sys] Peer " + peer_name + " left the chat." + ConsoleColors.RESET);
                break;
            }

            System.out.println("\n" + ConsoleColors.PEER_MSG + ">> " + peer_name + " says: " + ConsoleColors.RESET + message.trim());

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