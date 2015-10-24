package com.mengcraft.account.session;

import com.mengcraft.account.Main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Created on 15-10-23.
 */
public class SessionServer {

    public static final AcceptHandler ACCEPT_HANDLER = new AcceptHandler();
    public static final SessionMap CACHED_MAP = new SessionMap();

    public final AsynchronousServerSocketChannel server;
    public final Main main;
    public final int port;

    public SessionServer(Main main, int port) throws IOException {
        this.server = AsynchronousServerSocketChannel.open();
        this.main = main;
        this.port = port > 1024 ? port : 23429;
    }

    public void start() throws IOException {
        main.getLogger().info("[SessionServer] Try to listen on " + port + '!');

        server.bind(new InetSocketAddress(port));
        server.accept(this, ACCEPT_HANDLER);
    }

    static class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, SessionServer> {

        @Override
        public void completed(AsynchronousSocketChannel client, SessionServer server) {
            new SessionClient(server.main, client).handle();
            server.server.accept(server, ACCEPT_HANDLER);
        }

        @Override
        public void failed(Throwable exc, SessionServer server) {
            server.main.getLogger().info("[SessionServer] Error handle new connection! " + exc.getMessage());
        }
    }

}
