{{/*
Return the name of the chart
*/}}
{{- define "smart-content.name" -}}
smart-content
{{- end }}

{{/*
Return the full name of the chart
*/}}
{{- define "smart-content.fullname" -}}
{{ include "smart-content.name" . }}
{{- end }}
