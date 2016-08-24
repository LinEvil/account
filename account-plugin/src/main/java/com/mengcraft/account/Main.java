package com.mengcraft.account;

import com.mengcraft.account.bungee.BungeeMain;
import com.mengcraft.account.bungee.BungeeSupport;
import com.mengcraft.account.command.BindingCommand;
import com.mengcraft.account.entity.AppAccountBinding;
import com.mengcraft.account.entity.AppAccountEvent;
import com.mengcraft.account.entity.Member;
import com.mengcraft.account.util.Messenger;
import com.mengcraft.account.util.MetricsLite;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private boolean log;
    private boolean notifyMail;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EbeanHandler db = EbeanManager.DEFAULT.getHandler(this);
        if (!db.isInitialized()) {
            db.define(AppAccountBinding.class);
            db.define(AppAccountEvent.class);
            db.define(Member.class);
            try {
                db.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        db.install(true);
        db.reflect();

        Account.INSTANCE.setMain(this);
        log = getConfig().getBoolean("log");
        notifyMail = getConfig().getBoolean("notify.mail");

        if (!getConfig().getBoolean("minimal")) {
            new Executor(this, new Messenger(this)).bind();
            new ExecutorEvent().bind(this);

            if (getConfig().getBoolean("binding.command")) {
                getCommand("binding").setExecutor(new BindingCommand(this));
            }

            getServer().getMessenger().registerIncomingPluginChannel(this, BungeeMain.CHANNEL, BungeeSupport.INSTANCE);
            getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeMain.CHANNEL);
        }

        new MetricsLite(this).start();

        String[] j = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(j);
    }

    public void execute(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public void process(Runnable task, int tick) {
        getServer().getScheduler().runTaskLater(this, task, tick);
    }

    public void process(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    public boolean notifyMail() {
        return notifyMail;
    }

    public boolean isLog() {
        return log;
    }

}
