package com.mengcraft.account;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;

import com.mengcraft.account.entity.User;
import com.mengcraft.account.entity.lib.ArrayVector;
import com.mengcraft.account.entity.lib.SecureUtil;
import com.mengcraft.account.entity.lib.StringUtil;
import com.mengcraft.simpleorm.EbeanHandler;

public class Executor implements Listener {

	private Map stateMap = new ConcurrentHashMap();
	private Map userMap = new ConcurrentHashMap();
	
	private Main main;
	private ExecutorService pool;
	private EbeanHandler source;
	
	private final Object object = new Object();
	
	@EventHandler
	public void handle(PlayerLoginEvent event) {
		getStateMap().put(event.getPlayer().getName(), object);
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
		getTask().runTaskLater(getMain(), () -> {
			if (a(userName)) {
				event.getPlayer().kickPlayer(ChatColor.DARK_RED + "未登录");
			}
		}, 600);
		event.getPlayer().sendMessage(ChatColor.GREEN + "请注册或登录/w\\");
	}
	
	@EventHandler
	public void handle(PlayerMoveEvent event) {
		if (a(event.getPlayer().getName())) {
			Location from = event.getFrom();
			from.setPitch(event.getTo().getPitch());
			from.setYaw(event.getTo().getYaw());
			
			event.setTo(from);
		}
	}
	
	@EventHandler
	public void handle(PlayerInteractEvent event) {
		if (a(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(PlayerDropItemEvent event) {
		if (a(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(PlayerPickupItemEvent event) {
		if (a(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(InventoryOpenEvent event) {
		if (a(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(EntityDamageEvent event) {
		if (a(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(FoodLevelChangeEvent event) {
		if (a(event.getEntity().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(PlayerQuitEvent event) {
		getUserMap().remove(event.getPlayer().getName());
	}
	
	@EventHandler
	public void handle(AsyncPlayerChatEvent event) {
		if (a(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void handle(PlayerCommandPreprocessEvent event) {
		if (a(event.getPlayer().getName())) {
			String[] d = StringUtil.DEF.split(event.getMessage());
			ArrayVector<String> vector = new ArrayVector<>(d);
			String c = vector.next();
			if (c.equals("/l") || c.equals("/login")) {
				login(event.getPlayer(), vector);
			}
			if (c.equals("/r") || c.equals("/reg") || c.equals("/register")) {
				register(event.getPlayer(), vector);
			}
			event.setCancelled(true);
		}
	}

	public void bind(Main main, EbeanHandler source) {
		if (getMain() != main) {
			setMain(main);
			getMain().getServer()
					 .getPluginManager()
				     .registerEvents(this, main);
			setPool(new ThreadPoolExecutor(1, 4,
					60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>())
			);
			setSource(source);
		}
	}

	public Main getMain() {
		return main;
	}

	public void setMain(Main main) {
		this.main = main;
	}

	public ExecutorService getPool() {
		return pool;
	}

	public void setPool(ExecutorService pool) {
		this.pool = pool;
	}

	public EbeanHandler getSource() {
		return source;
	}

	public void setSource(EbeanHandler source) {
		this.source = source;
	}

	public Map<String, Object> getStateMap() {
		return stateMap;
	}

	public Map<String, User> getUserMap() {
		return userMap;
	}

	private BukkitScheduler getTask() {
		return getMain().getServer().getScheduler();
	}

	private void register(Player a, ArrayVector<String> vector) {
		if (vector.remain() == 2) {
			String name = a.getName();
			User user = getUserMap().get(name);
			String secure = vector.next();
			if (user != null && !user.valid() && secure.equals(vector.next())) {
				a(user, name, secure, a.getAddress().getAddress());
				a.sendMessage(ChatColor.GREEN + "注册成功");
			} else {
				a.sendMessage(ChatColor.DARK_RED + "注册失败");
			}
		}
	}

	private void login(Player a, ArrayVector<String> vector) {
		if (vector.remain() != 0) {
			User user = getUserMap().get(a.getName());
			if (user != null && user.valid() && user.valid(vector.next())) {
				getStateMap().remove(a.getName());
				a.sendMessage(ChatColor.GREEN + "登陆成功");
			} else {
				a.sendMessage(ChatColor.DARK_RED + "密码错误");
			}
		}
	}

	private void a(User user, String name, String secure, InetAddress ip) {
		SecureUtil util = SecureUtil.DEFAULT;
		String salt = util.random(3);
		try {
			user.setPassword(util.digest(util.digest(secure) + salt));
		} catch (Exception e) {
			getMain().getLogger().warning(e.toString());
		}
		user.setSalt(salt);
		user.setUsername(name);
		user.setRegip(ip.getHostAddress());
		getPool().execute(() -> {
			getSource().save(user);
		});
		getStateMap().remove(name);
	}

	private boolean a(Entity entity) {
		return entity instanceof Player ? a(b(entity)) : false;
	}

	private String b(Entity entity) {
		return Player.class.cast(entity).getName();
	}

	private boolean a(String name) {
		return getStateMap().get(name) != null;
	}

	private User a(User user) {
		return user != null ? user : getSource().bean(User.class);
	}

}
