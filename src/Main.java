import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println(ConsoleColors.ERROR + "Please provide a username! Example: java Main Shivam" + ConsoleColors.RESET);
            return;
        }

        Peer p = new Peer();
        p.user_name = args[0];
        p.datagramSocket = new DatagramSocket(p.UDP_PORT);

        Thread listenerThread = new Thread(() -> {
            try { p.lisning_scan(); } catch (IOException e) { e.printStackTrace(); }
        });
        listenerThread.start();

        Thread chatThread = new Thread(() -> {
            try { p.acceptLoop(); } catch (IOException e) { e.printStackTrace(); }
        });
        chatThread.start();

        // Print the beautiful ASCII dashboard right before user input starts
        printDashboard();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print(ConsoleColors.PROMPT + "> " + ConsoleColors.RESET);
            String cmd = sc.nextLine();

            if(cmd.equals("/chat")){
                System.out.print(ConsoleColors.SYS + "[sys] Starting chat mode.......\n" +
                        "[sys] Peer name: " + ConsoleColors.RESET);
                String peer_name = sc.nextLine();
                p.sending_chat(peer_name , sc);

            } else if (cmd.equals("/scan")) {
                p.find_peer();

            } else if (cmd.equals("/exit")) {
                p.datagramSocket.close();
                // Gracefully close active chat connections
                for (java.net.Socket socket : p.active_connection.values()) {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
                break;

            } else if(cmd.equals("/hello")){
                System.out.println(ConsoleColors.HIGHLIGHT + "Hello!" + ConsoleColors.RESET);

            } else if(cmd.equals("/a")){
                System.out.print(ConsoleColors.PROMPT + "Peer name: " + ConsoleColors.RESET);
                String connection_name = sc.next();
                sc.nextLine(); // Consume leftover newline
                p.reply_to_connection("ack" , connection_name);

            } else if (cmd.equals("/r")) {
                p.reply_to_connection("/reject" , "null");

            } else if (cmd.equals("/connect")){
                System.out.print(ConsoleColors.SYS + "[sys] Peer name: " + ConsoleColors.RESET);
                String peername = sc.next();

                System.out.print(ConsoleColors.SYS + "[sys] Peer IP: " + ConsoleColors.RESET);
                String peerIp = sc.next();

                sc.nextLine(); // Consume leftover newline

                p.send_connection(peername, p.user_name , peerIp);

            } else if (cmd.equals("/list")) {
                p.list_of_connection();

            } else {
                if(!cmd.trim().isEmpty()) {
                    System.out.println(ConsoleColors.ERROR + "Unknown command. Try looking at the dashboard above." + ConsoleColors.RESET);
                }
            }
        }
        System.out.println(ConsoleColors.ERROR + "Ending the session..." + ConsoleColors.RESET);
    }

    private static void printDashboard() {
        String asciiArt = ConsoleColors.PROMPT +
                "  ___ ___ ___    _      _   _  _     ___ _  _  _  _____ \n" +
                " | _ \\__ \\ _ \\  | |    /_\\ | \\| |   / __| || |/_\\|_   _|\n" +
                " |  _// //  _/  | |__ / _ \\| .` |  | (__| __ / _ \\ | |  \n" +
                " |_| /___|_|    |____/_/ \\_\\_|\\_|   \\___|_||_/_/ \\_\\_|  \n" +
                ConsoleColors.RESET;

        System.out.println(asciiArt);
        System.out.println(ConsoleColors.SYS + "========================================================" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HIGHLIGHT + "                  WELCOME TO P2P CHAT                   " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.SYS + "========================================================" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.PROMPT + " Available Commands:" + ConsoleColors.RESET);
        System.out.println("  " + ConsoleColors.PEER_MSG + "/scan" + ConsoleColors.RESET + "    - Scan the LAN for available peers");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/connect" + ConsoleColors.RESET + " - Send a connection request to a peer");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/a" + ConsoleColors.RESET + "       - Accept an incoming connection request");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/r" + ConsoleColors.RESET + "       - Reject an incoming connection request");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/list" + ConsoleColors.RESET + "    - View your active connections");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/chat" + ConsoleColors.RESET + "    - Enter chat mode with a connected peer");
        System.out.println("  " + ConsoleColors.PEER_MSG + "/leave" + ConsoleColors.RESET + "   - Exit the current active chat session");
        System.out.println("  " + ConsoleColors.ERROR + "/exit" + ConsoleColors.RESET + "    - Close the application");
        System.out.println(ConsoleColors.SYS + "========================================================" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HIGHLIGHT + "Type a command to get started..." + ConsoleColors.RESET);
    }
}