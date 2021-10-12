/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.embl.cli;

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

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
