package com.mengcraft.account;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.bukkit.entity.Player;

import com.mengcraft.account.entity.User;

public class Account {

	public static final Account DEFAULT = new Account();
	
	private final ExecutorService pool;
	private final Map<String, User> userMap;
	
	private Account() {
		this.userMap = new ConcurrentHashMap<>();
		this.pool = new ThreadPoolExecutor(1, 4, 60, SECONDS, 
				    new LinkedBlockingQueue<>());
	}

	public Map<String, User> getUserMap() {
		return userMap;
	}

	public ExecutorService getPool() {
		return pool;
	}

	public int getUserKey(String name) {
		return a(userMap.get(name));
	}

	public int getUserKey(Player player) {
		return a(userMap.get(player.getName()));
	}

	private int a(User user) {
		return user != null ? user.getUid() : 0;
	}

}
