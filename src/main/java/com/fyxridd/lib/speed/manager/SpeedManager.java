package com.fyxridd.lib.speed.manager;

import com.fyxridd.lib.core.api.MessageApi;
import com.fyxridd.lib.core.api.UtilApi;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.event.TimeEvent;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.msg.api.MsgApi;
import com.fyxridd.lib.msg.api.SideGetter;
import com.fyxridd.lib.speed.SpeedPlugin;
import com.fyxridd.lib.speed.config.SpeedConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SpeedManager {
    private static final String HANDLER_NAME = "speed";

    private SpeedConfig config;

    //缓存

    //长期
	//插件 类型 玩家名 时间
	private Map<String, Map<String, Map<String, Long>>> speedHash = new HashMap<>();

    //短期
    //玩家 插件 类型
    private Map<Player, Map<String, Map<String, Long>>> shortHash = new HashMap<>();

    //玩家 开始时间点(与清除提示有关)
    private Map<Player, Long> startHash = new HashMap<>();
    //玩家 需要等待的时间(与清除提示无关)
    private Map<Player, Long> waitHash = new HashMap<>();

	public SpeedManager() {
        if (SpeedPlugin.libMsgHook) {
            //延时0秒进行(解决插件循环依赖的问题)
            Bukkit.getScheduler().scheduleSyncDelayedTask(SpeedPlugin.instance, new Runnable() {
                @Override
                public void run() {
                    //注册获取器
                    MsgApi.registerSideGetter(HANDLER_NAME, new SideGetter() {
                        @Override
                        public String get(Player p, String data) {
                            Long waitTime = waitHash.get(p);
                            if (waitTime != null) return getWaitShow(p.getName(), waitTime);
                            return "";
                        }
                    });
                }
            });
        }

        //添加配置监听
        ConfigApi.addListener(SpeedPlugin.instance.pn, SpeedConfig.class, new Setter<SpeedConfig>() {
            @Override
            public void set(SpeedConfig value) {
                config = value;
            }
        });

        //注册事件
        //短期清理
        Bukkit.getPluginManager().registerEvent(TimeEvent.class, SpeedPlugin.instance, EventPriority.LOW, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event e) throws EventException {
                if (e instanceof TimeEvent) {
                    if (TimeEvent.getTime() % config.getShortClearInterval() == 0) {
                        Long now = System.currentTimeMillis();
                        long limit = config.getShortClearInterval()*1000;
                        Iterator<Map.Entry<Player, Map<String, Map<String, Long>>>> it1 = shortHash.entrySet().iterator();
                        while (it1.hasNext()) {
                            Map.Entry<Player, Map<String, Map<String, Long>>> entry1 = it1.next();
                            Iterator<Map.Entry<String, Map<String, Long>>> it2 = entry1.getValue().entrySet().iterator();
                            while (it2.hasNext()) {
                                Map.Entry<String, Map<String, Long>> entry2 = it2.next();
                                Iterator<Map.Entry<String, Long>> it3 = entry2.getValue().entrySet().iterator();
                                while (it3.hasNext()) {
                                    Map.Entry<String, Long> entry3 = it3.next();
                                    if (now - entry3.getValue() > limit) {//超过清理限制
                                        it3.remove();
                                    }
                                }
                                if (entry2.getValue().isEmpty()) it2.remove();
                            }
                            if (entry1.getValue().isEmpty()) it1.remove();
                        }
                    }
                }
            }
        }, SpeedPlugin.instance);
        //玩家退出
        Bukkit.getPluginManager().registerEvent(PlayerQuitEvent.class, SpeedPlugin.instance, EventPriority.HIGHEST, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event e) throws EventException {
                if (e instanceof PlayerQuitEvent) {
                    PlayerQuitEvent event = (PlayerQuitEvent) e;
                    //删除短期
                    shortHash.remove(event.getPlayer());
                    startHash.remove(event.getPlayer());
                    waitHash.remove(event.getPlayer());
                }
            }
        }, SpeedPlugin.instance);

        //计时器
        //每1tick检测所有玩家,清除过期数据及侧边栏提示
        Bukkit.getScheduler().scheduleSyncRepeatingTask(SpeedPlugin.instance, new Runnable() {
            @Override
            public void run() {
                if (config.isSideEnable()) {
                    long endTime = System.currentTimeMillis()-config.getSideClearTimeout();
                    Iterator<Map.Entry<Player, Long>> it = startHash.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Player, Long> entry = it.next();
                        if (entry.getValue() < endTime) {
                            it.remove();
                            waitHash.remove(entry.getKey());
                            if (SpeedPlugin.libMsgHook) MsgApi.updateSideShow(entry.getKey(), HANDLER_NAME);
                        }
                    }
                }
            }
        }, 1, 1);
    }

    /**
     * @see com.fyxridd.lib.speed.api.SpeedApi#check(Player, String, String, int, boolean)
     */
    public boolean check(Player p, String plugin, String type, int limit, boolean tip) {
        String name = p.getName();
        long now = System.currentTimeMillis();
        Map<String, Map<String, Long>> pluginHash = speedHash.get(plugin);
        if (pluginHash == null) {
            pluginHash = new HashMap<>();
            speedHash.put(plugin, pluginHash);
        }
        Map<String, Long> typeHash = pluginHash.get(type);
        if (typeHash == null) {
            typeHash = new HashMap<>();
            pluginHash.put(type, typeHash);
        }
        Long pre = typeHash.get(name);
        if (pre != null && now-pre<limit) {
            if (tip) MessageApi.send(p, get(p.getName(), 10, UtilApi.getDouble(((double) limit - (now - pre)) / 1000, 1)), true);
            return false;
        }
        typeHash.put(name, now);
        return true;
    }

    /**
     * @see com.fyxridd.lib.speed.api.SpeedApi#checkShort(Player, String, String, int)
     */
    public boolean checkShort(Player p, String plugin, String type, int level) {
        //检测level
        if (level < 1 || level > config.getShortLevels().length) return false;
        //数据c
        Map<String, Map<String, Long>> hash = shortHash.get(p);
        if (hash == null) {
            hash = new HashMap<>();
            shortHash.put(p, hash);
        }
        Map<String, Long> hash2 = hash.get(plugin);
        if (hash2 == null) {
            hash2 = new HashMap<>();
            hash.put(plugin, hash2);
        }
        //检测
        long now = System.currentTimeMillis();
        Long pre = hash2.get(type);
        int limit = config.getShortLevels()[level-1];
        if (pre != null && now-pre<limit) {//速度过快
            long wait = limit - (now - pre);
            startHash.put(p, now);
            waitHash.put(p, wait);
            if (config.isSideEnable() && SpeedPlugin.libMsgHook) MsgApi.updateSideShow(p, HANDLER_NAME);
            else MessageApi.send(p, getWaitShow(p.getName(), wait), false);
            return false;
        }
        //速度正常
        hash2.put(type, now);
        return true;

    }

    /**
     * 获取等待时间的显示
     */
    private String getWaitShow(String player, long wait) {
        return get(player, 10, UtilApi.getDouble((double)wait/1000, 1)).getText();
    }

    private FancyMessage get(String player, int id, Object... args) {
        return config.getLang().get(player, id, args);
    }
}
