package com.example.taskboard.config;

import java.nio.file.Path;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import com.zaxxer.hikari.HikariDataSource;

class DataInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsSeedWhenUsersAlreadyExistWithSingleConnectionPool() throws Exception {
        String dataSourceUrl = "jdbc:sqlite:" + tempDir.resolve("taskboard.db");
        try (HikariDataSource dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl(dataSourceUrl);
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setMaximumPoolSize(1);
            dataSource.setConnectionTimeout(500);

            createSchemaAndExistingUser(dataSource);

            DataInitializer initializer = new DataInitializer(dataSource, true, dataSourceUrl);

            assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                    .doesNotThrowAnyException();
            assertThat(countRows(dataSource, "users")).isEqualTo(1);
            assertThat(countRows(dataSource, "teams")).isZero();
        }
    }

    private static void createSchemaAndExistingUser(DataSource dataSource) throws Exception {
        try (var conn = dataSource.getConnection();
             var statement = conn.createStatement()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/schema.sql"));
            statement.executeUpdate("""
                    INSERT INTO users (id, username, password_hash, display_name)
                    VALUES (1, 'alice', 'hash', 'アリス')
                    """);
        }
    }

    private static int countRows(DataSource dataSource, String table) throws Exception {
        try (var conn = dataSource.getConnection();
             var statement = conn.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
