package com.xiaomian124.dragonegg;

import org.bukkit.plugin.java.JavaPlugin;

public class DragonEgg extends JavaPlugin {

    @Override
    public void onEnable() {
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new DragonEggListener(this), this);
        getLogger().info("插件已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("插件已禁用");
    }
}