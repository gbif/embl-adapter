package org.gbif.embl.cli;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.embl.api.EmblResponse;
import org.gbif.embl.client.EmblClient;
import org.gbif.embl.util.DwcArchiveBuilder;
import org.gbif.ws.client.ClientBuilder;

import java.io.File;
import java.util.Date;
import java.util.List;

import static org.gbif.embl.util.EmblAdapterConstants.QUERY_COUNTRY;
import static org.gbif.embl.util.EmblAdapterConstants.QUERY_GEO_BOX;
import static org.gbif.embl.util.EmblAdapterConstants.QUERY_IDENTIFIED_BY;
import static org.gbif.embl.util.EmblAdapterConstants.QUERY_SPECIMEN_VOUCHER;

public class EmblAdapter {

  private static final String ARCHIVE_NAME = "embl-archive_%s.zip";
  private static final Pageable DEFAULT_PAGE = new PagingRequest(0, 100);

  private final EmblClient emblClient;

  public EmblAdapter() {
    String wsUrl = "https://www.ebi.ac.uk/ena/portal/api/";
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(wsUrl);
    emblClient = clientBuilder.build(EmblClient.class);
  }

  public void joinSequences() {
    List<EmblResponse> result = sequencesWithCoordinates();
    result.addAll(sequencesWithCatalogNumber());
    result.addAll(sequencesWithIdentifiedBy());
    result.addAll(sequencesWithCountry());

    DwcArchiveBuilder dwcArchiveBuilder = new DwcArchiveBuilder();
    dwcArchiveBuilder.buildArchive(
        new File("output", String.format(ARCHIVE_NAME, new Date().getTime())), result);
  }

  public List<EmblResponse> sequencesWithCoordinates() {
    return emblClient.search(DEFAULT_PAGE, QUERY_GEO_BOX);
  }

  public List<EmblResponse> sequencesWithCatalogNumber() {
    return emblClient.search(DEFAULT_PAGE, QUERY_SPECIMEN_VOUCHER);
  }

  public List<EmblResponse> sequencesWithIdentifiedBy() {
    return emblClient.search(DEFAULT_PAGE, QUERY_IDENTIFIED_BY);
  }

  public List<EmblResponse> sequencesWithCountry() {
    return emblClient.search(DEFAULT_PAGE, QUERY_COUNTRY);
  }
}
