package cn.bugstack.ai.rag;

import cn.bugstack.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 知识目录监听（ai.knowledge.watch-enabled=true 时启用）：
 * 文件新增/修改/删除事件去抖后触发增量入库，实现“改文件即生效”
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.knowledge", name = "watch-enabled", havingValue = "true")
public class KnowledgeDirectoryWatcher {

    private final KnowledgeIngestionService ingestionService;
    private final AiProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public KnowledgeDirectoryWatcher(KnowledgeIngestionService ingestionService, AiProperties properties) {
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        Thread thread = new Thread(this::watchLoop, "knowledge-watcher");
        thread.setDaemon(true);
        thread.start();
        log.info("知识目录监听已启动 root={}", properties.getKnowledge().getRootPath());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
    }

    private void watchLoop() {
        Path root = Paths.get(properties.getKnowledge().getRootPath()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.error("知识目录不存在，监听未生效: {}", root);
            return;
        }
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            registerAll(root, watchService);
            while (running.get()) {
                WatchKey key = watchService.take();
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path child = dir.resolve((Path) event.context());
                    handleEvent(child, event.kind());
                }
                key.reset();
            }
        } catch (Exception e) {
            log.error("知识目录监听异常终止", e);
        }
    }

    private void handleEvent(Path path, WatchEvent.Kind<?> kind) {
        // 去抖：等待 1.5s，让文件写入完成
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        try {
            if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                ingestionService.markDeleted(path);
                log.info("监听：文件删除已下线 path={}", path);
            } else if (Files.isDirectory(path)) {
                log.info("监听：目录事件忽略 path={}", path);
            } else if (ingestionService.parserSupport(path)) {
                ingestionService.ingestFile(path);
                log.info("监听：文件变更已增量入库 path={}", path);
            }
        } catch (Exception e) {
            log.error("监听处理失败 path={}", path, e);
        }
    }

    private void registerAll(final Path root, final WatchService watchService) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}