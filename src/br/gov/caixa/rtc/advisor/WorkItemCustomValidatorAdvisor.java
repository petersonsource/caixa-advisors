/*******************************************************************************
 * Licensed Materials - Property of IBM (c) Copyright IBM Corporation 2005-2006.
 * All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights: Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ******************************************************************************/
package br.gov.caixa.rtc.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.util.RTCUtil;
import br.gov.caixa.rtc.advisor.validator.ConfigValidator;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.internal.links.Reference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.IProcessReport;
import com.ibm.team.process.common.advice.IReportInfo;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.internal.IWorkItemTemplateService;
import com.ibm.team.workitem.common.internal.template.AttributeVariable;
import com.ibm.team.workitem.common.internal.template.WorkItemTemplateSerializable;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.template.IAttributeVariable;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemCustomValidatorAdvisor extends AbstractService implements IOperationAdvisor {

	/**
		<precondition id="rtc.advisors.WorkItemCustomValidatorAdvisor" name="Custom Validator">
			<config workItemType="task" stateId="com.ibm.team.workitem.workflow.ss.state.s1" targetStateId="Fluxo_Trabalho_Requisicao_Mudanca.action.a2" templateName="opcional" propagateParentTitle="false">
			    <condition attrId="aprovacao_de_qualidade" value="Aprovação.literal.l2"/>
			    <condition attrId="aprovacao_de_sistemas" value="Aprovação.literal.l2"/>	
			    <condition attrId="aprovacao_de_dados" value="Aprovação.literal.l2"/>
			    <changeAttr attrId="aprovacao_de_dados" value="Aprovação.literal.l2"/>		
			</config>
		</precondition>
	 */
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor)	throws TeamRepositoryException {
		Object data = operation.getOperationData();
		
		if (!(data instanceof ISaveParameter)) {    		
			return;
		}
        
		ISaveParameter saveParameter = (ISaveParameter) data;
		IAuditable auditable = saveParameter.getNewState();
		if (!(auditable instanceof IWorkItem)) {
			return;
		}
		
		try {
			IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
			if (configElements != null) {				
				IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
				IRepositoryItemService repository = getService(IRepositoryItemService.class);
				IWorkItemServer workItemService = getService(IWorkItemServer.class);
				
				IWorkItem workItem = (IWorkItem) auditable;
				IWorkItem oldWorkItem = (IWorkItem) saveParameter.getOldState();
				
				for (IProcessConfigurationElement configElement : configElements) {									
					//check params
					if (ConfigValidator.isMissingConfigParams(configElement, collector)) {
						return;
					}
					if (!workItem.getWorkItemType().equals(configElement.getAttribute("workItemType"))) { 
						continue;
					}
					//if (oldWorkItem != null && workItem.getState2().getStringIdentifier().equals(oldWorkItem.getState2().getStringIdentifier())) { //nao mudou de estado
					//	continue;
					//}

					if (!workItem.getState2().getStringIdentifier().equals(configElement.getAttribute("stateId"))) { //o estado eh outro, pula a validacao 
						continue;
					}
					
					String templateName = configElement.getAttribute("templateName");
					
					boolean isConditionChanged = false;
					boolean conditionFail = false;
					for (IProcessConfigurationElement conditionElement : configElement.getChildren()) {
						String attrId = conditionElement.getAttribute("attrId");
						String value = conditionElement.getAttribute("value");

						IAttribute attribute = workItemService.findAttribute(workItem.getProjectArea(), attrId, monitor);
						boolean isExpectedValue = isExpectedValue(workItemService, workItem, attribute, value);
						
						if (!isExpectedValue && conditionElement.getName().equals("condition")) { //-- o valor não é o esperado
							conditionFail = true;
							break;
						} else if (conditionElement.getName().equals("changeAttr")) { //-- pega o atributo que tem que ser alterado
							changeValue(workItemService, oldWorkItem, attribute, value);
						}
					
						//-- valida se teve mudancas nos campos condition. Se não teve, aborta o processo
						if (conditionElement.getName().equals("condition") && isValueChanged(workItem, oldWorkItem, attribute)) {
							isConditionChanged = true;
						}
					}
					
					//Não teve alteração nenhuma, vai para o próximo config do validator
					if (!isConditionChanged) {
						continue;
					}
					
					if (!conditionFail) {
						if (templateName != null && !templateName.equals("")) {
							instanciateTemplate(auditableCommon, workItemService, saveParameter, configElement, workItem, templateName, collector, monitor);
						}
						
						//altera o estado
						if (configElement.getAttribute("targetStateId") != null && !configElement.getAttribute("targetStateId").equals("")) {
							IWorkflowInfo workflowInfo = workItemService.findWorkflowInfo(workItem, monitor);
							Identifier<IState> stateIdSelected = null;
							for (Identifier<IState> stateId : workflowInfo.getAllStateIds()) {
								if (stateId.getStringIdentifier().equals(configElement.getAttribute("targetStateId"))) {
									stateIdSelected = stateId;
								}
							}
							if (stateIdSelected != null) {
								workItem.setState2(stateIdSelected);
							}
						}
						
					/*	//************* Altera o estado do filho ***********************
						
						//Obtem os filhos
						IWorkItemReferences childReferences = workItemService.resolveWorkItemReferences(workItem, monitor);
				        List<IReference> childReferencesList = childReferences.getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
						
				        
				        //Valida o tipo do filho e finalmente altera o estado quando o tipo for o correto.
						IWorkItem childWorkItem = null;
						String configChildWorkItemType = configElement.getAttribute("childWorkItemType");
						String configChildStateId = configElement.getAttribute("childTargetStatedId");
						boolean childFound = false;
						
						if (childReferencesList != null && childReferencesList.size() > 0)	{
							for (IReference childReference : childReferencesList) {
								// Validar o tipo
								IWorkItemHandle childWorkItemHandle = (IWorkItemHandle)childReference.resolve();
								childWorkItem = (IWorkItem)repository.fetchItem(childWorkItemHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
									
								
								if (childWorkItem != null)	{
									String childWorkItemType = childWorkItem.getWorkItemType();
									// Se não for do tipo configurado, não deve continuar
									if (!childWorkItemType.equals(configChildWorkItemType)) {
										break;
									}
									
									//Seleciona o id do estado baseado no string da configuração
									if (configChildStateId != null && !configChildStateId.equals("")) {
										IWorkflowInfo childWorkflowInfo = workItemService.findWorkflowInfo(childWorkItem, monitor);
										Identifier<IState> childStateIdSelected = null;
										for (Identifier<IState> stateId : childWorkflowInfo.getAllStateIds()) {
											if (stateId.getStringIdentifier().equals(configChildStateId)) {
												childStateIdSelected = stateId;
											}
										}
										if (childStateIdSelected != null) {
											//Altera estado do item de trabalho filho
											childWorkItem.setState2(childStateIdSelected);
											workItemService.saveWorkItem2(childWorkItem, childReferences, null);
										} else	{
											//Estado configurado não existe.
								        	IAdvisorInfo info = collector.createProblemInfo("Configuração Incorreta", "Estado final do item de trabalho filho não existe.", "error");
								        	collector.addInfo(info);
								        	return;
										}
									} else {
										//Estado target do item filho não está configurado.
							        	IAdvisorInfo info = collector.createProblemInfo("Configuração Incorreta", "Não foi possível encontrar o estado final do item de trabalho filho.", "error");
							        	collector.addInfo(info);
							        	return;
									}
								}
								
							}
							
						} */
					}
				}
				
			}			
		} catch (TeamRepositoryException e) {
			IAdvisorInfo info = collector.createProblemInfo("Erro inesperado", e.getMessage(), "error");
			collector.addInfo(info);
			throw new TeamRepositoryException(e);
		}
	}
	
	private boolean isExpectedValue(IWorkItemServer workItemService, IWorkItem workItem, IAttribute attribute, String expectedValue) throws TeamRepositoryException {
		Object value = workItem.getValue(attribute);
		
		boolean matched = false;
		if (AttributeTypes.STRING_TYPES.contains(attribute.getAttributeType()) && value instanceof String) {
			if (value != null && value.toString().trim().equals(expectedValue)) return true;
		} else if (attribute.getAttributeType().equals(AttributeTypes.BOOLEAN) && value instanceof Boolean) {
			String strValue = value.toString();
			if (strValue != null && strValue.trim().equals(expectedValue)) return true;
		} else if (AttributeTypes.HTML_TYPES.contains(attribute.getAttributeType()) && value instanceof String) {
			String strValue = XMLString.createFromXMLText(value.toString()).getPlainText();
			if (strValue != null && strValue.trim().equals(expectedValue)) return true;
		} else if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			Identifier itemValue = (Identifier)value;
			if (itemValue != null && itemValue.getStringIdentifier().equals(expectedValue)) return true;
		} else if (AttributeTypes.isEnumerationListAttributeType(attribute.getAttributeType())) {
			List<Identifier> valueList = (List<Identifier>) value;
			for (Identifier item : valueList) {
				String itemValue = item.getStringIdentifier();
				if (itemValue != null && itemValue.toString().trim().equals(expectedValue)) return true;
			}
		}
		
		return matched;
	}
	
	private boolean isValueChanged(IWorkItem workItem, IWorkItem oldWorkItem, IAttribute attribute) throws TeamRepositoryException {
		if (oldWorkItem == null) {
			return true;
		}
		
		Object value = workItem.getValue(attribute);
		Object oldValue = oldWorkItem.getValue(attribute);
		
		boolean changed = false;
		if (AttributeTypes.STRING_TYPES.contains(attribute.getAttributeType()) && value instanceof String) {
			if (value != null && oldValue == null) {
				return true;
			} else if (value != null && !value.toString().trim().equals(oldValue.toString().trim())) {
				return true;
			}
		} else if (attribute.getAttributeType().equals(AttributeTypes.BOOLEAN) && value instanceof Boolean) {
			String strValue = value.toString();
			String oldStrValue = oldValue.toString();
			
			if (strValue != null && oldValue == null) {
				return true;
			} else if (strValue != null && !strValue.toString().trim().equals(oldStrValue.toString().trim())) {
				return true;
			}	
		} else if (AttributeTypes.HTML_TYPES.contains(attribute.getAttributeType()) && value instanceof String) {
			String strValue = XMLString.createFromXMLText(value.toString()).getPlainText();
			String oldStrValue = XMLString.createFromXMLText(oldValue.toString()).getPlainText();
			
			if (strValue != null && oldValue == null) {
				return true;
			} else if (strValue != null && !strValue.toString().trim().equals(oldStrValue.toString().trim())) {
				return true;
			}
		} else if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			Identifier itemValue = (Identifier)value;
			Identifier oldItemValue = (Identifier)oldValue;
			
			if (itemValue != null && oldItemValue == null) {
				return true;
			} else if (itemValue != null && !itemValue.getStringIdentifier().equals(oldItemValue.getStringIdentifier())) {
				return true;
			}
		} else if (AttributeTypes.isEnumerationListAttributeType(attribute.getAttributeType())) {
			List<Identifier> valueList = (List<Identifier>) value;
			List<Identifier> oldValueList = (List<Identifier>) oldValue;
			
			if (valueList.size() != oldValueList.size()) {
				return true;
			}
			
			for (Identifier item : valueList) {
				String itemValue = item.getStringIdentifier();
				
				boolean found = false;
				for (Identifier oldItem : oldValueList) {
					if (itemValue.equals(oldItem.getStringIdentifier())) {
						found = true;
						break;
					}
				}
				
				if (!found) {return true;}
			}
		}
		
		return changed;
	}
	
	private boolean changeValue(IWorkItemServer workItemService, IWorkItem workItem, IAttribute attribute, String value) throws TeamRepositoryException {
		if (attribute == null && !workItem.hasAttribute(attribute)) {
			return false;
		}
		
		if (AttributeTypes.STRING_TYPES.contains(attribute.getAttributeType())) {
			workItem.setValue(attribute, value);
		} else if (attribute.getAttributeType().equals(AttributeTypes.INTEGER)) {	
			workItem.setValue(attribute, Integer.valueOf(value));
		} else if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			IEnumeration enumeration = RTCUtil.resolveEnumerations(workItemService, attribute, null);
			List<ILiteral> literals = enumeration.getEnumerationLiterals();
			for (ILiteral literal : literals) {
				if (literal.getIdentifier2().getStringIdentifier().equals(value)) {
					workItem.setValue(attribute, literal.getIdentifier2());
					break;
				}
			}
		}
		
		return true;
	}
	
	private void instanciateTemplate(IAuditableCommon auditableCommon, IWorkItemServer workItemService, ISaveParameter saveParameter, IProcessConfigurationElement configElement, IWorkItem workItem, String templateName, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		IWorkItemTemplateService templateService = getService(IWorkItemTemplateService.class);
		
		//template variables
        Map<IAttributeVariable,Object> workItemTemplateVariables = new HashMap<IAttributeVariable,Object>();
        IAttribute attrPlannedFor = workItemService.findAttribute(workItem.getProjectArea(), IWorkItem.TARGET_PROPERTY, monitor);
        IAttributeVariable varPlannedFor = (IAttributeVariable)new AttributeVariable(attrPlannedFor);			        			        
        
        workItemTemplateVariables.put(varPlannedFor, workItem.getTarget());
		
        //find template
        String templateId = null;
        Object[] templates = templateService.getAvailableTemplates2(workItem.getProjectArea());
        int i=0;
        for (Object obj : templates) {
        	if (obj != null && obj instanceof String) {
	        	String t = (String)obj;
	        	
	        	if (t.equals(templateName)) {
	        		templateId = (String)templates[i-1];
	        		break;
	        	}
        	}
        	i++;
        }
        
        if (templateId == null) {
        	IAdvisorInfo info = collector.createProblemInfo("Template não encontrado", "Não foi possível encontrar o template "+templateName, "error");
        	collector.addInfo(info);
        	return;
        }				        
        
        int[] arrayWi = templateService.instantiateTemplate(templateId, WorkItemTemplateSerializable.serializeVariableAndParameterValues(workItemTemplateVariables, null), workItem.getProjectArea());      
        
        if (arrayWi == null || arrayWi.length == 0) {
        	IAdvisorInfo info = collector.createProblemInfo("Não foi possível criar os itens de trabalho", "Não foi possível criar os itens de trabalho a partir do template", "error");
        	collector.addInfo(info);
        	return;
        }
        
        List<Integer> listWi = new ArrayList<Integer>();
        for (int wiId : arrayWi) {
        	listWi.add(wiId);
        }			       
        
        //get wi created
        List<IWorkItemHandle> wisCreated = workItemService.findWorkItemsById(listWi, monitor);
        
        //create a link
		IWorkItemReferences refs = saveParameter.getNewReferences();								
		for (IWorkItemHandle wiCreated : wisCreated) {
			Reference reference = (Reference)IReferenceFactory.INSTANCE.createReferenceToItem(wiCreated);
			refs.add(WorkItemEndPoints.CHILD_WORK_ITEMS, reference); 					
		}
		
		String propagateParentTitle = configElement.getAttribute("propagateParentTitle");
		String childName = configElement.getAttribute("childName");
		
		//propagate the parent title
		for (IWorkItemHandle wiCreatedHandle : wisCreated) {
			IWorkItem wiCreated = (IWorkItem)auditableCommon.resolveAuditable(wiCreatedHandle, IWorkItem.FULL_PROFILE, monitor);
			XMLString xmlSummary = wiCreated.getHTMLSummary();
			String summary = xmlSummary.getPlainText();
			XMLString xmlParentSummary = workItem.getHTMLSummary();
			String parentSummary = xmlParentSummary.getPlainText();
			
			StringBuffer newSummary = new StringBuffer();
			if (propagateParentTitle != null && propagateParentTitle.equals("true")) {
				newSummary.append(summary).append(" - ").append(childName).append(" (").append(parentSummary).append("_").append(workItem.getId()).append(")");
			} else {
				newSummary.append(summary).append(" ").append(workItem.getId()).append(" - ").append(childName);
			}
			
			IWorkItem wiCreatedCopy = (IWorkItem)wiCreated.getWorkingCopy();
			wiCreatedCopy.setHTMLSummary(XMLString.createFromPlainText(newSummary.toString()));							
			IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(wiCreatedCopy, null);
			
			try {
				workItemService.saveWorkItem2(wiCreatedCopy, copyReferences, null);	
			} catch (Exception e) {
				IReportInfo info = collector.createProblemInfo("Problema ao salvar workitem", e.getMessage(), "info"); //$NON-NLS-1$
				info.setSeverity(IProcessReport.OK);
				collector.addInfo(info);
			}
		}
		
	}	
	
}
