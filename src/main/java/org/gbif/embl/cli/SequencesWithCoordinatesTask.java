package org.gbif.embl.cli;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.client.EmblClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

public class SequencesWithCoordinatesTask extends SequencesTask {

  private static final Logger LOG = LoggerFactory.getLogger(SequencesWithCoordinatesTask.class);

  private final AtomicLong offset;
  private final EmblClient emblClient;

  public SequencesWithCoordinatesTask(
      CyclicBarrier barrier,
      long numRecords,
      AtomicLong offset,
      EmblClient emblClient) {
    super(barrier, numRecords);
    this.offset = offset;
    this.emblClient = emblClient;
  }

  @Override
  protected List<EmblResponse> getEmblData() {
    LOG.debug("Getting EMBL data...");
    List<EmblResponse> result =
        emblClient.searchSequencesWithCoordinates(new PagingRequest(offset.getAndAdd(LIMIT), LIMIT));
    LOG.debug("EMBL data retrieved: {} records", result.size());
    return result;
  }

  @Override
  protected AtomicLong getOffset() {
    return offset;
  }

  @Override
  protected Logger getLog() {
    return LOG;
  }
}
