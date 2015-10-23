package com.mengcraft.account.session;

import com.mengcraft.account.Account;
import com.mengcraft.account.Main;
import com.mengcraft.account.entity.Event;
import com.mengcraft.simpleorm.EbeanHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created on 15-10-23.
 */
public class SessionExecutor implements PluginMessageListener, Listener {

    private final SessionMap receivedMap = new SessionMap();
    private final Main main;
    private final EbeanHandler source;

    public SessionExecutor(Main main, EbeanHandler source) {
        this.main   = main;
        this.source = source;
    }

    @Override
    public void onPluginMessageReceived(String label, Player player, byte[] buffer) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer))) {
            if (input.read() == 2) {
                receivedMap.put(player.getName(), new Session(input.readInt(), input.readInt(), input.readInt()));
            }
        } catch (IOException e) {
            main.getLogger().info("Exception when received a payload message! " + e.getMessage());
        }
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        main.sync(() -> {
            Session cached = SessionServer.CACHED_MAP.get(name);
            Session received = receivedMap.get(name);
            if (cached != null && cached.equals(received)) {
                success(player);
            } else {
                failing(player);
            }
        });
    }

    private void success(Player player) {
        Event event = Event.of(player, Event.LOG_SUCCESS);
        Account.DEFAULT.getPool().execute(() -> {
            source.insert(event);
        });
    }

    private void failing(Player player) {
        Event event = Event.of(player, Event.LOG_FAILURE);
        Account.DEFAULT.getPool().execute(() -> {
            source.insert(event);
        });
        player.kickPlayer(ChatColor.RED + "Error while check session!");
    }

}
