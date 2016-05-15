package com.fyxridd.lib.speed.manager;

import com.fyxridd.lib.config.api.ConfigApi;
import com.fyxridd.lib.config.manager.ConfigManager;
import com.fyxridd.lib.core.api.CoreApi;
import com.fyxridd.lib.core.api.UtilApi;
import com.fyxridd.lib.core.api.event.TimeEvent;
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
    private class SpeedHandler implements SideHandler {
        @Override
        public String get(Player p, String data) {
            Long waitTime = waitHash.get(p);
            if (waitTime != null) return getWaitShow(waitTime);
            return "";
        }
    }

    private static final String HANDLER_NAME = "speed";

    private SpeedConfig config;

    //缓存

    //可能为null
    private Object speedHandler;

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
        //添加配置监听
        ConfigApi.addListener(SpeedPlugin.instance.pn, SpeedConfig.class, new ConfigManager.Setter<SpeedConfig>() {
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
        }, SpeedPlugin.instance);
        //玩家退出
        Bukkit.getPluginManager().registerEvent(PlayerQuitEvent.class, SpeedPlugin.instance, EventPriority.HIGHEST, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event e) throws EventException {
                PlayerQuitEvent event = (PlayerQuitEvent) e;
                //删除短期
                shortHash.remove(event.getPlayer());
                startHash.remove(event.getPlayer());
                waitHash.remove(event.getPlayer());
            }
        }, SpeedPlugin.instance);

        if (CoreMain.libMsgHook) {
            speedHandler = new SpeedHandler();
            //注册获取器
            MsgApi.registerSideHandler(HANDLER_NAME, (SideHandler) speedHandler);
        }
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
     * @see #check(Player, String, String, int, boolean)
     */
	public boolean check(Player p, String plugin, String type, int limit) {
        return check(p, plugin, type, limit, true);
	}

    /**
     * 速度检测<br>
     * 会提示在聊天栏
     * @param p 玩家,不为null
     * @param plugin 插件,不为null
     * @param type 类型,不为null
     * @param limit 限制,单位毫秒,>=0
     * @param tip 速度过快时是否提示玩家(不是界面的强制显示)
     * @return true表示速度在允许范围内,false表示速度过快
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
            if (tip) {
                double wait = UtilApi.getDouble(((double) limit - (now - pre)) / 1000, 1);
                ShowApi.tip(p, get(1000, wait), true);
            }
            return false;
        }
        typeHash.put(name, now);
        return true;
    }

    /**
     * 检测短期间隔<br>
     * 会提示在侧边栏<br>
     * 注:短期间隔不用注册
     * @param p 玩家,不为null
     * @param plugin 插件,不为null
     * @param type 类型,不为null
     * @param level 等级,从1开始,配置文件中定义
     * @return true表示速度在允许范围内,false表示速度过快
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
            if (config.isSideEnable() && speedHandler != null) MsgApi.updateSideShow(p, HANDLER_NAME);
            else ShowApi.tip(p, getWaitShow(wait), false);
            return false;
        }
        //速度正常
        hash2.put(type, now);
        return true;

    }

    private String getWaitShow(long wait) {
        return get(1000, UtilApi.getDouble((double)wait/1000, 1)).getText();
    }
}
