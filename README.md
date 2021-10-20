
# Damu [![CircleCI](https://circleci.com/gh/entur/damu/tree/master.svg?style=svg)](https://circleci.com/gh/entur/damu/tree/master)

Damu converts NeTEX datasets into GTFS datasets.
The actual GTFS conversion is performed by the [Netex-to-GTFS converter](https://github.com/entur/netex-gtfs-converter-java) library.

When [Chouette](https://github.com/entur/chouette) or [Uttu](https://github.com/entur/uttu) complete a NeTEx export, [Marduk](https://github.com/entur/marduk) notifies Damu. Damu downloads the exported NeTEx dataset from a bucket in Google Cloud Storage, converts it to GTFS, uploads it back to the GCS bucket and notifies Marduk. 

