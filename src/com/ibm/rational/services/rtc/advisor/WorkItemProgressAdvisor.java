package com.ibm.rational.services.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemProgressAdvisor extends AbstractService implements IOperationAdvisor {

	/** 
	 * E.g.,
		<operation id="com.ibm.team.workitem.operation.workItemSave">
			<preconditions>
				<precondition id="com.ibm.rational.services.rtc.advisor.DateTimeAdvisor" name="Date Time Advisor">
					<config workItemType="task" stateId="open" percentageAttr="percent" value="0"/>
				</precondition>
			</preconditions>
		</operation>
	 */		
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			IAuditable auditable = saveParameter.getNewState();
			IAuditable oldAuditable = saveParameter.getOldState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				
				IWorkItemServer workItemService = getService(IWorkItemServer.class);
				
				String wiType = workItem.getWorkItemType();
				
				if (configElements != null) {
					for (IProcessConfigurationElement configElement : configElements) {
						String configWIType = configElement.getAttribute("workItemType");
						String percentageAttr = configElement.getAttribute("percentageAttr");
						String stateId = configElement.getAttribute("stateId");
						String value = configElement.getAttribute("value");

						if (wiType.equals(configWIType)) {
			        		IAttribute percentageAttribute = workItemService.findAttribute(workItem.getProjectArea(), percentageAttr, monitor);
							if (percentageAttribute != null && workItem.hasAttribute(percentageAttribute)) {
								int oldPercentage = 0;
								if (oldWorkItem != null) {
									oldPercentage = (Integer)oldWorkItem.getValue(percentageAttribute);
								}
								int actualPercentage = (Integer)workItem.getValue(percentageAttribute);								
								
								if (stateId.equals(workItem.getState2().getStringIdentifier()) && oldPercentage == actualPercentage) { //if in the correct state and the user doesn't change the percentage
									workItem.setValue(percentageAttribute, new Integer(value));
								} else if (stateId.equals(workItem.getState2().getStringIdentifier()) && value.equals("100")) { //if in the correct state and it's setted to 100% in config
									workItem.setValue(percentageAttribute, new Integer(value));
								}
																		
							} else {
								IAdvisorInfo info = collector.createProblemInfo("Attribute not found", percentageAttr+" not found, fix that or turn off WorkItemProgress Advisor", "warn");
								collector.addInfo(info);		
							}
						}
					}
				}
				
			}
		}
	}

}
