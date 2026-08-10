import jdk.swing.interop.SwingInterOpUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        Peer p = new Peer();
        p.user_name = args[0];
        p.user_port = Integer.parseInt(args[1]);
        p.send_port = Integer.parseInt(args[2]);
        p.socket = new DatagramSocket(p.user_port);


        Thread listenerThread = new Thread(() -> {
            try { p.lisner(); } catch (IOException e) { e.printStackTrace(); }
        });
        listenerThread.start();

        Thread chatThread = new Thread(() -> {
            try { p.acceptLoop(); } catch (IOException e) { e.printStackTrace(); }
        });
        chatThread.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            String cmd = sc.nextLine();
            if (cmd.equals("/scan")) {
                p.scan();
            } else if (cmd.equals("/exit")) {
                    p.socket.close();
                break;
            }else if(cmd.equals("/hello")){
                System.out.println("Hello");
            }else if(cmd.equals("/accept")){
                System.out.print("Peer name: ");
                String connection_name = sc.next();
                p.reply_to_connection("ack" , connection_name);
            } else if (cmd.equals("/reject")) {
                p.reply_to_connection("/reject" , "null");
            } else if (cmd.equals("/connect")){
                System.out.print("Peer name: ");
                String peername = sc.next();

                System.out.print("Peer Id: ");
                String peerIp = sc.next();

                System.out.print("Peer port: ");
                int peerport = sc.nextInt();

                p.connect_to(peername,p.user_name , peerIp,  peerport);
            } else if (cmd.equals("/chat")) {
                p.list_of_connection();
            }
        }
    }
}