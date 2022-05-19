package br.gov.caixa.rtc.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.internal.links.Reference;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.IProcessReport;
import com.ibm.team.process.common.advice.IReportInfo;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
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
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.template.IAttributeVariable;
import com.ibm.team.workitem.service.IWorkItemServer;

/*
 * <precondition id="rtc.advisors.CreateWorkItemFromTemplate" name="Create WorkItems From Template">
 * 		<config workItemType="com.ibm.team.workitem.workItemType.ss" stateId="com.ibm.team.workitem.workflow.ss.state.s1" attrTemplate="fluxo" attrTemplate2="fluxo2" attributeChangeTemplate="tipo_demanda_negocio" attributeChangeTemplateValue="tipo_demanda_negocio.literal.l4" propagateParentTitle="true"/>
 * </precondition>
*/

public class CreateWorkItemFromTemplate extends AbstractService implements IOperationAdvisor {
    
    public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration,
        IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
        
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
		        IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
		        IProcessServerService processService = getService(IProcessServerService.class);
		        IWorkItemTemplateService templateService = getService(IWorkItemTemplateService.class);
		        IWorkItemServer workItemService = getService(IWorkItemServer.class);
		        IRepositoryItemService repository = getService(IRepositoryItemService.class);
		        IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
				
		        IWorkItem workItem = (IWorkItem) auditable;
		        IWorkItem workItemOld = (IWorkItem) saveParameter.getOldState();
		        String wiType = workItem.getWorkItemType();
				
				for (IProcessConfigurationElement configElement : configElements) {
					String configWIType = configElement.getAttribute("workItemType"); // i.e.: Demanda de Manuntenção
					String configStateId = configElement.getAttribute("stateId");		// i.e.: com.ibm.team.workitem.workflow.ss.state.s1
					String configAttrTemplateName = configElement.getAttribute("attrTemplate"); // i.e.: Template Demanda de Negócio Média/Grande
					String configPropagateParentId = configElement.getAttribute("propagateParentTitle"); // i.e.: true
					String configChangeTemplate = configElement.getAttribute("attributeChangeTemplate"); // i.e.: atributo que verifica se a demanda é pequena ou não (demanda_pequena)
					String configChangeTemplateValue = configElement.getAttribute("attributeChangeTemplateValue"); // i.e.: Valor do atributo que identifica a demanda pequena.
					String configAttrTemplateName2 = configElement.getAttribute("attrTemplate2"); // i.e.: Template Demanda de Negócio Pequena
					String templateSelected = configAttrTemplateName;
					
					
					//check params
					
					if (!wiType.equals(configWIType)) {
						continue;
					}
					
					if (!configStateId.equals(workItem.getState2().getStringIdentifier())) {
						continue;
					}
					
					if ((workItemOld != null && workItemOld.getState2().getStringIdentifier().equals(workItem.getState2().getStringIdentifier()))) {
						continue;
					}
					
					// Determinar qual template deve ser utilizado.
					
			        IWorkItemReferences references = saveParameter.getNewReferences();
			        //IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItem, monitor);
			        List<IReference> parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
			        
			        IWorkItem workItemParent = null;
			        
			        if (parentReferences != null && parentReferences.size() > 0)	{ // Criar Itens de Trabalho filhos se há um item de trabalho pai. Caso contrário, não deve ser criado.
			        	
			        	if (configChangeTemplate != null || configChangeTemplateValue != null || configAttrTemplateName2 != null)	{ //Caso esteja configurado, seleciona o template a ser utilizado para a criação dos itens de trabalho.
				        	for (IReference parentReference : parentReferences)	{
				        		IWorkItemHandle wiParentHandle = (IWorkItemHandle)parentReference.resolve();
					        	workItemParent = (IWorkItem)repository.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray());
					        	IAttribute changeTemplateAttribute = workItemService.findAttribute(workItemParent.getProjectArea(), configChangeTemplate, monitor);
					        	Identifier changeTemplateValorID = (Identifier) changeTemplateAttribute.getValue(auditableCommon, workItemParent, monitor);
					        	String changeTemplateValor = changeTemplateValorID.getStringIdentifier();
					        	if (changeTemplateValor.equals(configChangeTemplateValue))	{
					        		templateSelected = configAttrTemplateName2; //Change the selected template
					        	}
				        	}
			        	}
			        
				        // Criar Itens de Trabalho
				        
				        Map<IAttributeVariable,Object> workItemTemplateVariables = new HashMap<IAttributeVariable,Object>();
				        IAttribute attrPlannedFor = workItemService.findAttribute(workItem.getProjectArea(), IWorkItem.TARGET_PROPERTY, monitor);
				        IAttributeVariable varPlannedFor = (IAttributeVariable)new AttributeVariable(attrPlannedFor);			        			        
				        workItemTemplateVariables.put(varPlannedFor, workItem.getTarget());
				        
				       	// Selecionar o Modelo de Itens de Trabalho
				        
				        String templateId = null;
				        Object[] templates = templateService.getAvailableTemplates2(workItem.getProjectArea());
				        int i=0;
				        for (Object obj : templates) {
				        	if (obj != null && obj instanceof String) {
					        	String t = (String)obj;
					        	
					        	if (t.equals(templateSelected)) { 
					        		templateId = (String)templates[i-1];
					        		break;
					        	}
				        	}
				        	i++;
				        }
				        
						if (templateSelected == null)	{
							continue;
						}
				        
				        if (templateId == null) {
				        	IAdvisorInfo info = collector.createProblemInfo("Template não encontrado", "Não foi possível encontrar o template "+templateSelected, "error");
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
				        
				        // Obter lista dos Itens de Trabalho criados à partir do Modelo selecionado.
				        
				        List<IWorkItemHandle> wisCreated = workItemService.findWorkItemsById(listWi, monitor);
				        
				        //Criar link entre os itens criados e o item de trabalho Pai (i.e.: Demanda de Manutenção de Sistemas)
				        
						IWorkItemReferences refs = saveParameter.getNewReferences();								
						for (IWorkItemHandle wiCreated : wisCreated) {
							IWorkItemReferences ref= workItemService.resolveWorkItemReferences(wiCreated, monitor);
							if (!ref.hasReferences(WorkItemEndPoints.PARENT_WORK_ITEM)){
								Reference reference = (Reference)IReferenceFactory.INSTANCE.createReferenceToItem(wiCreated);
								refs.add(WorkItemEndPoints.CHILD_WORK_ITEMS, reference); 	
							}
						}
						
						// Propagar o ID do Item de Trabalho Pai para os filhos.
						
						if (configPropagateParentId != null && configPropagateParentId.equals("true")) {
							for (IWorkItemHandle wiCreatedHandle : wisCreated) {
								IWorkItem wiCreated = (IWorkItem)repositoryService.fetchItem(wiCreatedHandle, new String[] {IWorkItem.SUMMARY_PROPERTY});
								XMLString xmlSummary = wiCreated.getHTMLSummary();
								String summary = xmlSummary.getPlainText();
								//XMLString xmlParentSummary = workItem.getHTMLSummary();
								//String parentSummary = xmlParentSummary.getPlainText();
								String parentId = String.valueOf(workItem.getId());
								
								StringBuffer newSummary = new StringBuffer();
								newSummary.append(summary).append(" (").append(parentId).append(")");
								
								IWorkItem wiCreatedCopy = (IWorkItem)wiCreated.getWorkingCopy();
								wiCreatedCopy.setHTMLSummary(XMLString.createFromPlainText(newSummary.toString()));							
								IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(wiCreatedCopy, null);
								
								try {
									workItemService.saveWorkItem2(wiCreatedCopy, copyReferences, null);	
								} catch (Exception e) {
									IReportInfo info = collector.createProblemInfo("Título do WI "+wiCreated.getId()+" não alterado", e.getMessage(), "info"); //$NON-NLS-1$
									info.setSeverity(IProcessReport.OK);
									collector.addInfo(info);
								}
							}						
						}
			        }
				}
			}			
		
		} catch (Exception e) {
			IAdvisorInfo info = collector.createProblemInfo("Erro inesperado", e.getMessage(), "error");
			collector.addInfo(info);
			throw new TeamRepositoryException(e);
		}
		
    }
}
		
    

