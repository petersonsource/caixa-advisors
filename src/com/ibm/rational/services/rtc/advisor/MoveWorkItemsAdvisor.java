package com.ibm.rational.services.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.rational.services.rtc.advisor.util.WorkItemUtil;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.service.IAuditableServer;
import com.ibm.team.workitem.service.IWorkItemServer;
import com.ibm.team.workitem.service.internal.ServerCopyToProjectOperation;
import com.ibm.team.workitem.service.internal.WorkItemWrapper;

public class MoveWorkItemsAdvisor extends AbstractService implements IOperationAdvisor {
	
	public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {
		Object data = operation.getOperationData();
		if (data instanceof ISaveParameter) {
			ISaveParameter saveParameter = (ISaveParameter) data;
			IAuditable auditable = saveParameter.getNewState();
			if (auditable instanceof IWorkItem) {
				IWorkItem workItem = (IWorkItem) auditable;

				String wiType = workItem.getWorkItemType();
				
				if (wiType.equals("task") && workItem.getHTMLSummary().getPlainText().startsWith("moveAllInto(")) {
					IWorkItemServer workItemService = getService(IWorkItemServer.class);	
					IAuditableServer auditableService = getService(IAuditableServer.class);
					IProcessServerService processServerService = getService(IProcessServerService.class);

					String temp[] = workItem.getHTMLSummary().getPlainText().split("moveAllInto\\(");
					String arg[] = temp[1].substring(0, temp[1].length()-1).split(",");
					String paName = arg[0];
					int limite = Integer.MAX_VALUE;

					if (arg.length == 2) try{limite = Integer.valueOf(arg[1]);}catch (NumberFormatException e){	};
					IProcessArea projectAreaDest = processServerService.findProcessArea(paName, null);

					if (projectAreaDest == null) {
						IAdvisorInfo info = collector.createProblemInfo("Project Area not found", paName+" not found", "error");
						collector.addInfo(info);
						return;
					}

					List<IWorkItem> workitems = WorkItemUtil.getWorkItems(auditableService, workItem.getProjectArea(), monitor);
					for (IWorkItem wi : workitems) {
						IWorkItem wiCopy = (IWorkItem)wi.getWorkingCopy();
				        IWorkItemReferences workItemReferences = workItemService.resolveWorkItemReferences(wiCopy, monitor); 

				        WorkItemWrapper wiWrapper = new WorkItemWrapper(wiCopy, workItemReferences, "");
				        ServerCopyToProjectOperation op = new ServerCopyToProjectOperation(projectAreaDest.getProjectArea(), true, true, false, null);
//TODO: arrumar os parametros				        op.run(wiWrapper, workItemService);

				        workItemService.saveWorkItem2(wiCopy, null, null);
				        if ((limite--)<=0) break;
					}	

					//IAdvisorInfo info = collector.createProblemInfo("Moved", "All items was moved to "+paName, "error");
					//collector.addInfo(info);
					//return;
				} else if (workItem.getHTMLSummary().getPlainText().matches(".*\\moveInto\\(.*\\)\\>")){
					IWorkItem wiCopy = (IWorkItem)workItem.getWorkingCopy();
					IWorkItemServer workItemService = getService(IWorkItemServer.class);	
					IProcessServerService processServerService = getService(IProcessServerService.class);
			        IWorkItemReferences workItemReferences = workItemService.resolveWorkItemReferences(wiCopy, monitor); 

			        WorkItemWrapper wiWrapper = new WorkItemWrapper(wiCopy, workItemReferences, "");
			        String[] summary = workItem.getHTMLSummary().getPlainText().split("<moveInto\\(");
			        summary[1] = summary[1].substring(0,summary[1].length()-2);
			        wiCopy.setHTMLSummary(XMLString.createFromPlainText(summary[0]));
					IProcessArea projectAreaDest = processServerService.findProcessArea(summary[1], null);

			        ServerCopyToProjectOperation op = new ServerCopyToProjectOperation(projectAreaDest.getProjectArea(), true, true, false, null);
//TODO: arrumar os parametros			        op.run(wiWrapper, workItemService);

			        workItemService.saveWorkItem2(wiCopy, null, null);

				}
				
			}
		}
	}

}
