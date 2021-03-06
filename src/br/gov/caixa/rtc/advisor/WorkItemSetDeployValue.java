package br.gov.caixa.rtc.advisor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.deploy.ReportableListDeploy;
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

public class WorkItemSetDeployValue extends AbstractRTCService implements
	IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";
    static String projectAreaUUID;
    static String projectAreaName;
    static String tipoDeploy = null;
    static String dpl;
    static IAttribute attr;
    @SuppressWarnings("rawtypes")
    static Identifier literalID;

    @Override
    public void run(AdvisableOperation operation,
	    IProcessConfigurationElement advisorConfiguration,
	    IAdvisorInfoCollector collector, IProgressMonitor monitor)
	    throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	IAuditable auditable = saveParameter.getNewState();
	IAuditable oldAuditable = saveParameter.getOldState();
	if (!(auditable instanceof IWorkItem)
		&& !(oldAuditable instanceof IWorkItem)) {
	    return;
	}
	IWorkItem workItem = (IWorkItem) auditable;
	IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
	HttpURLConnection con = null;

	String oldStateId = "";
	if (oldWorkItem != null) {
	    oldStateId = oldWorkItem.getState2().getStringIdentifier();
	}
	String newStateId = workItem.getState2().getStringIdentifier();

	if (oldStateId.equals(newStateId)) {
	    return;
	}
	try {
	    IProcessConfigurationElement[] configElements = advisorConfiguration
		    .getChildren();
	    if (configElements != null) {
		IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
		IWorkItemServer workItemService = getService(IWorkItemServer.class);

		String wiType = workItem.getWorkItemType();

		for (IProcessConfigurationElement configElement : configElements) {
		    String configURLCMDB = configElement
			    .getAttribute("urlCMDB");
		    String configWIType = configElement
			    .getAttribute("workItemType");
		    String configUnidade = configElement
			    .getAttribute("attrUnidade");
		    String configDeploy = configElement
			    .getAttribute("attrDeploy");
		    String configValueMan = configElement
			    .getAttribute("valueDeployMan");
		    String configValueAut = configElement
			    .getAttribute("valueDeployAut");
		    String configValueWithout = configElement
			    .getAttribute("valueSemClassficacao");
		    String configTriggerAttr = configElement
			    .getAttribute("triggerAttr");
		    String configTriggerValue = configElement
			    .getAttribute("triggerValue");
		    String configStateId = configElement.getAttribute("state");

		    if (!wiType.equals(configWIType)) {
			continue;
		    }

		    if (!configStateId.equalsIgnoreCase(workItem.getState2()
			    .getStringIdentifier())) {
			configStateId = configStateId.replace("s", "");
			if (!configStateId.equalsIgnoreCase(workItem
				.getState2().getStringIdentifier())) {
			    continue;
			}
		    }

		    Object triggerValue = getAttribute(workItem,
			    workItemService, configTriggerAttr, monitor);
		    if (!triggerValue.equals(configTriggerValue)) {
			continue;
		    }

		    EnumerationDto valueEnum = (EnumerationDto) getFullAttribute(
			    workItem, workItemService, configUnidade, monitor);
		    String strValueEnum = valueEnum.getValue();

		    IProjectAreaHandle ipah = operation.getProcessArea()
			    .getProjectArea();
		    IProjectArea ipa = (IProjectArea) repositoryService
			    .fetchItem(ipah, null);
		    projectAreaUUID = ipa.getContextId().getUuidValue();
		    projectAreaName = ipa.getName();

		    attr = getAttr(workItemService, workItem, configDeploy,
			    monitor);

		    URL url = new URL(configURLCMDB + projectAreaUUID);

		    try {
			con = (HttpURLConnection) url.openConnection();
		    } catch (Exception e) {
			IAdvisorInfo info = collector.createProblemInfo(
				"Erro!!!", e.getMessage(), "error");
			collector.addInfo(info);
		    }

		    InputStream stream = con.getInputStream();
		    InputStreamReader isReader = new InputStreamReader(stream);

		    int responseCode = con.getResponseCode();
		    if (responseCode != 200) {

			String msg = new String(
				"A consulta aos dados do CMDB n??o est?? dispon??vel para armazenar as informa????es sobre a tipo de deploy do sistema."
					.getBytes(), "UTF-8");
			IAdvisorInfo info = collector.createProblemInfo(
				"Erro!!! ", msg, "error");
			collector.addInfo(info);

		    } else {
			try {
			    JacksonXmlModule module = new JacksonXmlModule();
			    module.setDefaultUseWrapper(false);
			    XmlMapper xmlMapper = new XmlMapper(module);
			    final ReportableListDeploy projectList = xmlMapper
				    .readValue(isReader,
					    ReportableListDeploy.class);

			    if (projectList.getProjects() != null) {
				if (!projectList.getProjects().isEmpty()) {
				    for (int i = 0; i < projectList
					    .getProjects().size(); i++) {
					dpl = strValueEnum.substring(0, 4)
						+ "APL-" + projectAreaName;
					if (dpl.equalsIgnoreCase(projectList
						.getProjects().get(i).getName())) {
					    tipoDeploy = projectList
						    .getProjects().get(i)
						    .getTipoDeploy();

					    switch (tipoDeploy.toLowerCase()
						    .substring(0, 1)) {

					    // Manual
					    case "m":
						literalID = getLiteralEqualsString(
							configValueMan, attr,
							workItemService,
							monitor);
						workItem.setValue(attr,
							literalID);
						break;
					    // automatizado
					    case "a":
						literalID = getLiteralEqualsString(
							configValueAut, attr,
							workItemService,
							monitor);
						workItem.setValue(attr,
							literalID);
						break;
					    }
					}
				    }
				}
			    }
			    if (tipoDeploy == null) {
				literalID = getLiteralEqualsString(
					configValueWithout, attr,
					workItemService, monitor);
				workItem.setValue(attr, literalID);
			    }
			} catch (Exception e) {
			    message(e.getMessage(), ERROR_MESSAGE_TYPE,
				    collector);
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
    private static Object getFullAttribute(IWorkItem workItem,
	    IWorkItemServer workItemService, String attributeName,
	    IProgressMonitor monitor) throws TeamRepositoryException {
	EnumerationDto enumAttr = new EnumerationDto();
	IAttribute attribute = workItemService.findAttribute(
		workItem.getProjectArea(), attributeName, monitor);
	if (attribute == null || workItem.hasAttribute(attribute) == false) {
	    // Attribute not found
	    return null;
	}

	if (AttributeTypes.isEnumerationAttributeType(attribute
		.getAttributeType())) {
	    Identifier id = (Identifier) workItem.getValue(attribute);

	    IEnumeration enumeration = workItemService.resolveEnumeration(
		    attribute, monitor);
	    List<ILiteral> literals = enumeration.getEnumerationLiterals();
	    for (ILiteral literal : literals) {
		if (literal.getIdentifier2().getStringIdentifier()
			.equals(id.getStringIdentifier())) {
		    enumAttr.setId(literal.getIdentifier2()
			    .getStringIdentifier());
		    enumAttr.setValue(literal.getName());
		    break;
		}
	    }
	}
	return enumAttr;
    }

    private static IAttribute getAttr(IWorkItemServer workItemService,
	    IWorkItem workItem, String attr, IProgressMonitor monitor)
	    throws TeamRepositoryException {
	IAttribute attrValue = workItemService.findAttribute(
		workItem.getProjectArea(), attr, monitor);
	return attrValue;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Identifier getLiteralEqualsString(String name,
	    IAttribute requiredAttribute, IWorkItemServer workItemService,
	    IProgressMonitor monitor) throws TeamRepositoryException {

	Identifier literalID = null;
	IEnumeration enumeration = workItemService.resolveEnumeration(
		requiredAttribute, monitor);
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
