package cn.sandtripper.minecraft.sandconfigcomment;

import java.util.ArrayList;
import java.util.HashSet;

public class ConfigValues {
    public static void reload(SandConfigComment plugin) {
        CONFIG.reload(plugin);
    }

    public static void save(SandConfigComment plugin) {
        CONFIG.save(plugin);
    }

    public static class CONFIG {
        public static Boolean WORKSPACE_TO_PLUGIN;
        public static Boolean PLUGIN_TO_WORKSPACE;
        public static int DELAY_MS;
        public static boolean BACKUP_WHEN_START;
        public static boolean BACKUP_WHEN_STOP;
        public static HashSet<String> PLUGIN_NAMES;

        public static void reload(SandConfigComment plugin) {
            plugin.reloadConfig();
            CONFIG.WORKSPACE_TO_PLUGIN = plugin.getConfig().getBoolean("workspace-to-plugin");
            CONFIG.PLUGIN_TO_WORKSPACE = plugin.getConfig().getBoolean("plugin-to-workspace");
            CONFIG.DELAY_MS = plugin.getConfig().getInt("delay-ms");
            CONFIG.BACKUP_WHEN_START = plugin.getConfig().getBoolean("backup-when-start");
            CONFIG.BACKUP_WHEN_STOP = plugin.getConfig().getBoolean("backup-when-stop");
            CONFIG.PLUGIN_NAMES = new HashSet<>(plugin.getConfig().getStringList("plugin-names"));
        }

        public static void save(SandConfigComment plugin) {
            plugin.getConfig().set("workspace-to-plugin", WORKSPACE_TO_PLUGIN);
            plugin.getConfig().set("plugin-to-workspace", PLUGIN_TO_WORKSPACE);
            plugin.getConfig().set("delay-ms", DELAY_MS);
            plugin.getConfig().set("backup-when-start", BACKUP_WHEN_START);
            plugin.getConfig().set("backup-when-stop", BACKUP_WHEN_STOP);
            plugin.getConfig().set("plugin-names", new ArrayList<>(PLUGIN_NAMES));
            plugin.saveConfig();
        }
    }
}
