package cn.sandtripper.minecraft.sandconfigcomment;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class YamlWatcherUtil {

    private JavaPlugin plugin;
    private WatchService watchService;
    private Map<WatchKey, Path> watchKeyPathMap;
    private Consumer<String> callback;
    private Map<String, Integer> numberInQueue;
    private long delayMillis;
    private BlockingQueue<CallbackTask> taskQueue;

    private boolean isRunning = true;

    public YamlWatcherUtil(JavaPlugin plugin, Consumer<String> callback, long delayMillis) {
        this.plugin = plugin;
        this.callback = callback;
        this.watchKeyPathMap = new HashMap<>();
        this.numberInQueue = new HashMap<>();
        this.delayMillis = delayMillis;
        this.taskQueue = new LinkedBlockingQueue<>();

        try {
            // 创建 WatchService
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 启动监视线程
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::watchFiles);

        // 启动任务执行线程
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::executeTasks);
    }

    public void addWatchPlugin(String pluginName) {
        Path pluginPath = Paths.get("plugins", pluginName);
        Path workspacePath = Paths.get("plugins", plugin.getName(), "plugins", pluginName);
        registerWatchRecursively(pluginPath);
        registerWatchRecursively(workspacePath);
    }

    public void removeWatchPlugin(String pluginName) {
        Path pluginPath = Paths.get("plugins", pluginName);
        Path workspacePath = Paths.get("plugins", plugin.getName(), "plugins", pluginName);
        unregisterWatchRecursively(pluginPath);
        unregisterWatchRecursively(workspacePath);
    }

    private void registerWatchRecursively(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    watchKeyPathMap.put(watchKey, dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unregisterWatchRecursively(Path path) {
        watchKeyPathMap.entrySet().removeIf(entry -> {
            Path watchPath = entry.getValue();
            if (watchPath.startsWith(path)) {
                WatchKey watchKey = entry.getKey();
                watchKey.cancel(); // 取消对文件夹的监视
                return true;
            }
            return false;
        });
    }

    private void watchFiles() {
        while (isRunning) {
            try {
                // 等待事件发生
                WatchKey watchKey = watchService.poll(500, TimeUnit.MICROSECONDS);
                if (watchKey == null) {
                    continue;
                }
                Path dirPath = watchKeyPathMap.get(watchKey);

                // 处理监视事件
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    Path filePath = dirPath.resolve((Path) event.context());

                    // 检查文件是否为 .yml 文件
                    if (filePath.toString().endsWith(".yml")) {
                        String filePathStr = filePath.toString();

                        addNumberInQueue(filePathStr, 1);

                        // 将任务添加到队列中
                        taskQueue.offer(new CallbackTask(System.currentTimeMillis() + delayMillis, filePathStr));
                    }
                }

                // 重置监视键
                boolean valid = watchKey.reset();
                if (!valid) {
                    // 如果监视键无效,可能是文件夹被删除,从 watchKeyPathMap 中移除
                    plugin.getLogger().warning("监视键" + watchKey + "无效,可能文件夹被删除,从监视列表中移除");
                    watchKey.cancel(); // 显式取消对文件夹的监视
                    watchKeyPathMap.remove(watchKey);
                }
            } catch (InterruptedException e) {
                plugin.getLogger().info("监视线程被中断,退出监视");
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized int addNumberInQueue(String filePath, int number) {
        if (!numberInQueue.containsKey(filePath)) {
            numberInQueue.put(filePath, 0);
        }
        numberInQueue.put(filePath, numberInQueue.get(filePath) + number);
        if (numberInQueue.get(filePath) == 0) {
            numberInQueue.remove(filePath);
        }
        return numberInQueue.getOrDefault(filePath, 0);
    }

    private void executeTasks() {
        while (isRunning) {
            try {
                // 从队列中获取任务
                CallbackTask task = taskQueue.poll(1000, TimeUnit.MICROSECONDS);
                if (task == null) {
                    continue;
                }

                long currentTime = System.currentTimeMillis();

                if (currentTime < task.executeTime) {
                    Thread.sleep(task.executeTime - currentTime);
                }

                if (addNumberInQueue(task.filePath, -1) == 0) {
                    callback.accept(task.filePath);
                }

            } catch (InterruptedException e) {
                plugin.getLogger().info("回调线程被中断，退出");
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close(boolean isAsync) {
        if (isAsync) {
            isRunning = false;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    watchService.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 12L);
        } else {
            try {
                isRunning = false;
                Thread.sleep(600);
                watchService.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static private class CallbackTask {
        public long executeTime;
        private String filePath;

        public CallbackTask(long executeTime, String filePath) {
            this.executeTime = executeTime;
            this.filePath = filePath;
        }
    }

}
