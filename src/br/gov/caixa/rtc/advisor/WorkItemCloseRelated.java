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
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IResolution;
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
public class WorkItemCloseRelated extends AbstractService implements IOperationAdvisor {

	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor)	throws TeamRepositoryException {		Object data = operation.getOperationData();
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
					boolean relatedFound = false;
					
					IWorkItemServer workItemService = getService(IWorkItemServer.class);
					IRepositoryItemService repository = getService(IRepositoryItemService.class);
					IWorkflowInfo workflowInfo = workItemService.findWorkflowInfo(workItem, monitor);
					
					if (workflowInfo.getStateGroup(workItem.getState2()) == IWorkflowInfo.CLOSED_STATES) { //if you are closing this Work Item
						IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItem, monitor);
						
				        List<IReference> relatedReferences = references.getReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
				        
						for (IProcessConfigurationElement configElement : configElements) {
							String configWIType = configElement.getAttribute("wiRelatedType");
							String configTargetStateId = configElement.getAttribute("targetStateId");
							String configOutOfScopeResolutionId = configElement.getAttribute("outOfScopeResolutionId");
							
							//Advisor must not run if the current workitem has the out of scope resolution
//							String outOfScopeResolutionIdValue = workItem.getResolution2().getStringIdentifier();
							if (workflowInfo.getShowResoution(workItem.getState2()) && workItem.getResolution2().getStringIdentifier().equals(configOutOfScopeResolutionId))	{
								return;
							}
														
							IWorkItem workItemRelated = null;
														
					        if (relatedReferences != null && relatedReferences.size() > 0) { 
						        for (IReference relatedReference : relatedReferences) {
						        	IWorkItemHandle wiRelatedHandle = (IWorkItemHandle)relatedReference.resolve();
						        	workItemRelated = (IWorkItem)repository.fetchItem(wiRelatedHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
						        	
						        	if (workItemRelated != null) {
						        		//check if related can be closed (type check)
						        		String wiType = workItemRelated.getWorkItemType();
										if (!wiType.equals(configWIType)) {
											relatedFound = false;
											break;
										}						        								        		
						        		
						        		relatedFound = true;
						        		break;
						        	}
						        }
					        }
					        
					        boolean allRelatedClosed = true;
					        boolean allResolutionOutOfScope = true; 
					        
					        //---check if all children are closed
							if (relatedFound) {
								IWorkItemReferences references2 = workItemService.resolveWorkItemReferences(workItemRelated, monitor);
								List<IReference> backRelatedReferences = references2.getReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
								if (backRelatedReferences != null && backRelatedReferences.size() > 0) {
									boolean approved = false;
									for (IReference backRelatedReference : backRelatedReferences) {
										IWorkItemHandle wiBackRelatedHandle = (IWorkItemHandle)backRelatedReference.resolve();
										IWorkItem workItemBackRelated = (IWorkItem)repository.fetchItem(wiBackRelatedHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
										IWorkflowInfo workflowBackRelated = workItemService.findWorkflowInfo(workItemBackRelated, monitor);
										if (!wiBackRelatedHandle.sameItemId(workItem.getItemHandle())) { //if related != this work item
											if (workflowBackRelated.getStateGroup(workItemBackRelated.getState2()) != IWorkflowInfo.CLOSED_STATES) { //if there is open related open
												allRelatedClosed = false;
												break;
											}
											if (workflowBackRelated.getShowResoution(workItemBackRelated.getState2()))	{
												if (workItemBackRelated.getResolution2().getStringIdentifier().equals(configOutOfScopeResolutionId)&&(!approved))	{
													allResolutionOutOfScope = true;
												}else{
													allResolutionOutOfScope = false;
													approved = true;
												}
											}
										} else	{
											//Obter as resoluções do item de trabalho atual
											if	(workflowBackRelated.getStateResolutionIds(workItem.getState2()).length > 0)	{
												if (!workItem.getResolution2().getStringIdentifier().equals(configOutOfScopeResolutionId))	{
													allResolutionOutOfScope = false;
													approved = true;
												}
											}
											else{
												allResolutionOutOfScope = false;
											}
										}
									}
								}								
							}
							
							//---if all children closed and has no out of scope resolution, close related work item.
							if (allRelatedClosed && relatedFound && !allResolutionOutOfScope) {
								IWorkflowInfo workflowRelated = workItemService.findWorkflowInfo(workItemRelated, monitor);
								if (workflowRelated.getStateGroup(workItemRelated.getState2()) == IWorkflowInfo.CLOSED_STATES) {
									return; //related is already closed
								}
								
								IWorkItem workingRelatedCopy = (IWorkItem)workItemRelated.getWorkingCopy();
								IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(workingRelatedCopy, null);
								
								
								//altera para um estado especifico
								if (configTargetStateId != null && !configTargetStateId.equals("")) {
									Identifier<IState> stateIdSelected = null;
									for (Identifier<IState> stateId : workflowRelated.getAllStateIds()) {
										if (stateId.getStringIdentifier().equals(configTargetStateId)) {
											stateIdSelected = stateId;
										}
									}
									if (stateIdSelected != null) {
										workingRelatedCopy.setState2(stateIdSelected);
										workItemService.saveWorkItem2(workingRelatedCopy, copyReferences, null);
									}
								} else {
									//aciona o resolve action
					                String resolveAction = workflowRelated.getResolveActionId().getStringIdentifier();
									workItemService.saveWorkItem2(workingRelatedCopy, copyReferences, resolveAction);	
								}
											
							}
						        							
						}
					}

				}
					
			}
		}
	}
}
