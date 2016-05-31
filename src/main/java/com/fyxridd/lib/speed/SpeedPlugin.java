package com.fyxridd.lib.speed;

import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.plugin.SimplePlugin;
import com.fyxridd.lib.speed.config.SpeedConfig;
import com.fyxridd.lib.speed.manager.SpeedManager;

public class SpeedPlugin extends SimplePlugin{
    public static SpeedPlugin instance;
    public static boolean libMsgHook;

    private SpeedManager speedManager;

    @Override
    public void onEnable() {
        instance = this;
        try {
            Class.forName("com.fyxridd.lib.msg.MsgPlugin");
            libMsgHook = true;
        } catch (ClassNotFoundException e) {
        }

        //注册配置
        ConfigApi.register(pn, SpeedConfig.class);

        speedManager = new SpeedManager();

        super.onEnable();
    }

    public SpeedManager getSpeedManager() {
        return speedManager;
    }
}