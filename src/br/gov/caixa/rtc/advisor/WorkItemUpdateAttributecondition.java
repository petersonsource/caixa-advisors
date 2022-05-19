package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.internal.IWorkItemTemplateService;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemUpdateAttributecondition extends AbstractService implements IOperationAdvisor {

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
					String configValueAttribute = configElement.getAttribute("valueAttribute"); // i.e.: o novo valor que atributo assumirá.
					
					//check params
					if (!wiType.equals(configWIType)) {
						continue;
					}
					
					if (workItem.isNewItem()){
						continue;
					}
					
					//checa se o valor para o atributo foi passado
					if (configValueAttribute == null){
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
					
					IAttribute requiredAttribute = workItemService.findAttribute(workItem.getProjectArea(), configAttribute, monitor);
					IEnumeration enumeration = workItemService.resolveEnumeration(requiredAttribute, monitor);
					Identifier id =getLiteralEqualsString(configValueAttribute,requiredAttribute,workItemService,monitor);
					if (id==null){
						String msg = new String("Existe um problema na configuração de um parâmetro da automação \"Atualizar valor de atributo com Condição\". Contate o Administrador".getBytes(),"UTF-8");
						IAdvisorInfo info = collector.createProblemInfo("Erro!!!",msg, "error");
						collector.addInfo(info);
					}else{
						workItem.setValue(requiredAttribute, id);
					}
					
					
				}
			}
		}
		catch (Exception e) {
					// TODO: handle exception
				}
	
	}
	
	private static Identifier getLiteralEqualsString(String name, IAttribute requiredAttribute, IWorkItemServer workItemService, IProgressMonitor monitor) throws TeamRepositoryException {
		
		Identifier literalID = null;
		IEnumeration enumeration = workItemService.resolveEnumeration(requiredAttribute, monitor);
		List<ILiteral> literals = enumeration.getEnumerationLiterals();
		for (ILiteral literal:literals){
			if (literal.getIdentifier2().getStringIdentifier().equals(name)){
				literalID = literal.getIdentifier2();
				break;
			}
		}
		return literalID;
	}
}
