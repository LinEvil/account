package com.mengcraft.account;

import com.mengcraft.account.entity.Event;
import com.mengcraft.account.session.SessionExecutor;
import com.mengcraft.account.session.SessionServer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.account.entity.User;
import com.mengcraft.account.lib.MetricsLite;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;

import java.io.IOException;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        EbeanHandler source = EbeanManager.DEFAULT.getHandler(this);
        if (!source.isInitialized()) {
            source.define(Event.class);
            source.define(User.class);
            try {
                source.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        source.install(true);

        new ExecutorCore().bind(this, source);
        if (getConfig().getBoolean("coreMode")) {
            getLogger().info("Entering core mode! Will not handle any auth method!");
        } else if (getConfig().getBoolean("sessionMode.enable")) {
            SessionExecutor executor = new SessionExecutor(this, source);
            getServer().getMessenger().registerIncomingPluginChannel(this, "Account", executor);
            getServer().getPluginManager().registerEvents(executor, this);
            try {
                new SessionServer(this, getConfig().getInt("sessionMode.listen")).start();
            } catch (IOException e) {
                getLogger().info("[SessionServer] Error on SessionServer! " + e.getMessage());
            }
        } else {
            new Executor().bind(this, source);
        }
        new MetricsLite(this).start();

        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
    }

    public void sync(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

}
