package cn.sandtripper.minecraft.sandconfigcomment;

import cn.sandtripper.minecraft.sandconfigcomment.utils.StartPrint;
import co.aikar.commands.BukkitCommandManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public final class SandConfigComment extends JavaPlugin {
    HashMap<String, ConfigReader> configReaderHashMap = new HashMap<>();
    private YamlWatcherUtil yamlWatcher;

    @Override
    public void onEnable() {
        int pluginId = 23815;
        Metrics metrics = new Metrics(this, pluginId);

        new StartPrint(this).print(ConstValues.START_PICTURE);
        saveDefaultConfig();

        reload();

        if (ConfigValues.CONFIG.BACKUP_WHEN_START) {
            getLogger().info("正在备份工作区");
            Path backupPath = Paths.get("plugins", this.getName(), "backup", "startup", getTimeString());
            if (backup(backupPath)) {
                getLogger().info("成功备份到：" + backupPath);
            }
        }


        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.getCommandCompletions().registerCompletion("all_plugin", c -> Arrays.asList(Arrays.stream(getServer().getPluginManager().getPlugins())
                .map(Plugin::getName)
                .toArray(String[]::new)));
        commandManager.getCommandCompletions().registerCompletion("watched_plugin", c -> ConfigValues.CONFIG.PLUGIN_NAMES);
        commandManager.registerCommand(new CommandHandler(this));


    }

    @Override
    public void onDisable() {
        this.yamlWatcher.close(false);
        if (ConfigValues.CONFIG.BACKUP_WHEN_STOP) {
            getLogger().info("正在备份工作区");
            Path backupPath = Paths.get("plugins", this.getName(), "backup", "shutdown", getTimeString());
            if (backup(backupPath)) {
                getLogger().info("成功备份到：" + backupPath);
            }
        }
    }

    public void fileChange(String filename) {
        ConfigReader configReader = saveGetConfigReader(filename);
        if (!configReader.isConfigFileChanged()) {
            return;
        }
        configReader.reloadConfig();
        if (isPluginConfig(filename)) {
            if (ConfigValues.CONFIG.PLUGIN_TO_WORKSPACE) {
                Path targetPath = Paths.get("plugins", this.getName(), filename);
                ConfigReader targetConfigReader = saveGetConfigReader(targetPath.toString());
                copyConfigurationValues(configReader.getConfig(), targetConfigReader.getConfig());
                targetConfigReader.saveConfig();
                getLogger().info(targetPath.subpath(3, targetPath.getNameCount()) + " -> " + targetPath.subpath(1, targetPath.getNameCount()));
            }
        } else {
            if (ConfigValues.CONFIG.WORKSPACE_TO_PLUGIN) {
                Path filePath = Paths.get(filename);
                Path targetPath = filePath.subpath(2, filePath.getNameCount());
                ConfigReader targetConfigReader = saveGetConfigReader(targetPath.toString());
                copyConfigurationValues(configReader.getConfig(), targetConfigReader.getConfig());
                targetConfigReader.saveConfig();
                getLogger().info(filePath.subpath(1, filePath.getNameCount()) + " -> " + targetPath.subpath(1, targetPath.getNameCount()));
            }
        }
    }

    private void copyConfigurationValues(ConfigurationSection sourceSection, ConfigurationSection targetSection) {
        Set<String> sourceKeys = sourceSection.getKeys(false);
        Set<String> targetKeys = targetSection.getKeys(false);

        // 删除目标配置文件中存在但源配置文件中不存在的键值对
        for (String key : targetKeys) {
            if (!sourceKeys.contains(key)) {
                targetSection.set(key, null);
            }
        }

        // 拷贝源配置文件中的键值对到目标配置文件
        for (String key : sourceKeys) {
            if (sourceSection.isConfigurationSection(key)) {
                ConfigurationSection sourceSubSection = sourceSection.getConfigurationSection(key);
                if (!targetSection.isConfigurationSection(key)) {
                    targetSection.createSection(key);
                }
                ConfigurationSection targetSubSection = targetSection.getConfigurationSection(key);
                copyConfigurationValues(sourceSubSection, targetSubSection);
            } else {
                Object value = sourceSection.get(key);
                if (!value.equals(targetSection.get(key)))
                    targetSection.set(key, value);
            }
        }
    }


    private ConfigReader saveGetConfigReader(String filePath) {
        if (!configReaderHashMap.containsKey(filePath)) {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            Path parent = path.getParent();
            configReaderHashMap.put(filePath, new ConfigReader(this, parent.toFile(), fileName, true));
        }
        return configReaderHashMap.get(filePath);
    }

    private boolean isPluginConfig(String fileName) {
        return !fileName.startsWith(Paths.get("plugins", this.getName()).toString());
    }

    public void initWatchPlugin() {
        for (String pluginName : ConfigValues.CONFIG.PLUGIN_NAMES) {
            Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
            if (plugin == null)
                return;
            this.yamlWatcher.addWatchPlugin(pluginName);
        }
    }

    public void addWatchPlugin(String pluginName) {
        Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
        if (plugin == null)
            return;
        try {
            initTargetWorkspace(plugin.getDataFolder().toPath(), Paths.get("plugins", this.getName(), "plugins", plugin.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.yamlWatcher.addWatchPlugin(pluginName);
        ConfigValues.CONFIG.PLUGIN_NAMES.add(pluginName);
        ConfigValues.save(this);
    }

    public void removeWatchPlugin(String pluginName) {
        this.yamlWatcher.removeWatchPlugin(pluginName);
        ConfigValues.CONFIG.PLUGIN_NAMES.remove(pluginName);
        ConfigValues.save(this);
    }

    public void reload() {
        ConfigValues.reload(this);
        if (yamlWatcher != null) {
            yamlWatcher.close(true);
        }
        yamlWatcher = new YamlWatcherUtil(this, filename -> fileChange(filename), ConfigValues.CONFIG.DELAY_MS);
        initWatchPlugin();
    }

    private void copyFiles(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetSubDir = targetDir.resolve(sourceDir.relativize(dir));
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    public void initTargetWorkspace(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path targetFile, BasicFileAttributes attrs) throws IOException {
                if (targetFile.toString().endsWith(".yml")) {
                    Path relativeTargetFile = targetDir.relativize(targetFile);
                    Path correspondingSourceFile = sourceDir.resolve(relativeTargetFile);
                    if (!Files.exists(correspondingSourceFile)) {
                        Files.delete(targetFile);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null && Files.list(dir).count() == 0) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                if (sourceFile.toString().endsWith(".yml")) {
                    Path relativeSourceFile = sourceDir.relativize(sourceFile);
                    Path correspondingTargetFile = targetDir.resolve(relativeSourceFile);
                    if (!Files.exists(correspondingTargetFile)) {
                        Files.createDirectories(correspondingTargetFile.getParent());
                        Files.copy(sourceFile, correspondingTargetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }


    public boolean backup(Path backupPath) {
        Path targetDir = Paths.get("plugins", this.getName(), "plugins");
        try {
            copyFiles(targetDir, backupPath);
            return true;
        } catch (IOException e) {
            getLogger().severe("插件数据备份失败: " + e.getMessage());
            return false;
        }
    }

    public String getTimeString() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String time = now.format(formatter);
        return time;
    }
}
