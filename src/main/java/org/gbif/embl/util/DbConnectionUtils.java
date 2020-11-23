package org.gbif.embl.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DbConnectionUtils {

  public static HikariDataSource dataSource;

  static {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql://localhost:5432/test_insert");
    config.setUsername("mpodolskiy");
    config.setPassword("");
    config.addDataSourceProperty("minimumIdle", "5");
    config.addDataSourceProperty("maximumPoolSize", "25");

    dataSource = new HikariDataSource(config);
  }

  private DbConnectionUtils() {
  }
}
