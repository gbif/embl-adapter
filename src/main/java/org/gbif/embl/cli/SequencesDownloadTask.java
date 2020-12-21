package org.gbif.embl.cli;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.gbif.embl.util.EmblAdapterConstants.DATA_URL;

public class SequencesDownloadTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SequencesDownloadTask.class);

  private final String rawEmblDataOutputFilePath;
  private final CyclicBarrier barrier;

  public SequencesDownloadTask(String rawEmblDataFilePath, CyclicBarrier barrier) {
    this.rawEmblDataOutputFilePath = rawEmblDataFilePath;
    this.barrier = barrier;
  }

  @Override
  public void run() {
    CommandLine cmd = new CommandLine("curl");
    cmd.addArgument("-X");
    cmd.addArgument("GET");
    cmd.addArgument(DATA_URL);
    cmd.addArgument("-o");
    cmd.addArgument(rawEmblDataOutputFilePath);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);

    try {
      executor.execute(cmd);
      barrier.await();
    } catch (IOException e) {
      LOG.error("IOException while getting EMBL data", e);
    } catch (InterruptedException | BrokenBarrierException e) {
      LOG.error("Exception while waiting other tasks", e);
    }
  }
}
