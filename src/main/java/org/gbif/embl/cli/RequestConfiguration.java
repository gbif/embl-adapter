package org.gbif.embl.cli;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

@SuppressWarnings("PublicField")
public class RequestConfiguration {

  @NotNull public String url = "https://www.ebi.ac.uk/ena/portal/api/search";

  @NotNull public String dataPortal = "ena";

  @NotNull public String fields;

  @NotNull public Integer offset = 0;

  @NotNull public Integer limit = 0;

  @NotNull public String result;

  @NotNull public String query;

  @Override
  public String toString() {
    return new StringJoiner(", ", RequestConfiguration.class.getSimpleName() + "[", "]")
        .add("url='" + url + "'")
        .add("dataPortal='" + dataPortal + "'")
        .add("fields='" + fields + "'")
        .add("offset=" + offset)
        .add("limit=" + limit)
        .add("result='" + result + "'")
        .add("query='" + query + "'")
        .toString();
  }
}
