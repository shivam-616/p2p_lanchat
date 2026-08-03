import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class client_side {

    public static void probing() throws IOException {
        String probe = "ping";

        List<String> ls = new ArrayList<>();
        List<String> synls = Collections.synchronizedList(ls);

        InetAddress group = null;
        group = InetAddress.getByName("255.255.255.255");

        byte[] buf;
        buf = probe.getBytes(StandardCharsets.UTF_8);
        DatagramSocket socket = null;
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        socket.setSoTimeout(5000);

        DatagramPacket sending_packet;
        sending_packet = new DatagramPacket(buf, buf.length, group, 4446);

        long durationInMillis = 5000; // Run for 5 seconds
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationInMillis;


        byte[] buf1;
        buf1 = new byte[256];
        DatagramPacket reciving_packet;
        reciving_packet = new DatagramPacket(buf1, buf1.length, group, 4446);

        socket.send(sending_packet);
        while (true) {
            try{
                socket.receive(reciving_packet);
                synls.add(new String(reciving_packet.getData(), 0, reciving_packet.getLength()) + ":" + reciving_packet.getAddress() + ":" + reciving_packet.getPort());
            }catch(SocketTimeoutException e){
                break;
            }
        }
        System.out.println(synls.toString());
        // should print the ls here
    }
}
