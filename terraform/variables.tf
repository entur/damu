#Enviroment variables
variable "gcp_project" {
  description = "The GCP project hosting the workloads"
}

variable "gcp_storage_project" {
  description = "The GCP project hosting the Google Storage resources"
}

variable "gcp_pubsub_project" {
  description = "The GCP project hosting the PubSub resources"
}

variable "gcp_resources_project" {
  description = "The GCP project hosting the project resources"
}

variable "kube_namespace" {
  description = "The Kubernetes namespace"
}

variable "labels" {
  description = "Labels used in all resources"
  type = map(string)
  default = {
    manager = "terraform"
    team = "ror"
    slack = "talk-ror"
    app = "damu"
  }
}

variable "load_config_file" {
  description = "Do not load kube config file"
  default = false
}

variable "service_account_bucket_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/storage/docs/access-control/iam-roles"
  default = "roles/storage.objectViewer"
}

variable "bucket_marduk_instance_name" {
  description = "Marduk bucket name"
}





