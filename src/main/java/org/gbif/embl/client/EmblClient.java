package org.gbif.embl.client;

import org.gbif.embl.api.EmblResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@RequestMapping("search")
public interface EmblClient {

  @RequestMapping(
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<EmblResponse> search(
      @RequestParam("result") String result,
      @RequestParam("format") String format,
      @RequestParam("limit") String limit,
      @RequestParam("query") String query,
      @RequestParam("fields") List<String> fields
  );
}
