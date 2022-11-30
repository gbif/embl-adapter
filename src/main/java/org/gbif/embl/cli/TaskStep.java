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

/**
 * {@link DataGeneratorTask} steps.
 */
public enum TaskStep {

  /**
   * Data download step.
   * Download dta from URLs.
   */
  DOWNLOAD_DATA,

  /**
   * Data store step.
   * Store downloaded data into DB (raw data tables).
   */
  STORE_DATA,

  /**
   * Process data step.
   * Processes and converts data, stores into DB (processed data tables).
   */
  PROCESS_DATA,

  /**
   * Delete data files step.
   * Removes temp data files.
   */
  DELETE_DATA_FILES
}
