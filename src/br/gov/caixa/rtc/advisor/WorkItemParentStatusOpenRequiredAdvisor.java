/*******************************************************************************
 * Licensed Materials - Property of IBM (c) Copyright IBM Corporation 2005-2006.
 * All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights: Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ******************************************************************************/

//<precondition id="rtc.advisors.CreateWorkItemFromTemplate" name="Create WorkItems From Template">
// 		<config workItemType="tarefa" stateId="Concluído" parentRequired="Marco"/>
// </precondition>

package br.gov.caixa.rtc.advisor;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class WorkItemParentStatusOpenRequiredAdvisor extends AbstractService implements IOperationAdvisor {

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

				IWorkItem workItem = (IWorkItem) auditable;
				
				String wiType = workItem.getWorkItemType();
				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				if (configElements != null) {
					boolean parentFound = false;
					boolean stateParent = true;
					
					IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
					IWorkItemCommon workItemCommon = auditableCommon.getPeer(IWorkItemCommon.class);
					IRepositoryItemService repository = getService(IRepositoryItemService.class);
					
					//IWorkItemReferences references = workItemCommon.resolveWorkItemReferences(workItem, monitor);
					IWorkItemReferences references = saveParameter.getNewReferences();
					
					for (IProcessConfigurationElement configElement : configElements) {
						String configWIType = configElement.getAttribute("workItemType"); 
						String parentRequired = configElement.getAttribute("parentRequired"); 
						String configStateId = configElement.getAttribute("stateId");
						String messageParentClosed = configElement.getAttribute("parentClosed");
						
	
												
						if (wiType.equals(configWIType)) {
					        List<IReference> parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
					        if (parentReferences != null && parentReferences.size() > 0) { 
						        for (IReference parentReference : parentReferences) {
						        	IWorkItemHandle wiParentHandle = (IWorkItemHandle)parentReference.resolve();
						        	IWorkItem workItemParent = (IWorkItem)repository.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray());
						        	String parentStateID = workItemParent.getState2().getStringIdentifier();
						        	
						        	if (workItemParent.getWorkItemType().equals(parentRequired)) {
						        		parentFound = true;
						        		
						        		if (configStateId.equals(workItemParent.getState2().getStringIdentifier())) {
						        			stateParent = false;
						        			break;
						        		}
									}
						        }
					        }
					        if (stateParent == false) {
						        IAdvisorInfo info;
								info = collector.createProblemInfo("Operação bloqueada .", messageParentClosed, "error");
								collector.addInfo(info);
							}
					        
						}
					}

				}
					
			
		}
	
}
