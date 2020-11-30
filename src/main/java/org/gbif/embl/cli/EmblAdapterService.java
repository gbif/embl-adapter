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
import java.time.Duration;
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

  private static final int DEFAULT_START_HOUR = 0;
  private static final int DEFAULT_START_MINUTE = 0;
  private static final int DEFAULT_FREQUENCY = 7;

  private final DataSource dataSource;
  private final EmblClient emblClient;
  private final ScheduledExecutorService scheduler;

  private final Integer frequencyInDays;
  private final Long initialDelay;
  private final Integer numRecords;
  private final Integer limit;
  private final String workingDirectory;

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

    this.numRecords = config.numRecords;
    this.limit = config.limit;
    this.workingDirectory = config.workingDirectory;
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);
    this.emblClient = new ClientBuilder()
        .withUrl(config.emblEbiApi)
        .withConnectionPoolConfig(
            ClientBuilder.ConnectionPoolConfig.builder()
                .timeout(config.client.timeout)
                .maxConnections(config.client.maxConnections)
                .maxPerRoute(config.client.maxPerRoute)
                .build())
        .withExponentialBackoffRetry(
            Duration.ofMillis(config.client.initialInterval), config.client.multiplier, config.client.maxAttempts)
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
    LOG.debug("Created barrier of {} tasks", barrier.getParties());

    Integer numSequencesWithCountry = numRecords;
    Integer numSequencesWithCoordinates = numRecords;
    Integer numSequencesWithCatalogNumber = numRecords;
    Integer numSequencesWithIdentifiedBy = numRecords;
    if (numRecords == null) {
      numSequencesWithCountry = emblClient.countSequencesWithCountry();
      numSequencesWithCoordinates = emblClient.countSequencesWithCoordinates();
      numSequencesWithCatalogNumber = emblClient.countSequencesWithSpecimenVoucher();
      numSequencesWithIdentifiedBy = emblClient.countSequencesWithIdentifiedBy();
    }

    scheduleTask(new ArchiveGeneratorTask(dataSource, barrier, workingDirectory));

    scheduleTask(
        new SequencesWithCountryTask(
            dataSource, barrier, numSequencesWithCountry, offsetSequencesWithCountry, limit, emblClient));
    scheduleTask(
        new SequencesWithCoordinatesTask(
            dataSource, barrier, numSequencesWithCoordinates, offsetSequencesWithCoordinates, limit, emblClient));
    scheduleTask(
        new SequencesWithCatalogNumberTask(
            dataSource, barrier, numSequencesWithCatalogNumber, offsetSequencesWithCatalogNumber, limit, emblClient));
    scheduleTask(
        new SequencesWithIdentifiedByTask(
            dataSource, barrier, numSequencesWithIdentifiedBy, offsetSequencesWithIdentifiedBy, limit, emblClient));
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
