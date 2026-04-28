{{- define "smart-ticket.name" -}}
smart-ticket-platform
{{- end -}}

{{- define "smart-ticket.labels" -}}
app.kubernetes.io/name: {{ include "smart-ticket.name" . }}
app.kubernetes.io/part-of: smart-ticket-platform
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "smart-ticket.image" -}}
{{- printf "%s/%s:%s" .registry .image .tag -}}
{{- end -}}
