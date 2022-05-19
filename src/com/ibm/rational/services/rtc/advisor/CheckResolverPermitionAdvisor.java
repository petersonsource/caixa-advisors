package com.ibm.rational.services.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.foundation.common.internal.util.ItemQueryIterator;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.process.common.ITeamAreaHandle;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.internal.common.query.BaseTeamAreaQueryModel.TeamAreaQueryModel;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.IItemType;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.ast.IDynamicQueryModel;
import com.ibm.team.repository.common.query.ast.IItemQueryModel;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.ibm.team.workitem.service.IWorkItemServer;

/*
 * <precondition id="rtc.advisor.example.CheckResolverPermitionAdvisor" name="Verificar quem pode fechar o Work Item">
 * 		<config wiType="defect"/>
 * </precondition>
*/
public class CheckResolverPermitionAdvisor extends AbstractService implements IOperationAdvisor {
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;			
			IAuditable auditable = saveParameter.getNewState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				
				String wiType = workItem.getWorkItemType();
				IWorkItemServer workItemService = getService(IWorkItemServer.class);
				IQueryService queryService = getService(IQueryService.class);				
		        IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
		        
				IWorkflowInfo workflowInfo = workItemService.findWorkflowInfo(workItem, monitor);				

				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				if (configElements != null) {
					
					
					if (workflowInfo.getStateGroup(workItem.getState2()) == IWorkflowInfo.CLOSED_STATES) { //if you are closing this Work Item
						for (IProcessConfigurationElement configElement : configElements) {
							String configWIType = configElement.getAttribute("wiType");
							
							if (wiType.equals(configWIType)) {
								IContributorHandle creatorHandle = workItem.getCreator();
								IContributorHandle modifiedByHandle = auditableCommon.getUser();
								
								if (creatorHandle.sameItemId(modifiedByHandle)) { //close by the same user
									return;
								}
								
								List<ITeamAreaHandle> listTeamAreaHandle = findTeamAreas(queryService, workItem.getProjectArea(), creatorHandle);
								for (ITeamAreaHandle teamHandle : listTeamAreaHandle) {
									
									ITeamArea team = (ITeamArea)auditableCommon.resolveAuditable(teamHandle, ItemProfile.TEAM_AREA_DEFAULT, monitor);
									for (IContributorHandle member : team.getMembers()) {
										if (member.sameItemId(modifiedByHandle)) { //close by a peer of creator
											return;
										}										
									}
								}
								
								IAdvisorInfo info = collector.createProblemInfo("Operação não autorizada", "Você não tem permissão para fechar esse item de trabalho. Quem pode executar essa operação é quem abriu o item ou alguém que esteja na mesma equipe desse usuário.", "error");
								collector.addInfo(info);
								return;
							}
							
						}	
					}

				}
			}
		}
	}
	
	public static List<ITeamAreaHandle> findTeamAreas(IQueryService queryService, IItemHandle projectAreaHandle, IItemHandle contributorHandle) throws TeamRepositoryException {
		IItemType itemType = ITeamArea.ITEM_TYPE;
		IDynamicQueryModel model = itemType.getQueryModel();
		IItemQuery query = IItemQuery.FACTORY.newInstance((IItemQueryModel) model);
		
		IPredicate predicate1 = TeamAreaQueryModel.ROOT.contributors()._contains(query.newItemHandleArg());
		IPredicate predicate2 = TeamAreaQueryModel.ROOT.projectArea()._eq(query.newItemHandleArg());
		IPredicate predicate = query.and(predicate1, predicate2);
		query.filter(predicate);
		query.distinct();
		
		ItemQueryIterator<ITeamAreaHandle> iterator = new ItemQueryIterator<ITeamAreaHandle>(queryService, query, new Object[]{contributorHandle, projectAreaHandle});
		
		return iterator.getAllItems();
	} 	

}
