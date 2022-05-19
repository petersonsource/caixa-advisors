package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.IReportInfo;
import com.ibm.team.process.common.advice.runtime.IParticipantInfoCollector;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;
//import br.gov.caixa.rtc.dto.EnumerationDto;

public class AbstractRTCService extends AbstractService {
    

	public void message(String msg, String type, IAdvisorInfoCollector collector) {
		IAdvisorInfo info = collector.createProblemInfo(msg, "", type);
		collector.addInfo(info);
	}

	public void message(String msg, String type,
			IParticipantInfoCollector collector) {
		IReportInfo info = collector.createInfo(msg, type);
		collector.addInfo(info);
	}

	public void message(String msg, String title, String type,
			IAdvisorInfoCollector collector) {
		IAdvisorInfo info = collector.createProblemInfo(msg, title, type);
		collector.addInfo(info);
	}

	public void message(Exception ex, IAdvisorInfoCollector collector) {
		IAdvisorInfo info = collector.createExceptionInfo(ex.getMessage(), ex);
		collector.addInfo(info);

	}

	public static Object getAttribute(IWorkItem workItem,
			IWorkItemServer workItemService, String attributeName,
			IProgressMonitor monitor) throws TeamRepositoryException {
		Object value = null;

		IAttribute attribute = workItemService.findAttribute(
				workItem.getProjectArea(), attributeName, monitor);
		if (attribute == null || workItem.hasAttribute(attribute) == false) {
			// Attribute not found
			return null;
		}

		if (AttributeTypes.isEnumerationAttributeType(attribute
				.getAttributeType())) {
			Identifier id = (Identifier) workItem.getValue(attribute);

			IEnumeration enumeration = workItemService.resolveEnumeration(
					attribute, monitor);
			List<ILiteral> literals = enumeration.getEnumerationLiterals();
			for (ILiteral literal : literals) {
				if (literal.getIdentifier2().getStringIdentifier()
						.equals(id.getStringIdentifier())) {
					value = literal.getIdentifier2().getStringIdentifier();
					break;
				}
			}
		}
		return value;
	}


	public IWorkItem getParent(ISaveParameter saveParameter,
			IRepositoryItemService repository) throws ApplicationException {
		try {
			IWorkItemReferences references = saveParameter.getNewReferences();
			List<IReference> parentReferences = references
					.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
			IReference parentReference = null;
			IWorkItem workItemParent = null;
			if (parentReferences != null && parentReferences.size() > 0) {
				parentReference = parentReferences.get(0);
				IWorkItemHandle wiParentHandle = (IWorkItemHandle) parentReference
						.resolve();

				workItemParent = (IWorkItem) repository.fetchItem(
						wiParentHandle,
						IWorkItem.FULL_PROFILE.getPropertiesArray());

			}
			return workItemParent;
		} catch (TeamRepositoryException e) {
			throw new ApplicationException("Erro ao obter o pai", e);
		}

	}

}
