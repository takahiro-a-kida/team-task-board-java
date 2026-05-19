package com.example.taskboard.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DataSource dataSource;
    private final boolean seedEnabled;
    private final String dataSourceUrl;

    public DataInitializer(DataSource dataSource,
                           @Value("${taskboard.seed.enabled:true}") boolean seedEnabled,
                           @Value("${spring.datasource.url:}") String dataSourceUrl) {
        this.dataSource = dataSource;
        this.seedEnabled = seedEnabled;
        this.dataSourceUrl = dataSourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ensureFileDirectory();
        try (var conn = dataSource.getConnection()) {
            log.info("スキーマを適用します (datasource={})", dataSourceUrl);
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/schema.sql"));
            boolean empty = isEmpty(conn);
            if (seedEnabled && empty) {
                log.info("usersテーブルが空のため seed データを投入します");
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed.sql"));
            } else {
                log.info("seed はスキップ (enabled={}, empty={})", seedEnabled, empty);
            }
        }
    }

    private boolean isEmpty(Connection conn) throws Exception {
        try (var statement = conn.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }

    private void ensureFileDirectory() throws IOException {
        if (dataSourceUrl == null || !dataSourceUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        String path = dataSourceUrl.substring("jdbc:sqlite:".length());
        if (path.isEmpty() || path.contains(":memory:")) {
            return;
        }
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
