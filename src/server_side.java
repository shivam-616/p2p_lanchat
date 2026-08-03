import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// always listening , waiting for replies
public class server_side {
    DatagramSocket socket;
    String user_name;

    public void lisner() throws IOException {

        List<String> ls = new ArrayList<>();
        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");
        String reply = user_name;

        byte[] buf;
        buf = new byte[256];

        DatagramPacket reciving_packet;
        reciving_packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.receive(reciving_packet);

        DatagramPacket sending_packet;
        buf = reply.getBytes(StandardCharsets.UTF_8);
        sending_packet = new DatagramPacket(buf, buf.length,reciving_packet.getAddress(), reciving_packet.getPort());

        String receive = new String(reciving_packet.getData());
        if (receive.equalsIgnoreCase("ping")) {
            // wanting to know  are there in the lobby
            socket.send(sending_packet);
        } else {
            ls.add(reciving_packet.getData() + ":" + reciving_packet.getAddress() + ":" + reciving_packet.getPort());
        }
    }
}


