package br.gov.caixa.rtc.integracao.entidades;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties({ "_links" })
public final class IncidentsList {
	private Incident incidents[];

	@JsonCreator
	public IncidentsList(@JsonProperty("entries") Incident[] incident) {
		this.incidents = incident;
	}
	public Incident[] getIncident() {
	    return incidents;
	}
	public void setIncident(Incident[] incident) {
	    this.incidents = incident;
	}

	@Override
	public String toString() {
		return "IncidentsCMDBList: " + (incidents != null ? incidents.length : null);
	}
}