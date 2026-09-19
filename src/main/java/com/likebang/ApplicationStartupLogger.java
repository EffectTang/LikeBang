package com.likebang;import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationStartupLogger {

    private final Environment environment;

    public ApplicationStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        // 处理 port=0 的情况（随机端口），从 WebServerInitializedEvent 获取更准确，但一般配置里不会写 0
        log.info("=================================================");
        log.info("  LikeBang 启动成功！");
        log.info("  本地访问: http://localhost:{}{}", port, contextPath);
        log.info("=================================================");
    }
}
