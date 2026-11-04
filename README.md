
# Damu

Damu converts NeTEX datasets into GTFS datasets.
The actual GTFS conversion is performed by the [Netex-to-GTFS converter](https://github.com/entur/netex-gtfs-converter-java) library.

When [Chouette](https://github.com/entur/chouette) or [Uttu](https://github.com/entur/uttu) complete a NeTEx export, [Marduk](https://github.com/entur/marduk) notifies Damu. Damu downloads the exported NeTEx dataset from a bucket in Google Cloud Storage, converts it to GTFS, uploads it back to the GCS bucket and notifies Marduk. 

# Deployment

Damu is built and deployed by GitHub Actions. A push to `master` builds and pushes the image, deploys it
to dev, and then waits for manual approval before tst and again before prd (`.github/workflows/push.yml`).
A previously built image can be redeployed to any environment with the *Manual deploy* workflow
(`.github/workflows/manual-deploy.yml`), using the git tag the build produced. The Helm chart lives in
`helm/damu`, with one values file per environment under `helm/damu/env/`.
