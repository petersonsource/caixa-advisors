package com.ibm.rational.services.rtc.advisor.util;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.workitem.common.QueryIterator.ReadMode;
import com.ibm.team.workitem.common.internal.model.query.BaseWorkItemQueryModel.WorkItemQueryModel;
import com.ibm.team.workitem.common.internal.util.ItemQueryIterator;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.service.IAuditableServer;

public class WorkItemUtil {
	public static List<IWorkItem> getWorkItems(IAuditableServer auditableService, IProjectAreaHandle projectArea, ItemProfile profile, IProgressMonitor monitor) throws TeamRepositoryException {
		WorkItemQueryModel model= WorkItemQueryModel.ROOT;
		IItemQuery query = IItemQuery.FACTORY.newInstance(model);
		IPredicate predicate = model.projectArea()._eq(query.newItemHandleArg());
		query.filter(predicate);

		ItemQueryIterator<IWorkItemHandle> iterator = new ItemQueryIterator<IWorkItemHandle>(auditableService, query, new Object[] { projectArea }, null, null);

		List<IWorkItem> workItems = auditableService.resolveAuditables(iterator.toList(monitor), profile, monitor); 
		
		return workItems;		
	}
	
	
	public static List<IWorkItem> getWorkItems(IAuditableServer auditableService, IProjectAreaHandle projectArea, IProgressMonitor monitor) throws TeamRepositoryException {
		return getWorkItems(auditableService, projectArea, IWorkItem.FULL_PROFILE, monitor);
	}
}
