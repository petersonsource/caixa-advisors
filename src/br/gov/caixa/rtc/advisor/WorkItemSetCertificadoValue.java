package br.gov.caixa.rtc.advisor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.classificacao.ReportableListClassificacao;
import br.gov.caixa.rtc.dto.EnumerationDto;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemSetCertificadoValue extends AbstractRTCService implements IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";
    static String projectAreaUUID;
    static String projectAreaName;
    static String classificacao = null;
    static String apl;
    static IAttribute attr;
    @SuppressWarnings("rawtypes")
    static Identifier literalID;

    public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	IAuditable auditable = saveParameter.getNewState();
	IAuditable oldAuditable = saveParameter.getOldState();
	if (!(auditable instanceof IWorkItem) && !(oldAuditable instanceof IWorkItem)) {
	    return;
	}
	IWorkItem workItem = (IWorkItem) auditable;
	IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
	HttpURLConnection con = null;

	try {
	    IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
	    if (configElements != null) {
		IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
		IWorkItemServer workItemService = getService(IWorkItemServer.class);

		String wiType = workItem.getWorkItemType();

		for (IProcessConfigurationElement configElement : configElements) {
		    // INPUT PARAMETER
		    String configURLCMDB = configElement.getAttribute("urlCMDB"); // URL da fonte de dados.
		    String configWIType = configElement.getAttribute("workItemType"); // Tipo do workitem.
		    String configUnidade = configElement.getAttribute("attrUnidade"); // Unidade Executora.
		    String configClassificacao = configElement.getAttribute("attrClassificacao"); // Classificacao.
		    String configValueOk = configElement.getAttribute("valueClassficacaoOk"); // i.e.: // Valor homologado.
		    String configValueNo = configElement.getAttribute("valueClassficacaoNo"); // i.e.: // Valor não homologado.
		    String configValueWithout = configElement.getAttribute("valueSemClassficacao"); // i.e.: Valor sem homologação.
		    // END INPUT PARAMETER
		    
		    // CHECK
		    if (!wiType.equals(configWIType)) {
			continue;
		    }

		    // verifica se o valor da unidade foi alterado
		    Object idValue = getAttribute(workItem, workItemService, configUnidade, monitor);
		    if (oldWorkItem != null) {
			Object idOldValue = getAttribute(oldWorkItem, workItemService, configUnidade, monitor);
			if (!idValue.equals(idOldValue)) {
			} else {
			    continue;
			}
		    }

		    // recupera o valor label da enumeracao
		    EnumerationDto valueEnum = (EnumerationDto) getFullAttribute(workItem, workItemService, configUnidade, monitor);
		    String strValueEnum = valueEnum.getValue();

		    // FIM Check

		    IProjectAreaHandle ipah = operation.getProcessArea().getProjectArea();
		    IProjectArea ipa = (IProjectArea) repositoryService.fetchItem(ipah, null);
		    projectAreaUUID = ipa.getContextId().getUuidValue();
		    projectAreaName = ipa.getName();

//		    projectAreaUUID = "_vc7WYDjoEeydH4SAM-fA5w";
		    
		    // Gerar iattribute classificacao
		    attr = getAttr(workItemService, workItem, configClassificacao, monitor);

		    // Consulta integracao CMDB

		    URL url = new URL(configURLCMDB + projectAreaUUID);

		    try {
			con = (HttpURLConnection) url.openConnection();
		    } catch (Exception e) {
			IAdvisorInfo info = collector.createProblemInfo("Erro!!!", e.getMessage(), "error");
			collector.addInfo(info);
		    }

		    InputStream stream = con.getInputStream();
		    InputStreamReader isReader = new InputStreamReader(stream, StandardCharsets.ISO_8859_1);

		    int responseCode = con.getResponseCode();
		    if (responseCode != 200) {

			String msg = new String("A consulta aos dados do CMDB não está disponível para armazenar as informações sobre a homologação do sistema.".getBytes(), "UTF-8");
			IAdvisorInfo info = collector.createProblemInfo("Erro!!! ", msg, "error");
			collector.addInfo(info);

		    } else {
			try {
			    JacksonXmlModule module = new JacksonXmlModule();
			    module.setDefaultUseWrapper(false);
			    XmlMapper xmlMapper = new XmlMapper(module);
			    final ReportableListClassificacao projectList = xmlMapper.readValue(isReader, ReportableListClassificacao.class);

			    if (projectList.getProjects() != null) {
				if (!projectList.getProjects().isEmpty()) {
				    for (int i = 0; i < projectList.getProjects().size(); i++) {
					apl = strValueEnum.substring(0, 4) + "APL-" + projectAreaName;
					if (apl.equalsIgnoreCase(projectList.getProjects().get(i).getName())) {
					    classificacao = projectList.getProjects().get(i).getClassificacao().trim();
					    
//					    switch (classificacao.toLowerCase().substring(0, 1)) {
					    switch (classificacao.toLowerCase()) {
					   	
					    case "não homologado":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
					    
					    case "homologado":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "em homologação":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "em certificação":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "certificado":
						literalID = getLiteralEqualsString(configValueOk, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "certificado - hmp":
						literalID = getLiteralEqualsString(configValueOk, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "certificado - tqs":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "em certificação - hmp":
						literalID = getLiteralEqualsString(configValueOk, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "em certificação - tqs":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;
						
					    case "em preparação de certificação":
						literalID = getLiteralEqualsString(configValueNo, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;				
					    
					    case "s":
						literalID = getLiteralEqualsString(configValueWithout, attr, workItemService, monitor);
						workItem.setValue(attr, literalID);
						break;

					    }
					}
				    }
				}
			    }
			    if (classificacao == null) {
				literalID = getLiteralEqualsString(configValueWithout, attr, workItemService, monitor);
				workItem.setValue(attr, literalID);
			    }
			} catch (Exception e) {
			    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
			}

		    }

		}
	    }
	} catch (TeamRepositoryException e) {
	    message(e, collector);
	} catch (Exception e) {
	    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	}
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object getFullAttribute(IWorkItem workItem, IWorkItemServer workItemService, String attributeName, IProgressMonitor monitor) throws TeamRepositoryException {
	EnumerationDto enumAttr = new EnumerationDto();
	IAttribute attribute = workItemService.findAttribute(workItem.getProjectArea(), attributeName, monitor);
	if (attribute == null || workItem.hasAttribute(attribute) == false) {
	    // Attribute not found
	    return null;
	}

	if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
	    Identifier id = (Identifier) workItem.getValue(attribute);

	    IEnumeration enumeration = workItemService.resolveEnumeration(attribute, monitor);
	    List<ILiteral> literals = enumeration.getEnumerationLiterals();
	    for (ILiteral literal : literals) {
		if (literal.getIdentifier2().getStringIdentifier().equals(id.getStringIdentifier())) {
		    enumAttr.setId(literal.getIdentifier2().getStringIdentifier());
		    enumAttr.setValue(literal.getName());
		    break;
		}
	    }
	}
	return enumAttr;
    }

    private static IAttribute getAttr(IWorkItemServer workItemService, IWorkItem workItem, String attr, IProgressMonitor monitor) throws TeamRepositoryException {
	IAttribute attrValue = workItemService.findAttribute(workItem.getProjectArea(), attr, monitor);
	return attrValue;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Identifier getLiteralEqualsString(String name, IAttribute requiredAttribute, IWorkItemServer workItemService, IProgressMonitor monitor) throws TeamRepositoryException {

	Identifier literalID = null;
	IEnumeration enumeration = workItemService.resolveEnumeration(requiredAttribute, monitor);
	List<ILiteral> literals = enumeration.getEnumerationLiterals();
	for (ILiteral literal : literals) {
	    if (literal.getIdentifier2().getStringIdentifier().equals(name)) {
		literalID = literal.getIdentifier2();
		break;
	    }
	}
	return literalID;
    }
}
