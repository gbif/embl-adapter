package org.gbif.embl.cli;

import com.google.common.util.concurrent.AbstractIdleService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.gbif.embl.client.EmblClient;
import org.gbif.ws.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UnstableApiUsage")
public class EmblAdapterService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(EmblAdapterService.class);

  private static final ClientBuilder.ConnectionPoolConfig CONNECTION_POOL_CONFIG =
      ClientBuilder.ConnectionPoolConfig.builder()
          .timeout(10000)
          .maxConnections(100)
          .maxPerRoute(100)
          .build();

  private static final int DEFAULT_START_HOUR = 0;
  private static final int DEFAULT_START_MINUTE = 0;
  private static final int DEFAULT_FREQUENCY = 7;

  private final DataSource dataSource;
  private final EmblClient emblClient;
  private final ScheduledExecutorService scheduler;

  private final Integer frequencyInDays;
  private final Long initialDelay;

  private final AtomicLong offsetSequencesWithCountry = new AtomicLong(0);
  private final AtomicLong offsetSequencesWithCoordinates = new AtomicLong(0);
  private final AtomicLong offsetSequencesWithCatalogNumber = new AtomicLong(0);
  private final AtomicLong offsetSequencesWithIdentifiedBy = new AtomicLong(0);

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.scheduler = Executors.newScheduledThreadPool(8);

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.db.url);
    hikariConfig.setUsername(config.db.user);
    hikariConfig.setPassword(config.db.password);
    hikariConfig.setMaximumPoolSize(config.db.maximumPoolSize);
    hikariConfig.setConnectionTimeout(config.db.connectionTimeout);

    this.dataSource = new HikariDataSource(hikariConfig);

    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);
    this.emblClient = new ClientBuilder()
        .withUrl("https://www.ebi.ac.uk/ena/portal/api/")
        .withConnectionPoolConfig(CONNECTION_POOL_CONFIG)
        .build(EmblClient.class);

    Integer startHour;
    Integer startMinute;
    if (StringUtils.contains(config.startTime, ":")) {
      String[] timeParts = config.startTime.split(":");
      startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    } else {
      startHour = null;
      startMinute = null;
    }

    long initialDelay = 0;
    if (startHour != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    // if the delay is passed then start it next day
    if (initialDelay < 0) {
      this.initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    } else {
      this.initialDelay = initialDelay;
    }
  }

  @Override
  protected void startUp() {
    LOG.info("Service started");
    CyclicBarrier barrier = new CyclicBarrier(5);
    scheduleTask(
        new SequencesWithCountryTask(dataSource, barrier, 500L, offsetSequencesWithCountry, emblClient));
    scheduleTask(
        new SequencesWithCoordinatesTask(
            dataSource, barrier, 500L, offsetSequencesWithCoordinates, emblClient));
    scheduleTask(
        new SequencesWithCatalogNumberTask(
            dataSource, barrier, 500L, offsetSequencesWithCatalogNumber, emblClient));
    scheduleTask(
        new SequencesWithIdentifiedByTask(
            dataSource, barrier, 500L, offsetSequencesWithIdentifiedBy, emblClient));
    scheduleTask(new ArchiveGeneratorTask(dataSource, barrier));
  }

  @Override
  protected void shutDown() {
    scheduler.shutdown();
  }

  private void scheduleTask(Runnable runnable) {
    scheduler.scheduleAtFixedRate(
        runnable,
        initialDelay,
        frequencyInDays * (ChronoUnit.DAYS.getDuration().toMinutes()),
        TimeUnit.MINUTES);
  }
}
