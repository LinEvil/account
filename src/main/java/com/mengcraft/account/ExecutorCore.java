package com.mengcraft.account;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mengcraft.account.entity.User;
import com.mengcraft.simpleorm.EbeanHandler;

public class ExecutorCore implements Listener {
	
	private final Map userMap = Account.DEFAULT.getUserMap();
	private final ExecutorService pool = Account.DEFAULT.getPool();
	
	private EbeanHandler source;
	
	public void bind(Main main, EbeanHandler source) {
		if (getSource() != source) {
			setSource(source);
			main.getServer().getPluginManager().registerEvents(this, main);
		}
	}
	
	@EventHandler
	public void handle(PlayerJoinEvent event) {
		String userName = event.getPlayer().getName();
		getPool().execute(() -> {
			User user = getSource().find(User.class)
					.where()
					.eq("username", userName)
					.findUnique();
			getUserMap().put(userName, a(user));
		});
	}
	
	@EventHandler
	public void handle(PlayerQuitEvent event) {
		getUserMap().remove(event.getPlayer().getName());
	}
	
	private User a(User user) {
		return user != null ? user : getSource().bean(User.class);
	}

	public ExecutorService getPool() {
		return pool;
	}

	public Map getUserMap() {
		return userMap;
	}

	public EbeanHandler getSource() {
		return source;
	}

	public void setSource(EbeanHandler source) {
		this.source = source;
	}

}
