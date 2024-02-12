/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu;

public final class Constants {

  public static final String FILE_HANDLE = "RutebankenFileHandle";

  public static final String BLOBSTORE_PATH_OUTBOUND = "outbound/";

  public static final String DATASET_REFERENTIAL = "EnturDatasetReferential";

  /**
   * Headers originating from Marduk that must be sent back when notifying Marduk
   */
  public static final String CORRELATION_ID = "RutebankenCorrelationId";
  public static final String PROVIDER_ID = "RutebankenProviderId";
  public static final String ORIGINAL_PROVIDER_ID =
    "RutebankenOriginalProviderId";

  public static final String NETEX_FILENAME_PREFIX = "netex/";
  public static final String GTFS_FILENAME_PREFIX = "gtfs/";

  public static final String NETEX_FILENAME_SUFFIX = "-aggregated-netex.zip";
  public static final String GTFS_FILENAME_SUFFIX = "-aggregated-gtfs.zip";

  private Constants() {}
}
