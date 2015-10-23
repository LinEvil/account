package com.mengcraft.account;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.mengcraft.account.Account;
import com.mengcraft.account.Main;
import com.mengcraft.account.entity.Event;
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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.scheduler.BukkitScheduler;

import com.mengcraft.account.entity.User;
import com.mengcraft.account.lib.ArrayVector;
import com.mengcraft.account.lib.SecureUtil;
import com.mengcraft.account.lib.StringUtil;
import com.mengcraft.simpleorm.EbeanHandler;

public class Executor implements Listener, Runnable {

    private final Map stateMap = new ConcurrentHashMap();
    private final Object object = new Object();

    private final Map userMap = Account.DEFAULT.getUserMap();
    private final ExecutorService pool = Account.DEFAULT.getPool();

    private Main main;
    private EbeanHandler source;

    private String[] contents;

    public void bind(Main main, EbeanHandler source) {
        if (getMain() != main) {
            setContents(main.getConfig().getStringList("broadcast.content"));
            setMain(main);
            getMain().getServer()
                    .getPluginManager()
                    .registerEvents(this, main);
            getMain().getServer()
                    .getScheduler()
                    .runTaskTimer(main, this, 600, 20 * main
                            .getConfig().getInt("broadcast.interval"));
            setSource(source);
        }
    }

    @Override
    public void run() {
        for (Player p : getMain().getServer().getOnlinePlayers()) {
            if (a(p.getName())) p.sendMessage(contents);
        }
    }

    @EventHandler
    public void handle(AsyncPlayerPreLoginEvent event) {
        if (event.getName().length() > 15) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage("用户名长度不能大于15位");
        } else if (!event.getName().matches("[\\w]+")) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage("用户名只能包含英文数字下划线");
        }
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String userName = player.getName();
        getStateMap().put(userName, object);
        getTask().runTaskLater(getMain(), () -> {
            if (player.isOnline() && a(userName)) {
                event.getPlayer().kickPlayer(ChatColor.DARK_RED + "未登录");
                pool.execute(() -> source.save(Event.of(player, Event.LOG_FAILURE)));
            }
        }, 600);
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

    private Main getMain() {
        return main;
    }

    private void setMain(Main main) {
        this.main = main;
    }

    private ExecutorService getPool() {
        return pool;
    }

    private EbeanHandler getSource() {
        return source;
    }

    private void setSource(EbeanHandler source) {
        this.source = source;
    }

    private Map<String, Object> getStateMap() {
        return stateMap;
    }

    private Map<String, User> getUserMap() {
        return userMap;
    }

    private BukkitScheduler getTask() {
        return getMain().getServer().getScheduler();
    }

    private void register(Player player, ArrayVector<String> vector) {
        if (vector.remain() == 2) {
            String name = player.getName();
            User user = getUserMap().get(name);
            String secure = vector.next();
            if (user != null && !user.valid() && secure.equals(vector.next())) {
                a(user, name, secure, player.getAddress().getAddress());
                player.sendMessage(ChatColor.GREEN + "注册成功");
                pool.execute(() -> source.save(Event.of(player, Event.REG_SUCCESS)));
            } else {
                player.sendMessage(ChatColor.DARK_RED + "注册失败");
                pool.execute(() -> source.save(Event.of(player, Event.REG_FAILURE)));
            }
        }
    }

    private void login(Player player, ArrayVector<String> vector) {
        if (vector.remain() != 0) {
            User user = getUserMap().get(player.getName());
            if (user != null && user.valid() && user.valid(vector.next())) {
                getStateMap().remove(player.getName());
                player.sendMessage(ChatColor.GREEN + "登陆成功");
                pool.execute(() -> source.save(Event.of(player, Event.LOG_SUCCESS)));
            } else {
                player.sendMessage(ChatColor.DARK_RED + "密码错误");
                pool.execute(() -> source.save(Event.of(player, Event.LOG_FAILURE)));
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
        user.setRegdate(nowSec());
        getPool().execute(() -> getSource().save(user));
        getStateMap().remove(name);
    }

    private int nowSec() {
        return Long.class.cast(System.currentTimeMillis() / 1000).intValue();
    }

    private boolean a(Entity entity) {
        return entity instanceof Player && a(b(entity));
    }

    private String b(Entity entity) {
        return Player.class.cast(entity).getName();
    }

    private boolean a(String name) {
        return getStateMap().get(name) != null;
    }

    private void setContents(List<String> list) {
        contents = list.toArray(new String[list.size()]);
    }

}
