package com.mengcraft.account;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.account.entity.User;
import com.mengcraft.account.entity.lib.MetricsLite;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;

public class Main extends JavaPlugin {
	
	@Override
	public void onEnable() {
		getConfig().options().copyDefaults();
		saveConfig();
		
		EbeanHandler handler = EbeanManager.DEFAULT.getHandler(this);
		if (!handler.isInitialized()) {
			handler.define(User.class);
			try {
				handler.initialize();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		new Executor().bind(this, handler, Account.DEFAULT.getUserMap());
		new MetricsLite(this).start();
		
		String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
	}
	
}
