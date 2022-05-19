/*******************************************************************************
 * Licensed Materials - Property of IBM (c) Copyright IBM Corporation 2005-2006.
 * All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights: Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ******************************************************************************/
package br.gov.caixa.rtc.participant;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IReportInfo;
import com.ibm.team.process.common.advice.runtime.IOperationParticipant;
import com.ibm.team.process.common.advice.runtime.IParticipantInfoCollector;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.ibm.team.workitem.service.IWorkItemServer;

/*
 * <precondition id="com.ibm.rational.services.rtc.advisor.WorkItemChangeParentChildStates" name="Work Item Change Parent and Child States for Attribute Change"/>
 * 		<config workItemType="com.ibm.team.workitem.workItemType.ss" stateId="" targetStateId="" childWorkItemType="" childTargetStateId="" changedAttributeId="" attributeNewValue=""/>
 * </precondition>
 */
public class WorkItemChangeParentChildStates extends AbstractService implements IOperationParticipant {

	private void changeWorkItemState(String targetState, IWorkItem workItem, IWorkItemServer workItemService, IParticipantInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException	{
		
		if (targetState != null && !targetState.equals("")) {
			Identifier<IState> stateIdSelected = null;
			IWorkflowInfo workflow = workItemService.findWorkflowInfo(workItem, monitor);
			for (Identifier<IState> stateId : workflow.getAllStateIds()) {
				if (stateId.getStringIdentifier().equals(targetState)) {
					stateIdSelected = stateId;
				}
			}
			if (stateIdSelected != null) {
				// IWorkItem workingCopy = (IWorkItem)workItem.getWorkingCopy(); --- Fix https://jazz.net/forum/questions/85212/stale-data-exception-in-an-operation-participant
				IWorkItem workingCopy= (IWorkItem)workItemService.getAuditableCommon().resolveAuditable(workItem ,IWorkItem.FULL_PROFILE,monitor).getWorkingCopy();
				IWorkItemReferences references = workItemService.resolveWorkItemReferences(workingCopy, null);
				workingCopy.setState2(stateIdSelected);
				workItemService.saveWorkItem2(workingCopy, references, null);
			} else	{
				//Estado configurado não existe.
	        	IReportInfo info = collector.createInfo("Configuração Incorreta", "Estado final do item de trabalho filho não existe.");
	        	collector.addInfo(info);
	        	return;
			}
		}	else {
			//Estado target do item filho não está configurado.
        	IReportInfo info = collector.createInfo("Configuração Incorreta", "Não foi possível encontrar o estado final do item de trabalho filho.");
        	collector.addInfo(info);
        	return;
		}
		
	}
	public void run(AdvisableOperation operation,
			IProcessConfigurationElement participantConfig,
			IParticipantInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			
			IAuditable auditable = saveParameter.getNewState();
			IAuditable oldAuditable = saveParameter.getOldState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
				
			/*	//------if don't change state, break
				String oldStateId = "";
				if (oldWorkItem != null) {
					oldStateId = oldWorkItem.getState2().getStringIdentifier();
				}
				String newStateId = workItem.getState2().getStringIdentifier();
				
				if (oldStateId.equals(newStateId)) {
					return;
				}
				*/
				IProcessConfigurationElement[] configElements = participantConfig.getChildren();
				if (configElements != null) {
										
					IWorkItemServer workItemService = getService(IWorkItemServer.class);
					IRepositoryItemService repository = getService(IRepositoryItemService.class);

					//Get the children list
					IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItem, monitor);
				    List<IReference> childReferences = references.getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
				    
				  //---------- Change state of work item due to attribute change -------------------
					for (IProcessConfigurationElement configElement : configElements) {
						String configWorkItemType = configElement.getAttribute("workItemType");
						String configStateId = configElement.getAttribute("stateId");
						String configChangedAttribute = configElement.getAttribute("changedAttributeId");
						String configAttributeNewValue = configElement.getAttribute("attributeNewValue");
						
						
						//check work item type
						String workItemType = workItem.getWorkItemType();
						if (!workItemType.equals(configWorkItemType))	{
							break; //Stop advisor if is the wrong type.
						}
						
						//check the current state
						String workItemState = workItem.getState2().getStringIdentifier();
						if (!workItemState.equals(configStateId))	{
							break; //Stop advisor if in wrong state.
						}
						
						//check attribute change - In this case, must be an enumeration based attribute.
						IAttribute changedAttribute = workItemService.findAttribute(workItem.getProjectArea(), configChangedAttribute, monitor);
						if (!changedAttribute.hasValueSet(workItemService.getAuditableCommon(), monitor) || changedAttribute.getAttributeType().equals("boolean"))	{
				        	IReportInfo info = collector.createInfo("Configuração Incorreta.", "Participant: Atributo com modificação monitorada deve ser uma enumeração.");
				        	collector.addInfo(info);
				        	break;						
						}
						Identifier attributeOldValueID = (Identifier) oldWorkItem.getValue(changedAttribute);
						String attributeOldValue = attributeOldValueID.getStringIdentifier();
						Identifier attributeCurrentValueID = (Identifier) workItem.getValue(changedAttribute);
						String attributeCurrentValue = attributeCurrentValueID.getStringIdentifier();
						String configAttributeStartValue = configElement.getAttribute("attributeStartValue");
						
						if (!configAttributeStartValue.equals(attributeOldValue)){
							break;
						}
					
						if(attributeOldValue.equals(attributeCurrentValue))	{ //if value wasn't changed...
							break; //Stop follow-up action if the attribute hasn't changed.
						}
						else{if (!attributeCurrentValue.equals(configAttributeNewValue))	{ // if current value is different from configured value	
								continue; //Stop follow-up action if the attribute has the wrong value
							}
						
						
							//change the state of the work item
							String configTargetStateId = configElement.getAttribute("targetStateId");
							changeWorkItemState(configTargetStateId, workItem, workItemService, collector, monitor);
						
							//change the state of the configured children
							IWorkItem childWorkItem = null;
								
						    if (childReferences != null && childReferences.size() > 0) { 
						        for (IReference childReference : childReferences) {
						        	IWorkItemHandle wiChildHandle = (IWorkItemHandle)childReference.resolve();
						        	childWorkItem = (IWorkItem)repository.fetchItem(wiChildHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
						        	
						        	if (childWorkItem != null) {
						        		String configChildWorkItemType = configElement.getAttribute("childWorkItemType");
						        		String childWorkItemType = childWorkItem.getWorkItemType();
										
						        		//check if is possible to change the state of the child work item (type check)
						        		if (!childWorkItemType.equals(configChildWorkItemType)) { 
											//Jump to next child. This work item type is of the wrong type.
											break;
										}						        								        		
						        		
						        		//check the current state of the child work item
						        		String configChildTargetStateId = configElement.getAttribute("childTargetStateId");
						        		String childCurrentStateId = childWorkItem.getState2().getStringIdentifier();
						        		
						        		if (configChildTargetStateId.equals(childCurrentStateId))	{
						        			break; //Jump to next child. The state do not need to be changed.
						        		}
						        		
						        		//change child work item state
						        		changeWorkItemState(configChildTargetStateId, childWorkItem, workItemService, collector, monitor);
						        	}
						        }
						    }
						}
					}
				}
			}
		}
	}
}