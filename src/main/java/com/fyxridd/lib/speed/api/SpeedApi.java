package com.fyxridd.lib.speed.api;

import com.fyxridd.lib.speed.SpeedPlugin;
import org.bukkit.entity.Player;

public class SpeedApi {
    /**
     * @see #check(Player, String, String, int, boolean)
     * 速度过快时会提示玩家
     */
    public static boolean check(Player p, String plugin, String type, int limit) {
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
    public static boolean check(Player p, String plugin, String type, int limit, boolean tip) {
        return SpeedPlugin.instance.getSpeedManager().check(p, plugin, type, limit, tip);
    }

    /**
     * 检测短期间隔<br>
     * 会提示在侧边栏
     * @param p 玩家,不为null
     * @param plugin 插件,不为null
     * @param type 类型,不为null
     * @param level 等级,从1开始,配置文件中定义
     * @return true表示速度在允许范围内,false表示速度过快
     */
    public static boolean checkShort(Player p, String plugin, String type, int level) {
        return SpeedPlugin.instance.getSpeedManager().checkShort(p, plugin, type, level);
    }
}
