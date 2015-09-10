package com.mengcraft.account;

import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.account.entity.User;
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
		handler.install();
		handler.reflect();
		
		new Executor().bind(this, handler);
	}
	
}
