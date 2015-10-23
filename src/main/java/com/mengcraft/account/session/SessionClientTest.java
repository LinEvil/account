package com.mengcraft.account.session;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Created on 15-10-23.
 */
public class SessionClientTest {

    public void client(String host, int port, String user, String pass) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port));

            byte[] userBuffer = user.getBytes(StandardCharsets.UTF_8);
            byte[] passBuffer = pass.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream bufferWriter = new DataOutputStream(buffer);

            bufferWriter.write(0);
            bufferWriter.writeInt(userBuffer.length);
            bufferWriter.writeInt(passBuffer.length);
            bufferWriter.write(userBuffer);
            bufferWriter.write(passBuffer);

            OutputStream out = socket.getOutputStream();
            DataOutputStream outWriter = new DataOutputStream(out);
            outWriter.writeInt(buffer.size());
            outWriter.write(buffer.toByteArray());

            outWriter.flush();

            InputStream in = socket.getInputStream();
            DataInputStream inReader = new DataInputStream(in);

            System.out.println("Packet length: " + inReader.readInt());
            System.out.println("Protocol: " + inReader.read());
            System.out.println("X: " + inReader.readInt());
            System.out.println("Y: " + inReader.readInt());
            System.out.println("Z: " + inReader.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
