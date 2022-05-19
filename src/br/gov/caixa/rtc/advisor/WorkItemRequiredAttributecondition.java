package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.internal.IWorkItemTemplateService;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemRequiredAttributecondition extends AbstractService implements IOperationAdvisor {

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
		if (!(auditable instanceof IWorkItem)&& !(oldAuditable instanceof IWorkItem)) {
			return;
		}
		IWorkItem workItem = (IWorkItem) auditable;
		IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
		
		//------if don't change state, break
		String oldStateId = "";
		
		if (oldWorkItem != null) {
			oldStateId = oldWorkItem.getState2().getStringIdentifier();
		}
		String newStateId = workItem.getState2().getStringIdentifier();
		
		if (oldStateId.equals(newStateId)) {
			return;
		}
		
	
		// TODO Auto-generated method stub
		try {
			IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
		
			if (configElements != null) {
		        IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
		        IProcessServerService processService = getService(IProcessServerService.class);
		        IWorkItemTemplateService templateService = getService(IWorkItemTemplateService.class);
		        IWorkItemServer workItemService = getService(IWorkItemServer.class);	
		        IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
			
		        String wiType = workItem.getWorkItemType();
		        IWorkItem workItemOld = (IWorkItem) saveParameter.getOldState();
		        
		        
				for (IProcessConfigurationElement configElement : configElements) {
					String configWIType = configElement.getAttribute("workItemType"); // Tipo do workitem.
					String configStateId = configElement.getAttribute("stateId"); // Estado atual.
					String configOldStateId = configElement.getAttribute("oldStateId"); // Estado anterior.
					String configAttribute = configElement.getAttribute("attribute"); // i.e.: Atributo que será afetado.
					
					//check if is a new item
					if (workItem.isNewItem()){
						continue;
					}
					
					//check params
					if (!wiType.equals(configWIType)) {
						continue;
					}
					
//					if (!configOldStateId.equalsIgnoreCase("s"+workItemOld.getState2().getStringIdentifier())){
					if (!configOldStateId.equalsIgnoreCase(workItemOld.getState2().getStringIdentifier())){						
						configOldStateId = configOldStateId.replace("s", "");
						if (!configOldStateId.equalsIgnoreCase(workItemOld.getState2().getStringIdentifier())){						
							continue;					
						}
					}
					
//					if (!configStateId.equalsIgnoreCase("s"+workItem.getState2().getStringIdentifier())){
					if (!configStateId.equalsIgnoreCase(workItem.getState2().getStringIdentifier())){
						configStateId = configStateId.replace("s", "");
						if (!configStateId.equalsIgnoreCase(workItem.getState2().getStringIdentifier())){						
							continue;							
						}
					}
					
					//recupera a acao executada
					IWorkflowInfo workflowInfo = workItemService.findWorkflowInfo(workItem, monitor);
						Identifier availableActions[] = workflowInfo.getActionIds(workItemOld.getState2());
						String action = ((ISaveParameter) data).getWorkflowAction();
						Identifier<IWorkflowAction> actionId = null;
						
						for (int i = 0; i < availableActions.length; i++) {
							if (action == availableActions[i].getStringIdentifier()) {
								actionId = availableActions[i];
								break;
							}
						}	
						String ActionName = "";
				        if (actionId!=null){
				        	ActionName = workItemService.findWorkflowInfo(workItem, monitor).getActionName(actionId);
				        }
					
					IAttribute requiredAttribute = workItemService.findAttribute(workItem.getProjectArea(), configAttribute, monitor);
					// identifica se o attribute é do tipo enumeration
					String valueAttribute = "";
					if (AttributeTypes.isEnumerationAttributeType(requiredAttribute.getAttributeType())){
						//identifica o valor do attribute
						Identifier id = (Identifier)workItem.getValue(requiredAttribute);
						// resolver os valores do enumeration attribute
						IEnumeration enumeration = workItemService.resolveEnumeration(requiredAttribute, monitor);
						
						//identifica o valor padrao da enumeration
						//ILiteral defaultLiteral = enumeration.findDefaultEnumerationLiteral();

						//identifica o valor unassigned da enumeration
						ILiteral unassignedLiteral = enumeration.findNullEnumerationLiteral();
						if ( unassignedLiteral.getIdentifier2().getStringIdentifier().equals(id.getStringIdentifier())){	
							
							IAdvisorInfo info = collector.createProblemInfo("Erro","Para \""+ActionName+ "\" deve ser designado um valor para o atributo \""+configAttribute+ "\"", "error");
							collector.addInfo(info);
						}

						
						// gera uma lista de valores obtitidos do enumeration
//						List<ILiteral> literals = enumeration.getEnumerationLiterals();
//						for (ILiteral literal:literals){
//							if (literal.getIdentifier2().getStringIdentifier().equals(id.getStringIdentifier())){
//								valueAttribute = literal.getName();
//								break;
//							}
//						}
					}
									
					
//					if(wiType.contains("fail")) {
//						IAdvisorInfo info = collector.createProblemInfo("Advisor de Obrigatoriedade", "Mensagem de erro personalizada", "error");
//						collector.addInfo(info);
//					}
				
					
				}
			}	
		}
		catch (Exception e) {
			IAdvisorInfo info = collector.createProblemInfo("Mensagem de Erro inesperado", e.getMessage(), "error");
			collector.addInfo(info);
			throw new TeamRepositoryException(e);
		}
	}

}
