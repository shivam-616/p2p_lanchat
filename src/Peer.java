import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Peer {
    DatagramSocket socket;
    String user_name;
    List<String> discovered = Collections.synchronizedList(new ArrayList<>());
    boolean scanning;
    int user_port;
    int send_port;

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

}
