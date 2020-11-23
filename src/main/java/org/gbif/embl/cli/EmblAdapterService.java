package org.gbif.embl.cli;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.gbif.embl.client.EmblClient;
import org.gbif.ws.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

  private final ScheduledExecutorService scheduler;
  private final SequencesWithCountryTask sequencesWithCountryTask;
  private final SequencesWithCoordinatesTask sequencesWithCoordinatesTask;

  private final Integer frequencyInDays;
  private final Integer startHour;
  private final Integer startMinute;
  private final Long initialDelay;

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);
    EmblClient emblClient = new ClientBuilder()
        .withUrl("https://www.ebi.ac.uk/ena/portal/api/")
        .withConnectionPoolConfig(CONNECTION_POOL_CONFIG)
        .build(EmblClient.class);
    this.sequencesWithCountryTask = new SequencesWithCountryTask(1000, emblClient);
    this.sequencesWithCoordinatesTask = new SequencesWithCoordinatesTask(1000, emblClient);

    if (StringUtils.contains(config.startTime, ":")) {
      String[] timeParts = config.startTime.split(":");
      this.startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      this.startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    } else {
      this.startHour = null;
      this.startMinute = null;
    }

    long initialDelay = 0;
    if (this.startHour != null) {
      LocalTime t = LocalTime.of(this.startHour, this.startMinute);
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
    LOG.info("Service started!");
    scheduleTask(sequencesWithCountryTask);
    scheduleTask(sequencesWithCountryTask);
    scheduleTask(sequencesWithCoordinatesTask);
    scheduleTask(sequencesWithCoordinatesTask);
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
