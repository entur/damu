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
{{- end }}
