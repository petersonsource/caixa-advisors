//package br.gov.caixa.rtc.advisor;
//
//import java.util.List;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//
//import com.ibm.team.process.common.IProcessConfigurationElement;
//import com.ibm.team.process.common.advice.AdvisableOperation;
//import com.ibm.team.process.common.advice.IAdvisorInfo;
//import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
//import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
//import com.ibm.team.repository.common.IAuditable;
//import com.ibm.team.repository.common.TeamRepositoryException;
//import com.ibm.team.workitem.common.ISaveParameter;
//import com.ibm.team.workitem.common.model.IAttribute;
//import com.ibm.team.workitem.common.model.ICategory;
//import com.ibm.team.workitem.common.model.IWorkItem;
//import com.ibm.team.workitem.service.IWorkItemServer;
//
//public class WorkItemSetPlannedForFromEnumeration extends AbstractRTCService implements
//	IOperationAdvisor {
//
//    private static final String ERROR_MESSAGE_TYPE = "error";
//
//    @Override
//    public void run(AdvisableOperation operation,
//	    IProcessConfigurationElement advisorConfiguration,
//	    IAdvisorInfoCollector collector, IProgressMonitor monitor)
//	    throws TeamRepositoryException {
//
//	Object data = operation.getOperationData();
//	if (!(data instanceof ISaveParameter)) {
//	    return;
//	}
//
//	ISaveParameter saveParameter = (ISaveParameter) data;
//	IAuditable auditable = saveParameter.getNewState();
//	IAuditable oldAuditable = saveParameter.getOldState();
//	if (!(auditable instanceof IWorkItem)
//		&& !(oldAuditable instanceof IWorkItem)) {
//	    return;
//	}
//	IWorkItem workItem = (IWorkItem) auditable;
//	IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
//	IAdvisorInfo info;
//	String labelAttrCategory = null;
//	String oldLabelAttrCategory = null;
//	IAttribute attrEnumeration;
//	IAttribute oldAttrEnumeration;
//	
//
//	try {
//	    IProcessConfigurationElement[] configElements = advisorConfiguration
//		    .getChildren();
//	    if (configElements != null) {
//		IWorkItemServer workItemService = getService(IWorkItemServer.class);
//
//		String wiType = workItem.getWorkItemType();
//
//		for (IProcessConfigurationElement configElement : configElements) {
//		    // INPUT PARAMETER
//		    String configWIType = configElement
//			    .getAttribute("workItemType");
//
//		    String condifAttrId = configElement
//			    .getAttribute("attributeId");
//
//		    if (!wiType.equals(configWIType)) {
//			continue;
//		    }
//
//		    attrEnumeration = getAttr(workItemService, workItem,
//			    condifAttrId, monitor);
//
//		    // get label of value
//		    if (workItem.hasCustomAttribute(attrEnumeration)) {
//			Object value = workItem.getValue(attrEnumeration);
//			if (value instanceof String) {
//			    labelAttrCategory = (String) value;
//			}
//		    }
//
//		    // if not change
//		    if (oldWorkItem != null) {
//			oldAttrEnumeration = getAttr(workItemService, oldWorkItem,
//				condifAttrId, monitor);
//			Object oldValue = oldWorkItem.getValue(oldAttrEnumeration);
//			if (oldValue instanceof String) {
//			    oldLabelAttrCategory = (String) oldValue;
//			}
//			if (!labelAttrCategory.equals(oldLabelAttrCategory)) {
//
//			} else {
//			    continue;
//			}
//		    }
//
//		    // get categories
//		    List<ICategory> listaCategorias = workItemService
//			    .findCategories(workItem.getProjectArea(),
//				    ICategory.FULL_PROFILE, monitor);
//
//		    for (ICategory categoria : listaCategorias) {
//			if (categoria.getName().equals(labelAttrCategory)) {
//			    if (!categoria.isArchived()) {
//				categoryFound = categoria;
//				break;
//			    }
//			}
//		    }
//
//		    // string of attribute not found in category list
//		    if (categoryFound == null) {
//			info = collector.createProblemInfo(
//				"Categoria inexistente ou arquivada",
//				labelAttrCategory, "");
//			collector.addInfo(info);
//		    }
//
//		    // set valid category
//		    workItem.setCategory(categoryFound);
//
//		}
//	    }
//	} catch (TeamRepositoryException e) {
//	    message(e, collector);
//	} catch (Exception e) {
//	    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
//	}
//    }
//
//    private static IAttribute getAttr(IWorkItemServer workItemService,
//	    IWorkItem workItem, String attr, IProgressMonitor monitor)
//	    throws TeamRepositoryException {
//	IAttribute attrValue = null;
//	attrValue = workItemService.findAttribute(workItem.getProjectArea(),
//		attr, monitor);
//	return attrValue;
//
//    }
//}
