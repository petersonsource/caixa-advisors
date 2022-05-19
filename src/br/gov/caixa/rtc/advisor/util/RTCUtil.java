package br.gov.caixa.rtc.advisor.util;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.foundation.common.internal.util.ItemQueryIterator;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.internal.common.query.BaseIterationQueryModel.IterationQueryModel;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItemType;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.ast.IDynamicQueryModel;
import com.ibm.team.repository.common.query.ast.IItemQueryModel;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

public class RTCUtil {
	public static IWorkItem createWorkItem(IWorkItemServer workItemService, IWorkItemType workItemType, String summary, IContributorHandle modifiedBy) throws TeamRepositoryException{
		IWorkItem workItem = workItemService.createWorkItem2(workItemType);
		workItem.setHTMLSummary(XMLString.createFromPlainText(summary));
		workItem.setCreator(modifiedBy);
		
		return workItem;
	}	
	
	public static IIteration createIteration(IRepositoryItemService repositoryService, IProcessServerService processService, IIterationHandle iterParentHandle, String name, String id) throws TeamRepositoryException {
		IIteration iterationParent = (IIteration) repositoryService.fetchItem(iterParentHandle, IRepositoryItemService.COMPLETE);
		IIteration newIteration = (IIteration) iterationParent.getItemType().createItem();
		IIteration itParentCopy = (IIteration) iterationParent.getWorkingCopy(); 
		
		newIteration.setDevelopmentLine(iterationParent.getDevelopmentLine());
		newIteration.setHasDeliverable(true);        
		newIteration.setName(name);
		newIteration.setId(id);
		newIteration.setParent(iterParentHandle);      
        itParentCopy.addChild(newIteration);
        
        processService.saveProcessItems(new IProcessItem[] {newIteration, itParentCopy});
        
        return newIteration;
	}
	
	public static IIteration createIteration(IRepositoryItemService repositoryService, IProcessServerService processService, IDevelopmentLine iDevelopmentLine, String name, String id, Date startDate, Date endDate) throws TeamRepositoryException {
		IDevelopmentLine timeLine = (IDevelopmentLine) repositoryService.fetchItem(iDevelopmentLine, IRepositoryItemService.COMPLETE);
		IIteration newIteration = (IIteration) timeLine.getCurrentIteration().getItemType().createItem();
		IDevelopmentLine timeLineCopy = (IDevelopmentLine) timeLine.getWorkingCopy(); 
		
		newIteration.setDevelopmentLine(iDevelopmentLine);
		newIteration.setHasDeliverable(true);
		newIteration.setName(name);
		newIteration.setId(id);
		newIteration.setStartDate(startDate);
		newIteration.setEndDate(endDate);
        timeLineCopy.addIteration(newIteration);
        
        processService.saveProcessItems(new IProcessItem[] {newIteration, timeLineCopy});
        
        return newIteration;
	}
	
	public static List<IProjectAreaHandle> findProjectAreas(IQueryService queryService) throws TeamRepositoryException {
		IItemType itemType = IProjectArea.ITEM_TYPE;
		IDynamicQueryModel model = itemType.getQueryModel();
		IItemQuery query = IItemQuery.FACTORY.newInstance((IItemQueryModel) model);
		ItemQueryIterator<IProjectAreaHandle> iterator = new ItemQueryIterator<IProjectAreaHandle>(queryService, query, new Object[]{});
		
		return iterator.getAllItems();
	}
	
	public static IIterationHandle findIterationById(IQueryService queryService, String iterationId) throws TeamRepositoryException {
		IItemType itemType = IIteration.ITEM_TYPE;
		IDynamicQueryModel model = itemType.getQueryModel();
		IItemQuery query = IItemQuery.FACTORY.newInstance((IItemQueryModel) model);
		
		IPredicate predicate = IterationQueryModel.ROOT.id()._eq(query.newStringArg());
		query.filter(predicate);
		
		ItemQueryIterator<IIterationHandle> iterator = new ItemQueryIterator<IIterationHandle>(queryService, query, new Object[]{iterationId});
		
		return iterator.next();
	} 	
		
	public static IProjectArea getProjectArea(IProcessServerService processService, String projectAreaName) throws TeamRepositoryException {
		IProjectArea projectArea = (IProjectArea) processService.findProcessArea(projectAreaName, null);
		if (projectArea == null) {
			System.out.println("Project area not found.");
			return null;
		}

		return projectArea;
	}
	
	public static List<IReference> getChildReferences(ISaveParameter saveParameter) {
		List<IReference> children = null;
		IWorkItemReferences refs = saveParameter.getNewReferences();
		if (refs.hasReferences(WorkItemEndPoints.CHILD_WORK_ITEMS)) {												
			children = refs.getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
		}
		return children != null ? children : Collections.<IReference>emptyList();
	}
	
	public static IEnumeration resolveEnumerations(IWorkItemServer workItemService, IAttribute attribute, IProgressMonitor monitor) throws TeamRepositoryException {
    	if (AttributeTypes.isEnumerationAttributeType(attribute.getIdentifier())) {
    		IEnumeration enumeration = workItemService.resolveEnumeration(attribute, monitor);
    		return enumeration;
    	}		
		
    	return null;
	}
	
	
	//TODO LPF
	@SuppressWarnings("rawtypes")
	private static Identifier getLiteralEqualsString(final String name, final IAttribute requiredAttribute, final IWorkItemServer workItemService, final IProgressMonitor monitor) throws TeamRepositoryException {
		
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
