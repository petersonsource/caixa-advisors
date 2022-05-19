package br.gov.caixa.rtc.integracao.entidades;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties({ "_links", "idIntermoIncidente"})
public class Values {

    private String idIntermoIncidente;
//    private String status;
//    private String statusReason;
    private String z1D_Action = "MODIFY";
    private String z1D_Secure_Log = "Yes";
    private String z1D_View_Access = "Public";
    private String z1D_CommunicationSource = "Other";
    private String z1D_Activity_Type = "General Information";
    private String z1D_WorklogDetails = null;
    private String z1D_Details = null;
    

	@JsonCreator
	public Values(/**@JsonProperty("Status") String status, @JsonProperty("Status_Reason") String statusReason,*/
		@JsonProperty("z1D Action") String z1D_Action, @JsonProperty("z1D_Secure_Log") String z1D_Secure_Log,
		@JsonProperty("z1D_View_Access") String z1D_View_Access, @JsonProperty("z1D_CommunicationSource") String z1D_CommunicationSource,
		@JsonProperty("z1D_Activity_Type") String z1D_Activity_Type, @JsonProperty("z1D_Details") String z1D_Details,
		@JsonProperty("z1D_WorklogDetails") String z1D_WorklogDetails, String idIntermoIncidente) {
	    
	    this.idIntermoIncidente = idIntermoIncidente;
//	    this.status = status;
//	    this.statusReason = statusReason;
	    this.z1D_Action = z1D_Action;
	    this.z1D_Secure_Log = z1D_Secure_Log;
	    this.z1D_View_Access = z1D_View_Access;
	    this.z1D_CommunicationSource = z1D_CommunicationSource;
	    this.z1D_Activity_Type = z1D_Activity_Type;
	    this.z1D_WorklogDetails = z1D_WorklogDetails;
	    this.z1D_Details = z1D_Details;
		
	}


	public String getIdIntermoIncidente() {
	    return idIntermoIncidente;
	}
	public void setIdIntermoIncidente(String idIntermoIncidente) {
	    this.idIntermoIncidente = idIntermoIncidente;
	}
//	public String getStatus() {
//	    return status;
//	}
//	public void setStatus(String status) {
//	    this.status = status;
//	}
//	public String getStatusReason() {
//	    return statusReason;
//	}
//	public void setStatusReason(String statusReason) {
//	    this.statusReason = statusReason;
//	}
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
	public String getZ1D_WorklogDetails() {
	    return z1D_WorklogDetails;
	}
	public void setZ1D_WorklogDetails(String z1d_WorklogDetails) {
	    z1D_WorklogDetails = z1d_WorklogDetails;
	}
	public String getZ1D_Details() {
	    return z1D_Details;
	}
	public void setZ1D_Details(String z1d_Details) {
	    z1D_Details = z1d_Details;
	}


	@Override
	public String toString() {
	    return "Values [idIntermoIncidente=" + idIntermoIncidente +/* ", status=" + status + ", statusReason=" + statusReason +*/ ", z1D_Action=" + z1D_Action + ", z1D_Secure_Log=" + z1D_Secure_Log + ", z1D_View_Access=" + z1D_View_Access + ", z1D_CommunicationSource=" + z1D_CommunicationSource + ", z1D_Activity_Type=" + z1D_Activity_Type + ", z1D_WorklogDetails=" + z1D_WorklogDetails + ", z1D_Details=" + z1D_Details + "]";
	}


	
}
