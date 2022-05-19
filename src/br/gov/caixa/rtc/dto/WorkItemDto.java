package br.gov.caixa.rtc.dto;

import java.util.Arrays;
import java.util.List;

public class WorkItemDto {
	private String type;

	private String state;

	private String attrTemplate;

	private boolean propagateParentTitle;

	private boolean validateParent;

	private List<String> listaIDEstados;

	private List<String> itensRelacionados;

	private List<String> itensFilhos;

	private List<String> listaAtributosPaiVerificar;
	
	private String tipoNotaTrabalhoITSM;

	// lpf
	private String oldState;

	private String attribute;

	private String attributeValue;
	// fim lpf
	private String qualified;

	private String qualifiedValue;

	private String parentWIType;

	private String parentAttr;

	private String wiClassId;

	private String wiClassValue;

	private String wiAmbineteId;

	private String wiAmbineteValue;

	private String idTipoCentroCusto;

	private String valueTipoCentroCusto;
	
	private String idEstadoLimpar;
	
		// lpf
	private String grupoAlcada;

	private String idCargo;

	private String idRole;

	private int intervaloTempo;

	private String mailSummary;

	private String bodyMessage;

	private String contributor;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAttrTemplate() {
		return attrTemplate;
	}

	public void setAttrTemplate(String attrTemplate) {
		this.attrTemplate = attrTemplate;
	}

	public boolean getPropagateParentTitle() {
		return propagateParentTitle;
	}

	public void setPropagateParentTitle(String propagateParentTitle) {
		if (propagateParentTitle == null
				|| propagateParentTitle.equalsIgnoreCase("false")) {
			this.propagateParentTitle = false;
		} else {
			this.propagateParentTitle = true;
		}
	}

	public String getQualified() {
		return qualified;
	}

	public void setQualified(String qualified) {
		this.qualified = qualified;
	}

	public String getQualifiedValue() {
		return qualifiedValue;
	}

	public void setQualifiedValue(String qualifiedValue) {
		this.qualifiedValue = qualifiedValue;
	}

	public String getParentWIType() {
		return parentWIType;
	}

	public void setParentWIType(String parentWIType) {
		this.parentWIType = parentWIType;
	}

	public String getParentAttr() {
		return parentAttr;
	}

	public void setParentAttr(String parentAttr) {
		this.parentAttr = parentAttr;
	}

	public String getWiClassId() {
		return wiClassId;
	}

	public void setWiClassId(String wiClassId) {
		this.wiClassId = wiClassId;
	}

	public String getWiClassValue() {
		return wiClassValue;
	}

	public void setWiClassValue(String wiClassValue) {
		this.wiClassValue = wiClassValue;
	}

	public String getWiAmbineteId() {
		return wiAmbineteId;
	}

	public void setWiAmbineteId(String wiAmbineteId) {
		this.wiAmbineteId = wiAmbineteId;
	}

	public String getWiAmbineteValue() {
		return wiAmbineteValue;
	}

	public void setWiAmbineteValue(String wiAmbineteValue) {
		this.wiAmbineteValue = wiAmbineteValue;
	}

	// lpf
	public String getGrupoAlcada() {
		return grupoAlcada;
	}

	public void setGrupoAlcada(String grupoAlcada) {
		this.grupoAlcada = grupoAlcada;
	}

	// lpf
	public String getIdCargo() {
		return idCargo;
	}

	public void setIdCargo(String idCargo) {
		this.idCargo = idCargo;
	}

	public String getIdRole() {
		return idRole;
	}

	public void setIdRole(String idRole) {
		this.idRole = idRole;
	}

	// lpf
	public String getOldState() {
		return oldState;
	}

	public void setOldState(String oldState) {
		this.oldState = oldState;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getAttributeValue() {
		return attributeValue;
	}

	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}

	public boolean isValidateParent() {
		return validateParent;
	}

	public void setValidateParent(String validateParent) {
		if (validateParent == null || validateParent.equalsIgnoreCase("false")) {
			this.validateParent = false;
		} else {
			this.validateParent = true;
		}

	}

	public List<String> getListaIDEstados() {
		return listaIDEstados;
	}

	public void setListaIDEstados(String listaEstados) {
		if (listaEstados != null) {
			this.listaIDEstados = Arrays.asList(listaEstados.toLowerCase().split(";"));
			return;
		}
		this.listaIDEstados = null;
	}

	public List<String> getItensRelacionados() {
		return itensRelacionados;
	}

	public void setItensRelacionados(String itensRelacionados) {
		if (itensRelacionados != null) {
			this.itensRelacionados = Arrays
					.asList(itensRelacionados.split(";"));
			return;
		}
		this.itensRelacionados = null;
	}

	public List<String> getItensFilhos() {
		return itensFilhos;
	}

	public void setItensFilhos(String itensFilhos) {
		if (itensFilhos != null) {
			this.itensFilhos = Arrays.asList(itensFilhos.split(";"));
			return;
		}
		this.itensFilhos = null;
	}

	public int getIntervaloTempo() {
		return intervaloTempo;
	}

	public void setIntervaloTempo(String intervaloTempo) {
		if (intervaloTempo != null) {
			this.intervaloTempo = Integer.parseInt(intervaloTempo);
			return;
		}
		this.intervaloTempo = 0;
	}

	public String getIdTipoCentroCusto() {
		return idTipoCentroCusto;
	}

	public void setIdTipoCentroCusto(String idTipoCentroCusto) {
		this.idTipoCentroCusto = idTipoCentroCusto;
	}

	public String getValueTipoCentroCusto() {
		return valueTipoCentroCusto;
	}

	public void setValueTipoCentroCusto(String valueTipoCentroCusto) {
		this.valueTipoCentroCusto = valueTipoCentroCusto;
	}

	public List<String> getListaAtributosPaiVerificar() {
		return listaAtributosPaiVerificar;
	}

	public void setListaAtributosPaiVerificar(String aListaAtributosPai) {
		if (aListaAtributosPai != null) {
			this.listaAtributosPaiVerificar = Arrays.asList(aListaAtributosPai
					.split(";"));
			return;
		}
		this.listaAtributosPaiVerificar = null;
	}

	public String getMailSummary() {
		return mailSummary;
	}

	public void setMailSummary(String mailSummary) {
		this.mailSummary = mailSummary;
	}

	public String getBodyMessage() {
		return bodyMessage;
	}

	public void setBodyMessage(String bodyMessage) {
		this.bodyMessage = bodyMessage;
	}

	public String getContributor() {
		return contributor;
	}

	public void setContributor(String contributor) {
		this.contributor = contributor;
	}
	
	public String getTipoNotaTrabalhoITSM() {
	    return tipoNotaTrabalhoITSM;
	}

	public void setTipoNotaTrabalhoITSM(String tipoNotaTrabalhoITSM) {
	    this.tipoNotaTrabalhoITSM = tipoNotaTrabalhoITSM;
	}

	public String getIdEstadoLimpar() {
	    return idEstadoLimpar;
	}

	public void setIdEstadoLimpar(String idEstadoLimpar) {
	    this.idEstadoLimpar = idEstadoLimpar;
	}

}
