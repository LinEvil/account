package com.mengcraft.account;

import com.mengcraft.account.entity.Event;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.account.entity.User;
import com.mengcraft.account.lib.MetricsLite;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;

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
        if (!getConfig().getBoolean("coreMode")) {
            new Executor().bind(this, source);
        }
        new MetricsLite(this).start();

        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
    }

}
