package com.ibm.rational.services.rtc.advisor;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
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

public class DateTimeAdvisor extends AbstractService implements IOperationAdvisor {

	/**
	 * E.g.,
		<operation id="com.ibm.team.workitem.operation.workItemSave">
			<preconditions>
				<precondition id="com.ibm.rational.services.rtc.advisor.DateTimeAdvisor" name="Date Time Advisor">
					<config workItemType="task" dateAttr="dateOne" timeAttr="timeOne"/>
				</precondition>
			</preconditions>
		</operation>
	 */		
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			IAuditable auditable = saveParameter.getNewState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;
				IProcessConfigurationElement[] configElements = advisorConfiguration.getChildren();
				
				IWorkItemServer workItemService = getService(IWorkItemServer.class);
				
				String wiType = workItem.getWorkItemType();
				
				if (configElements != null) {
					for (IProcessConfigurationElement configElement : configElements) {
						String configWIType = configElement.getAttribute("workItemType");
						String configDateAttr = configElement.getAttribute("dateAttr");
						String configTimeAttr = configElement.getAttribute("timeAttr");
						Date dateValue = null;
						String timeValue = null;

						if (wiType.equals(configWIType)) {
			        		IAttribute dateAttribute = workItemService.findAttribute(workItem.getProjectArea(), configDateAttr, monitor);
							if (dateAttribute == null || !workItem.hasAttribute(dateAttribute)) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.getString("dateTime.missingAttribute"), Messages.getString("dateTime.missing")+" "+configDateAttr+Messages.getString("dateTime.attribute"), "error");
								collector.addInfo(info);
								return;
							}
							
			        		IAttribute timeAttribute = workItemService.findAttribute(workItem.getProjectArea(), configTimeAttr, monitor);
							if (timeAttribute == null || !workItem.hasAttribute(timeAttribute)) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.getString("dateTime.missingAttribute"), Messages.getString("dateTime.missing")+" "+configTimeAttr+Messages.getString("dateTime.attribute"), "error");
								collector.addInfo(info);
								return;
							}							
							
							Object objDate = workItem.getValue(dateAttribute);
							if (objDate instanceof Timestamp || objDate instanceof Date) {
								dateValue = (Date)objDate;	
							}
							if (objDate == null) { //don't have date, so is nothing to do
								return;								
							}
							
							Object objTime = workItem.getValue(timeAttribute);
							if (objTime instanceof String) {
								timeValue = objTime.toString();	
							}								
							if (timeValue == null || timeValue.equals("")) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.getString("dateTime.unexpectingTime"), Messages.getString("dateTime.filled")+" "+configTimeAttr+Messages.getString("dateTime.field"), "error");
								collector.addInfo(info);
								return;								
							}
							
							int hour = 0;
							int minute = 0;
							int second = 0;
							try {
								String[] arrayTime = timeValue.split(":");
								hour = new Integer(arrayTime[0]);
								minute = new Integer(arrayTime[1]);
								if (arrayTime.length == 3) {
									second = new Integer(arrayTime[2]);
								}
							} catch (Exception e) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.getString("dateTime.unexpectingTime"), Messages.getString("dateTime.unexpectingTime")+" "+timeValue, "error");
								collector.addInfo(info);
								return;
							}
															
							try {
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(dateValue);
								calendar.set(Calendar.HOUR_OF_DAY, hour);
								calendar.set(Calendar.MINUTE, minute);
								calendar.set(Calendar.SECOND, second);
								
								Timestamp dateTime = new Timestamp(calendar.getTime().getTime());
								
								workItem.setValue(dateAttribute, dateTime);								
							} catch (Exception e) {
								IAdvisorInfo info = collector.createProblemInfo(Messages.getString("dateTime.invalidTime"), Messages.getString("dateTime.invalidTimeDesc")+": "+e.getMessage(), "error");
								collector.addInfo(info);
								e.printStackTrace();
								return;
							}

						}
					}
				}
				
			}
		}
	}

}
