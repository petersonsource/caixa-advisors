package br.gov.caixa.rtc.dto;

import com.ibm.team.process.common.IProcessConfigurationElement;

public class ParserWorkItemDto {
	
		public WorkItemDto parser(IProcessConfigurationElement configElement){
			final WorkItemDto dto = new WorkItemDto();
			dto.setType(configElement.getAttribute("workItemType"));
			dto.setState(configElement.getAttribute("stateId"));
			dto.setQualified(configElement.getAttribute("attributeQualified"));
			dto.setQualifiedValue(configElement.getAttribute("attributeValueQualified"));
			dto.setAttribute(configElement.getAttribute("attribute"));
			dto.setAttributeValue(configElement.getAttribute("attributeValue"));
			dto.setParentWIType(configElement.getAttribute("parentWorkitemType"));
			dto.setParentAttr(configElement.getAttribute("parentAttribute"));
			dto.setAttrTemplate(configElement.getAttribute("attrTemplate"));
			dto.setPropagateParentTitle(configElement.getAttribute("propagateParentTitle"));
			dto.setValidateParent(configElement.getAttribute("validateParent"));
			dto.setListaIDEstados(configElement.getAttribute("listaIDEstados"));
			dto.setItensRelacionados(configElement.getAttribute("itensRelacionados"));
			dto.setWiClassId(configElement.getAttribute("wiClassId"));
			dto.setWiClassValue(configElement.getAttribute("wiClassValue"));
			dto.setWiAmbineteId(configElement.getAttribute("wiAmbineteId"));
			dto.setWiAmbineteValue(configElement.getAttribute("wiAmbineteValue"));
			dto.setIntervaloTempo(configElement.getAttribute("intervaloTempo"));
			dto.setIdTipoCentroCusto(configElement.getAttribute("idTipoCentroCusto"));
			dto.setValueTipoCentroCusto(configElement.getAttribute("valueTipoCentroCusto"));
			dto.setItensFilhos(configElement.getAttribute("itensFilhos"));
			dto.setListaAtributosPaiVerificar(configElement.getAttribute("listaAtributosPaiVerificar"));
			dto.setContributor(configElement.getAttribute("contributor"));
			dto.setMailSummary(configElement.getAttribute("mailSummary"));
			dto.setBodyMessage(configElement.getAttribute("bodyMessage"));
			dto.setTipoNotaTrabalhoITSM(configElement.getAttribute("tipoNotaTrabalhoITSM"));
			dto.setIdEstadoLimpar(configElement.getAttribute("idEstadoLimpar"));
			
			
			
			
			//lpf
			dto.setGrupoAlcada(configElement.getAttribute("grupoAlcada"));
			dto.setIdCargo(configElement.getAttribute("idCargo"));
			dto.setIdRole(configElement.getAttribute("idRole"));
			return dto;
		}

		
		
}
