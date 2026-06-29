{{/* Generate basic labels */}}
{{- define "damu.common.labels" }}
app: damu
release: {{ .Release.Name }}
team: ror
slack: talk-ror
type: backend
environment: {{.Values.common.env }}
customLogRetention: enabled
namespace: {{ .Release.Namespace }}
app.kubernetes.io/managed-by: Helm
{{- end }}

{{/* Generate common Helm ownership annotations */}}
{{- define "damu.common.annotations" }}
meta.helm.sh/release-name: {{ .Release.Name }}
meta.helm.sh/release-namespace: {{ .Release.Namespace }}
{{- end }}
