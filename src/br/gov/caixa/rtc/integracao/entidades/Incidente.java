package br.gov.caixa.rtc.integracao.entidades;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;



@JsonIgnoreProperties({ "_links", "z1D_Action" })
public class Incidente {
    // Campos básicos para criar o incidente. Deve ser recuperado do item de trabalho no RTC
//    private String idIncidente;
    private String IdIntermoIncidente;
    private String status;
  
    
    @JsonCreator
    public Incidente(@JsonProperty("Entry ID")String idInternoIncidente, @JsonProperty("Status")String status) {
	this.IdIntermoIncidente = idInternoIncidente;
	this.status = status;
    }

    
    public String getIdIntermoIncidente() {
        return IdIntermoIncidente;
    }
    public void setIdIntermoIncidente(String idIntermoIncidente) {
        IdIntermoIncidente = idIntermoIncidente;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
   
    
    @Override
    public String toString() {
	return "Incidente [ IdIntermoIncidente=" + IdIntermoIncidente + ", status=" + status + "]";
    }
    

}
