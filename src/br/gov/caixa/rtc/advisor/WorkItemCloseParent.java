/*******************************************************************************
 * Licensed Materials - Property of IBM (c) Copyright IBM Corporation 2005-2006.
 * All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights: Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ******************************************************************************/
package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.ibm.team.workitem.service.IWorkItemServer;

/*
 * <precondition id="com.ibm.rational.services.rtc.advisor.WorkItemCloseParent" name="Work Item Close Parent"/>
 * 		<config wiParentType="com.ibm.team.workitem.workItemType.ss" />
 * 		<config wiParentType="com.ibm.team.workitem.workItemType.ss" targetStateId="ignora a resolve action e manda para esse estado" />
 * </precondition>
 */
public class WorkItemCloseParent extends AbstractService implements IOperationAdvisor {

	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor)	throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			IAuditable auditable = saveParameter.getNewState();
			IAuditable oldAuditable = saveParameter.getOldState();
			if (auditable instanceof IWorkItem) {
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
				//-----------------------------------
								
				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				if (configElements != null) {
					boolean parentFound = false;
					
					IWorkItemServer workItemService = getService(IWorkItemServer.class);
					IRepositoryItemService repository = getService(IRepositoryItemService.class);
					IWorkflowInfo workflowInfo = workItemService.findWorkflowInfo(workItem, monitor);
					
					if (workflowInfo.getStateGroup(workItem.getState2()) == IWorkflowInfo.CLOSED_STATES) { //if you are closing this Work Item
						IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItem, monitor);
				        List<IReference> parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
				        
						for (IProcessConfigurationElement configElement : configElements) {
							String configWIType = configElement.getAttribute("wiParentType");
							String configTargetStateId = configElement.getAttribute("targetStateId");
							
							IWorkItem workItemParent = null;
							
					        if (parentReferences != null && parentReferences.size() > 0) { 
						        for (IReference parentReference : parentReferences) {
						        	IWorkItemHandle wiParentHandle = (IWorkItemHandle)parentReference.resolve();
						        	workItemParent = (IWorkItem)repository.fetchItem(wiParentHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
						        	
						        	if (workItemParent != null) {
						        		//check if parent can be closed (type check)
						        		String wiType = workItemParent.getWorkItemType();
										if (!wiType.equals(configWIType)) {
											parentFound = false;
											break;
										}						        								        		
						        		
						        		parentFound = true;
						        		break;
						        	}
						        }
					        }
					        
					        boolean allChildrenClosed = true;
					        
					        //---check if all children are closed
							if (parentFound) {
								IWorkItemReferences references2 = workItemService.resolveWorkItemReferences(workItemParent, monitor);
								List<IReference> childReferences = references2.getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
								if (childReferences != null && childReferences.size() > 0) {
									for (IReference childReference : childReferences) {
										IWorkItemHandle wiChildHandle = (IWorkItemHandle)childReference.resolve();
										if (!wiChildHandle.sameItemId(workItem.getItemHandle())) { //if child != this work item
											IWorkItem workItemChild = (IWorkItem)repository.fetchItem(wiChildHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
										
											IWorkflowInfo workflowChild = workItemService.findWorkflowInfo(workItemChild, monitor);
											
											if (workflowChild.getStateGroup(workItemChild.getState2()) != IWorkflowInfo.CLOSED_STATES) { //if there is open child open
												allChildrenClosed = false;
												break;
											}
										}										
									}
									
								}								
							}
							
							//---if all children closed, close parent
							if (allChildrenClosed && parentFound) {
								IWorkflowInfo workflowParent = workItemService.findWorkflowInfo(workItemParent, monitor);
								if (workflowParent.getStateGroup(workItemParent.getState2()) == IWorkflowInfo.CLOSED_STATES) {
									return; //parent is already closed
								}
								
								IWorkItem workingParentCopy = (IWorkItem)workItemParent.getWorkingCopy();
								IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(workingParentCopy, null);
								
								
								//altera para um estado especifico
								if (configTargetStateId != null && !configTargetStateId.equals("")) {
									Identifier<IState> stateIdSelected = null;
									for (Identifier<IState> stateId : workflowParent.getAllStateIds()) {
										if (stateId.getStringIdentifier().equals(configTargetStateId)) {
											stateIdSelected = stateId;
										}
									}
									if (stateIdSelected != null) {
										workingParentCopy.setState2(stateIdSelected);
										workItemService.saveWorkItem2(workingParentCopy, copyReferences, null);
									}
								} else {
									//aciona o resolve action
					                String resolveAction = workflowParent.getResolveActionId().getStringIdentifier();
									workItemService.saveWorkItem2(workingParentCopy, copyReferences, resolveAction);	
								}
											
							}
						        							
						}
					}

				}
					
			}
		}
	}
}
