package org.gbif.embl.cli;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class EmblAdapterService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(EmblAdapterService.class);

  private static final int DEFAULT_START_HOUR = 0;
  private static final int DEFAULT_START_MINUTE = 0;
  private static final int DEFAULT_FREQUENCY = 7;

  private final ScheduledExecutorService scheduler;

  private final Integer frequencyInDays;
  private final Long initialDelay;
  private final String workingDirectory;
  private final String metadataFilePath;
  private final String rawEmblDataOutputFilePath;

  public EmblAdapterService(EmblAdapterConfiguration config) {
    this.scheduler = Executors.newScheduledThreadPool(8);
    this.workingDirectory = config.workingDirectory;
    this.metadataFilePath = config.metadataFile;
    this.rawEmblDataOutputFilePath = config.rawEmblDataOutputFile;
    this.frequencyInDays = ObjectUtils.defaultIfNull(config.frequencyInDays, DEFAULT_FREQUENCY);

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
    CyclicBarrier barrier = new CyclicBarrier(2);
    LOG.debug("Created barrier of {} tasks", barrier.getParties());

    scheduleTask(new ArchiveGeneratorTask(rawEmblDataOutputFilePath, barrier, workingDirectory, metadataFilePath));
    scheduleTask(new SequencesDownloadTask(rawEmblDataOutputFilePath, barrier));
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
