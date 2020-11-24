package org.gbif.embl.cli;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.client.EmblClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SequencesWithCatalogNumberTask extends SequencesTask {

  private static final Logger LOG = LoggerFactory.getLogger(SequencesWithCatalogNumberTask.class);

  private final AtomicLong offset;
  private final EmblClient emblClient;

  public SequencesWithCatalogNumberTask(long numRecords, AtomicLong offset, EmblClient emblClient) {
    super(numRecords);
    this.offset = offset;
    this.emblClient = emblClient;
  }

  @Override
  protected List<EmblResponse> getEmblData() {
    LOG.debug("Getting EMBL data...");
    List<EmblResponse> result =
        emblClient.searchSequencesWithSpecimenVoucher(new PagingRequest(offset.getAndAdd(LIMIT), LIMIT));
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
