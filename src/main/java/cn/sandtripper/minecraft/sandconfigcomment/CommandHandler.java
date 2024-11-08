package cn.sandtripper.minecraft.sandconfigcomment;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

import static cn.sandtripper.minecraft.sandconfigcomment.ConstValues.PLUGIN_PREFIX;

@CommandAlias("sandconfigcomment|scc")
@CommandPermission("sandconfigcomment.admin")
public class CommandHandler extends BaseCommand {
    private SandConfigComment plugin;

    public CommandHandler(SandConfigComment plugin) {
        this.plugin = plugin;
    }

    @Subcommand("add")
    @CommandCompletion("@unwatched_plugin")
    public void onAddPlugin(CommandSender commandSender, String pluginName) {
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            commandSender.sendMessage(PLUGIN_PREFIX + "§c插件不存在");
            return;
        }
        if (targetPlugin.getName().equals(plugin.getName())) {
            commandSender.sendMessage(PLUGIN_PREFIX + "§c无法添加我自己！");
            return;
        }
        if (ConfigValues.CONFIG.PLUGIN_NAMES.contains(pluginName)) {
            commandSender.sendMessage(PLUGIN_PREFIX + "§e插件已在监视列表里");
            return;
        }
        plugin.addWatchPlugin(pluginName, true);
        commandSender.sendMessage(PLUGIN_PREFIX + "§7添加成功!");
    }

    @Subcommand("remove")
    @CommandCompletion("@watched_plugin")
    public void onRemovePlugin(CommandSender commandSender, String pluginName) {
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            commandSender.sendMessage(PLUGIN_PREFIX + "§c插件不存在");
            return;
        }
        if (!ConfigValues.CONFIG.PLUGIN_NAMES.contains(pluginName)) {
            commandSender.sendMessage(PLUGIN_PREFIX + "§e插件不在监视列表里");
            return;
        }
        plugin.removeWatchPlugin(pluginName);
        commandSender.sendMessage(PLUGIN_PREFIX + "§7移除成功");
    }

    @Subcommand("list")
    public void onListPlugin(CommandSender commandSender) {
        StringBuilder sb = new StringBuilder();
        for (String pluginName : ConfigValues.CONFIG.PLUGIN_NAMES) {
            sb.append(pluginName).append(", ");
        }
        commandSender.sendMessage(PLUGIN_PREFIX + "§7监视列表：§e[" + sb + "]");
    }


    @Subcommand("backup")
    public void onBackup(CommandSender commandSender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String time = plugin.getTimeString();
            Path targetPath = Paths.get("plugins", plugin.getName(), "backup", "command", time);
            if (plugin.backup(targetPath)) {
                commandSender.sendMessage(PLUGIN_PREFIX + "§7成功备份到：§f" + targetPath);
            } else {
                commandSender.sendMessage(PLUGIN_PREFIX + "§c备份失败");
            }
        });
    }


    @HelpCommand
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }


    @Subcommand("reload")
    public void onReloadConfig(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(PLUGIN_PREFIX + "§7重载成功");
    }
}
