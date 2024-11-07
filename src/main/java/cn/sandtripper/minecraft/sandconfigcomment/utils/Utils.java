package cn.sandtripper.minecraft.sandconfigcomment.utils;

import org.bukkit.plugin.java.JavaPlugin;

public class Utils {
    static public void logWithColor(String s, JavaPlugin plugin) {
        plugin.getServer().getConsoleSender().sendMessage("[" + plugin.getDescription().getName() + "] " + s);
    }
}