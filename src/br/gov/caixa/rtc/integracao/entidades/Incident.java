package br.gov.caixa.rtc.integracao.entidades;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties({ "_links" })
public class Incident {
	private Values values;

	@JsonCreator
	public Incident(@JsonProperty("values") Values values) {
		this.setValues(values);
	}

	public Values getValues() {
		return values;
	}

	public void setValues(Values values) {
		this.values = values;
	}

}