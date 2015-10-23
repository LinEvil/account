package com.mengcraft.account.session;

import com.mengcraft.account.session.Session;
import com.mengcraft.account.session.SessionMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created on 15-10-23.
 */
public class SessionExecutor implements PluginMessageListener, Listener {

    private final SessionMap receivedMap = new SessionMap();

    @Override
    public void onPluginMessageReceived(String label, Player player, byte[] buffer) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer))) {
            if (input.read() == 2) {
                receivedMap.put(player.getName(), new Session(input.readInt(), input.readInt(), input.readInt()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception when received a payload message!", e);
        }
    }

}
