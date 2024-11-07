package cn.sandtripper.minecraft.sandconfigcomment;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConfigReader {
    private final JavaPlugin plugin;
    private final String configName;
    private File configFile;
    private FileConfiguration config;
    private boolean saveFileHash;
    private byte[] lastFileHash;

    public ConfigReader(JavaPlugin plugin, String filename) {
        this(plugin, filename, false);
    }

    public ConfigReader(JavaPlugin plugin, File parent, String filename) {
        this(plugin, parent, filename, false);
    }

    public ConfigReader(JavaPlugin plugin, String filename, boolean saveFileHash) {
        this.plugin = plugin;
        this.configName = filename;
        this.configFile = new File(plugin.getDataFolder(), filename);
        this.saveFileHash = saveFileHash;
    }

    public ConfigReader(JavaPlugin plugin, File parent, String filename, boolean saveFileHash) {
        this.plugin = plugin;
        this.configName = filename;
        this.configFile = new File(parent, filename);
        this.saveFileHash = saveFileHash;
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                InputStream defaultStream = plugin.getResource(configName);
                YamlConfiguration defaultFile = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                config = defaultFile;
                saveConfig();
            } catch (IOException e) {
                plugin.getLogger().severe("配置文件" + configFile.getPath() + "创建失败!");
            }
        }
    }

    public FileConfiguration getConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("配置文件" + configFile.getPath() + "创建失败!");
            }
        }
        if (config == null || (saveFileHash && isConfigFileChanged())) {
            this.reloadConfig();
        }
        return config;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), configName);
        }
        try (InputStream inputStream = new FileInputStream(configFile)) {
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            InputStream defaultStream = plugin.getResource(configName);
            if (defaultStream != null) {
                YamlConfiguration defaultFile = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                config.setDefaults(defaultFile);
            }
            if (saveFileHash) {
                lastFileHash = calculateFileHash(new FileInputStream(configFile));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("配置文件" + configName + "读取失败!");
        }
    }

    public void saveConfig() {
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            config.save(configFile);
            if (saveFileHash) {
                try (InputStream inputStream = new FileInputStream(configFile)) {
                    lastFileHash = calculateFileHash(inputStream);
                }
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("配置保存失败!请重试!");
        }
    }

    public boolean isConfigFileChanged() {
        if (!saveFileHash) {
            return false;
        }
        try (InputStream inputStream = new FileInputStream(configFile)) {
            byte[] currentFileHash = calculateFileHash(inputStream);
            return !MessageDigest.isEqual(lastFileHash, currentFileHash);
        } catch (IOException e) {
            plugin.getLogger().severe("配置文件" + configFile.getPath() + "读取失败!");
            return false;
        }
    }

    private byte[] calculateFileHash(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            plugin.getLogger().severe("不支持的哈希算法: SHA-256");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            plugin.getLogger().severe("读取文件内容时发生 I/O 异常");
            e.printStackTrace();
            return null;
        }
    }

}
