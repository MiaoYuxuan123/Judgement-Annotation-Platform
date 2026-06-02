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
        if (isDatabaseInitialized()) {
            log.info("Database already initialized, skipping schema and data scripts.");
            return;
        }

        log.info("Database not initialized, running schema and data scripts.");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(true);
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource("db/admin.sql"));
        populator.addScript(new ClassPathResource("db/creator.sql"));
        populator.addScript(new ClassPathResource("db/annotator.sql"));
        populator.addScript(new ClassPathResource("db/reviewer.sql"));
        populator.addScript(new ClassPathResource("db/data.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
        log.info("Database initialization completed.");
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
