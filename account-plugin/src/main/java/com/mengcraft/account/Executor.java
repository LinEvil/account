package com.mengcraft.account;

import com.avaje.ebean.EbeanServer;
import com.mengcraft.account.entity.AppAccountEvent;
import com.mengcraft.account.entity.Member;
import com.mengcraft.account.event.UserLoggedInEvent;
import com.mengcraft.account.lib.It;
import com.mengcraft.account.lib.Messenger;
import com.mengcraft.account.lib.SecureUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

import static com.mengcraft.account.entity.AppAccountEvent.LOG_FAILURE;
import static com.mengcraft.account.entity.AppAccountEvent.LOG_SUCCESS;
import static com.mengcraft.account.entity.AppAccountEvent.of;

public class Executor implements Listener {

    private final Main main;
    private final Messenger messenger;
    private final EbeanServer db;

    public Executor(Main main, Messenger messenger) {
        this.messenger = messenger;
        this.main = main;
        this.db = main.getDatabase();
    }

    private String[] contents;
    private int castInterval;

    @EventHandler
    public void handle(PlayerCommandPreprocessEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            It<String> it = new It<>(event.getMessage().split(" "));
            String j = it.next();
            if (j.equals("/l") || j.equals("/login")) {
                processLogin(event.getPlayer(), it);
            } else if (j.equals("/r") || j.equals("/reg") || j.equals("/register")) {
                register(event.getPlayer(), it);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(AsyncPlayerPreLoginEvent event) {
        if (event.getName().length() > 15) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage(messenger.find("login.length", "用户名长度不能大于15位"));
        } else if (!event.getName().matches("[\\w]+")) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage(messenger.find("login.except", "用户名只能包含英文数字下划线"));
        }
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (BungeeSupport.INSTANCE.hasLoggedIn(p)) {
            LockedList.INSTANCE.remove(p.getUniqueId());
        } else {
            new BukkitRunnable() {
                public void run() {
                    if (p.isOnline() && isLocked(p.getUniqueId()))
                        p.sendMessage(contents);
                    else
                        cancel(); // Cancel if p exit or unlocked.
                }
            }.runTaskTimer(main, 20, castInterval);
            main.process(() -> {
                if (p.isOnline() && isLocked(p.getUniqueId())) {
                    event.getPlayer().kickPlayer(messenger.find("login.kick", ChatColor.DARK_RED + "未登录"));
                    if (main.isLog()) {
                        main.execute(() -> db.save(of(p, LOG_FAILURE)));
                    }
                }
            }, main.getConfig().getInt("kick", 600));
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Account.INSTANCE.drop(event.getPlayer().getName());
    }

    public void setCastInterval(int castInterval) {
        this.castInterval = castInterval;
    }

    public void bind() {
        setContents(main.getConfig().getStringList("broadcast.content"));
        getMain().getServer()
                .getPluginManager()
                .registerEvents(this, main);
        setCastInterval(main.getConfig().getInt("broadcast.interval"));
    }

    private void register(Player player, It<String> it) {
        if (Main.eq(it.size() - it.nextIndex(), 2)) {
            register(player, it.next(), it.next());
        } else {
            messenger.send(player, "register.format", ChatColor.DARK_RED + "输入/register <密码> <重复密码>以完成注册");
        }
    }

    private void register(Player p, String pass, String next) {
        main.execute(() -> {
            Member j = Account.INSTANCE.getMember(p);
            if (j.valid()) {
                messenger.send(p, "register.failure", ChatColor.DARK_RED + "注册失败");
            } else if (pass.length() < 6) {
                messenger.send(p, "register.password.short", ChatColor.DARK_RED + "注册失败，请使用6位长度以上的密码");
            } else if (!Main.eq(pass, next)) {
                messenger.send(p, "register.password.equal", ChatColor.DARK_RED + "注册失败，两次输入的密码内容不一致");
            } else {
                init(p, pass, j);
            }
        });
    }

    private void init(Player p, String pass, Member member) {
        SecureUtil util = SecureUtil.DEFAULT;
        String salt = util.random(3);

        try {
            member.setPassword(util.digest(util.digest(pass) + salt));
        } catch (Exception e) {
            throw new RuntimeException("init", e);
        }

        member.setSalt(salt);
        member.setUsername(p.getName());
        member.setRegip(p.getAddress().getAddress().getHostAddress());
        member.setRegdate(unixTime());
        member.setSecques("");
        member.setEmail("");
        member.setMyid("");
        member.setMyidkey("");

        db.save(member); //May throw exception.
        if (main.isLog()) {
            db.save(of(p, AppAccountEvent.REG_SUCCESS));
            db.save(of(p, LOG_SUCCESS));
        }

        BungeeSupport.INSTANCE.sendLoggedIn(main, p);
        LockedList.INSTANCE.remove(p.getUniqueId());

        messenger.send(p, "register.succeed", ChatColor.GREEN + "注册成功");
    }

    private void processLogin(Player p, It<String> it) {
        if (it.hasNext()) {
            main.execute(() -> {// IO blocking.
                Member j = Account.INSTANCE.getMember(p);
                if (j.valid() && j.valid(it.next())) {
                    BungeeSupport.INSTANCE.sendLoggedIn(main, p);
                    LockedList.INSTANCE.remove(p.getUniqueId());
                    messenger.send(p, "login.done", ChatColor.GREEN + "登录成功");
                    if (main.isLog()) {
                        main.execute(() -> db.save(of(p, LOG_SUCCESS)));
                    }
                    UserLoggedInEvent.post(p);
                } else {
                    messenger.send(p, "login.password", ChatColor.DARK_RED + "密码错误");
                }
            });
        }
    }

    private int unixTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private boolean isLocked(UUID uuid) {
        return LockedList.INSTANCE.isLocked(uuid);
    }

    private void setContents(List<String> list) {
        contents = list.toArray(new String[list.size()]);
    }

    private Main getMain() {
        return main;
    }

}
