package cn.sandtripper.minecraft.sandconfigcomment.utils;

import org.bukkit.plugin.java.JavaPlugin;

import static cn.sandtripper.minecraft.sandconfigcomment.utils.Utils.logWithColor;


public class StartPrint {
    private JavaPlugin plugin;

    public StartPrint(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void print(String picture) {
        for (String line : picture.split("\n")) {
            logWithColor("§d  " + line, plugin);
        }
        logWithColor("", plugin);
        logWithColor("§7  Made by: §b沙酱紫漏 (SandTripper)", plugin);
        logWithColor("", plugin);
    }
}