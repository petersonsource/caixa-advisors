package br.gov.caixa.rtc.integracao.entidades;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import br.gov.caixa.rtc.util.Formatters;

import com.fasterxml.jackson.annotation.JsonProperty;



@JsonIgnoreProperties({ "_embedded","_links" })
public class NotaAtividades {
    
//   Campos que serão utilizados para Atualizar o item no ITSM. 
//   Criando uma nota no item para os estados no RTC que estão configurados para o advisor
    
    @JsonProperty("z1D Action")
    private String z1D_Action = "MODIFY";
    private String z1D_Secure_Log = "Yes";
    private String z1D_View_Access = "Public";
    private String z1D_CommunicationSource = "Other";
    private String z1D_Activity_Type = "General Information";
    private String z1D_Details = null;
    private String z2AF_Act_Attachment_1 = null;
    
    
    @JsonCreator
    public NotaAtividades() {
	
    }
    
    @JsonCreator
    public NotaAtividades(String z1D_Activity_Type, String z1D_Details, String z2AF_Act_Attachment_1 ){
	this.z1D_Activity_Type = z1D_Activity_Type;
//	System.out.println("String ANTES conversão do CONSTRUTOR: " + z1D_Details);
//	this.z1D_Details = Formatters.parseHTML2String(z1D_Details);
//	System.out.println("String DEPOIS conversão do CONSTRUTOR: " + this.z1D_Details);
	this.z1D_Details = z1D_Details;
	this.z2AF_Act_Attachment_1 = z2AF_Act_Attachment_1;
    }

    
    public String getZ1D_Action() {
        return z1D_Action;
    }
    public void setZ1D_Action(String z1d_Action) {
        z1D_Action = z1d_Action;
    }
    public String getZ1D_Secure_Log() {
        return z1D_Secure_Log;
    }
    public void setZ1D_Secure_Log(String z1d_Secure_Log) {
        z1D_Secure_Log = z1d_Secure_Log;
    }
    public String getZ1D_View_Access() {
        return z1D_View_Access;
    }
    public void setZ1D_View_Access(String z1d_View_Access) {
        z1D_View_Access = z1d_View_Access;
    }
    public String getZ1D_CommunicationSource() {
        return z1D_CommunicationSource;
    }
    public void setZ1D_CommunicationSource(String z1d_CommunicationSource) {
        z1D_CommunicationSource = z1d_CommunicationSource;
    }
    public String getZ1D_Activity_Type() {
        return z1D_Activity_Type;
    }
    public void setZ1D_Activity_Type(String z1d_Activity_Type) {
        z1D_Activity_Type = z1d_Activity_Type;
    }
    public String getZ1D_Details() {
//	 System.out.println("String ANTES conversão do GET: " + z1D_Details);
//	 String detalhes = Formatters.parseHTML2String(z1D_Details);
//	 System.out.println("String DEPOIS conversão do GET: " + detalhes);
        return z1D_Details;
    }
    public void setZ1D_Details(String z1d_Details) {
//	System.out.println("String ANTES conversão do SET: " + z1d_Details);
//        z1D_Details = Formatters.parseHTML2String(z1d_Details);
//        System.out.println("String DEPOIS conversão do SET: " + z1D_Details);
	 z1D_Details = z1d_Details;
    }
    public String getZ2AF_Act_Attachment_1() {
        return z2AF_Act_Attachment_1;
    }
    public void setZ2AF_Act_Attachment_1(String z2af_Act_Attachment_1) {
        z2AF_Act_Attachment_1 = z2af_Act_Attachment_1;
    }
}
