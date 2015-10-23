package com.mengcraft.account.session;

import com.mengcraft.account.Main;
import com.mengcraft.account.entity.User;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * Created on 15-10-23.
 */
public class SessionClient {

    private static final ReadHandler READ_HANDLE = new ReadHandler();
    private static final WriteHandler WRITE_HANDLER = new WriteHandler();

    public final AsynchronousSocketChannel client;
    public final ByteBuffer buffer = ByteBuffer.allocate(0xFFFF);
    public final Main main;

    public SessionClient(Main main, AsynchronousSocketChannel client) {
        this.main = main;
        this.client = client;
    }

    public void handle() {
        client.read(buffer, 15, TimeUnit.SECONDS, this, READ_HANDLE);
    }

    static class WriteHandler implements CompletionHandler<Integer, SessionClient> {

        @Override
        public void completed(Integer length, SessionClient client) {
            if (length != 0 && client.buffer.remaining() != 0) {
                client.client.write(client.buffer, 15, TimeUnit.SECONDS, client, WRITE_HANDLER);
            } else {
                client.close();
            }
        }

        @Override
        public void failed(Throwable exc, SessionClient client) {
            client.main.getLogger().info("[SessionServer] Error when response! " + exc.getMessage());
            try {
                client.client.close();
            } catch (IOException e) {
                client.main.getLogger().info("[SessionServer] Error when close handle! " + e.getMessage());
            }
        }

    }

    static class ReadHandler implements CompletionHandler<Integer, SessionClient> {

        @Override
        public void completed(Integer length, SessionClient client) {
            if (length > 4) {
                ByteBuffer buffer = client.buffer;

                int cursor = buffer.position();

                buffer.flip();

                int needed = buffer.getInt();
                int remain = buffer.remaining();

                if (needed > 0xFFFF) {
                    client.close("Close due to error packet!");
                } else if (needed > remain) {
                    buffer.clear();
                    buffer.position(cursor);
                    // Re read due to half packet.
                    client.handle();
                } else if (response(client, handle(buffer))) {
                    client.main.getLogger().info("[SessionServer] Succeed valid a request!");
                } else {
                    client.close("Close due to error request!");
                }
            } else {
                client.close("Close due to too short request!");
            }
        }

        private boolean response(SessionClient client, String name) {
            if (name != null) {
                ByteBuffer buffer = client.buffer;
                Session session = new Session();

                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteBuffer);
                try {
                    out.write(1);
                    out.writeInt(session.getX());
                    out.writeInt(session.getY());
                    out.writeInt(session.getZ());
                } catch (IOException e) {
                    client.main.getLogger().info("[SessionServer] Error when response! " + e.getMessage());
                }

                SessionServer.CACHED_MAP.put(name, session);

                buffer.flip();

                buffer.putInt(byteBuffer.size());
                buffer.put(byteBuffer.toByteArray());

                buffer.flip();

                client.client.write(buffer, 15, TimeUnit.SECONDS, client, WRITE_HANDLER);

                return true;
            }
            return false;
        }

        private String handle(ByteBuffer buffer) {
            byte id = buffer.get();
            if (id == 0) {
                int nameSize = buffer.getInt();
                int passSize = buffer.getInt();

                byte[] stringBuffer = new byte[nameSize + passSize];

                buffer.get(stringBuffer, 0, nameSize);
                buffer.get(stringBuffer, nameSize, passSize);

                String name = new String(stringBuffer, 0, nameSize);
                String pass = new String(stringBuffer, nameSize, passSize);

                return check(name, pass) ? name : null;
            }
            return null;
        }

        private boolean check(String name, String pass) {
            EbeanHandler db = EbeanManager.DEFAULT.getHandler("Account");
            User user = db.find(User.class)
                    .where()
                    .eq("username", name)
                    .findUnique();
            return user != null && user.valid(pass);
        }

        @Override
        public void failed(Throwable exc, SessionClient client) {
            client.main.getLogger().info("[SessionServer] Error when read request! " + exc.toString());
            try {
                client.client.close();
            } catch (IOException e) {
                client.main.getLogger().info("[SessionServer] Error when close handle! " + e.getMessage());
            }
        }

    }

    private void close(String message) {
        main.getLogger().info("[SessionServer] " + message);
        try {
            client.close();
        } catch (IOException e) {
            main.getLogger().info("[SessionServer] Error when close handle! " + e.getMessage());
        }
    }

    private void close() {
        close("Try close a completed connection!");
    }

}
