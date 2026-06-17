package edu.nju.jap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Order(0)
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String MARKER_TABLE = "sys_user";

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean initialized = isDatabaseInitialized();
        log.info(initialized ? "Database already initialized, running schema-only." : "Fresh database, running full setup.");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(true);
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource("db/admin.sql"));
        populator.addScript(new ClassPathResource("db/creator.sql"));
        populator.addScript(new ClassPathResource("db/annotator.sql"));
        populator.addScript(new ClassPathResource("db/reviewer.sql"));

        if (!initialized) {
            populator.addScript(new ClassPathResource("db/data.sql"));
        }

        DatabasePopulatorUtils.execute(populator, dataSource);
        patchMissingColumns();
        log.info("Database initialization completed.");
    }

    private void patchMissingColumns() {
        try (Connection connection = dataSource.getConnection()) {
            addColumnIfMissing(connection, "sys_user", "is_deleted",
                    "ALTER TABLE `sys_user` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0=正常 1=已删除'");
            addColumnIfMissing(connection, "guide_version", "attachment_name",
                    "ALTER TABLE `guide_version` ADD COLUMN `attachment_name` VARCHAR(255) DEFAULT NULL");
            addColumnIfMissing(connection, "annotation", "layout_json",
                    "ALTER TABLE `annotation` ADD COLUMN `layout_json` JSON DEFAULT NULL COMMENT '论证图布局覆盖' AFTER `updated_at`");
            addColumnIfMissing(connection, "annotation", "reject_reason",
                    "ALTER TABLE `annotation` ADD COLUMN `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '裁定不予采纳理由' AFTER `layout_json`");
            addColumnIfMissing(connection, "task", "deadline",
                    "ALTER TABLE `task` ADD COLUMN `deadline` DATETIME DEFAULT NULL COMMENT '任务截止日期' AFTER `stage_changed_at`");
            addColumnIfMissing(connection, "arbitration_snapshot", "based_on_annotator_id",
                    "ALTER TABLE `arbitration_snapshot` ADD COLUMN `based_on_annotator_id` BIGINT DEFAULT NULL COMMENT '裁定基于的标注员ID' AFTER `arbitrated_at`");
            createTableIfMissing(connection, "message",
                    "CREATE TABLE `message` (" +
                    "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "`user_id` BIGINT NOT NULL," +
                    "`type` VARCHAR(32) NOT NULL DEFAULT 'INFO'," +
                    "`title` VARCHAR(255) NOT NULL," +
                    "`content` VARCHAR(2000) DEFAULT NULL," +
                    "`task_id` INT DEFAULT NULL," +
                    "`task_document_id` INT DEFAULT NULL," +
                    "`data_id` INT DEFAULT NULL," +
                    "`is_read` TINYINT NOT NULL DEFAULT 0," +
                    "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX `idx_user_id` (`user_id`)," +
                    "INDEX `idx_user_read` (`user_id`, `is_read`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表'");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to patch database schema", ex);
        }
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String ddl) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet columns = metaData.getColumns(catalog, null, tableName, columnName)) {
            if (columns.next()) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("Patched missing column {}.{}", tableName, columnName);
        }
    }

    private void createTableIfMissing(Connection connection, String tableName, String ddl) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet tables = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (tables.next()) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("Created missing table {}", tableName);
        }
    }

    private boolean isDatabaseInitialized() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet tables = metaData.getTables(catalog, null, MARKER_TABLE, new String[]{"TABLE"})) {
                return tables.next();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to check database initialization status", ex);
        }
    }
}
