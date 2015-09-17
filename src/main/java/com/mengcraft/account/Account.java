package com.mengcraft.account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.mengcraft.account.entity.User;

public class Account {

	public static final Account DEFAULT = new Account();
	
	private final Map<String, User> userMap;
	
	private Account() {
		this.userMap = new ConcurrentHashMap<>();
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

	Map<String, User> getUserMap() {
		return userMap;
	}

}
