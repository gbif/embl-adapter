package org.gbif.embl.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.embl.api.EmblResponse;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.gbif.embl.util.EmblAdapterConstants.FIELDS;
import static org.gbif.embl.util.EmblAdapterConstants.FORMAT_JSON;
import static org.gbif.embl.util.EmblAdapterConstants.QUERY_COUNTRY;
import static org.gbif.embl.util.EmblAdapterConstants.RESULT_SEQUENCE;

@RequestMapping("search")
public interface EmblClient {

  @RequestMapping(
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<EmblResponse> search(
      @SpringQueryMap Pageable pageable,
      @RequestParam("result") String result,
      @RequestParam("format") String format,
      @RequestParam("query") String query,
      @RequestParam("fields") List<String> fields
  );

  default List<EmblResponse> search(Pageable pageable, String query) {
    return search(pageable, RESULT_SEQUENCE, FORMAT_JSON, query, FIELDS);
  }

  default List<EmblResponse> searchSequencesWithCountry(Pageable pageable) {
    return search(pageable, RESULT_SEQUENCE, FORMAT_JSON, QUERY_COUNTRY, FIELDS);
  }
}
