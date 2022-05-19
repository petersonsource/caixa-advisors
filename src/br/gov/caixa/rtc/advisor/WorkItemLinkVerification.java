package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class WorkItemLinkVerification extends AbstractRTCService implements
	IOperationAdvisor {

    String ERROR_MESSAGE_TYPE = "error";
    IAdvisorInfo createProblemInfo;
    String configChildrens[] = null;
    String wiType;

    @Override
    public void run(AdvisableOperation operation,
	    IProcessConfigurationElement advisorConfiguration,
	    IAdvisorInfoCollector collector, IProgressMonitor monitor)
	    throws TeamRepositoryException {

	Object data = operation.getOperationData();

	if (data instanceof ISaveParameter) {
	    ISaveParameter saveParameter = (ISaveParameter) data;
	    IAuditable auditable = saveParameter.getNewState();
	    if (auditable instanceof IWorkItem) {
		IAuditableCommon iac = saveParameter
			.getSaveOperationParameter().getAuditableCommon();
		IWorkItem workItem = (IWorkItem) auditable;

		List<IReference> parentReference = saveParameter
			.getNewReferences().getCreatedReferences(
				WorkItemEndPoints.PARENT_WORK_ITEM);
		List<IReference> childReference = saveParameter
			.getNewReferences().getCreatedReferences(
				WorkItemEndPoints.CHILD_WORK_ITEMS);

		// Verifica se está incluíndo item como pai ou como
		// filho
		if (parentReference.isEmpty() && childReference.isEmpty()) {
		    return;
		}

		try {
		    IProcessConfigurationElement[] configElements = advisorConfiguration
			    .getChildren();
		    if (configElements.length > 0) {

			wiType = workItem.getWorkItemType();
			String configTypeParent = null;
			String configTypeChildren = null;
			String parentType;
			List<IReference> childReferences = null;
			List<IReference> parentReferences = null;
			IWorkItem childWI = null;
			IWorkItem parentWI = null;
			IReference parentWIReference;
			int dentroRegra = 0;
			int dentroRegraChild = 0;
			int dentroRegraParent = 0;
			int sumDentroRegra;
			int manyChildrens = 0;

			for (IProcessConfigurationElement configElement : configElements) {
			    configTypeParent = configElement
				    .getAttribute("typeParent");
			    configTypeChildren = configElement
				    .getAttribute("typeChildren");

			    // Separa o id de cada filho
			    if (configTypeChildren != null) {
				configChildrens = configTypeChildren.split(",");
			    }

			    // Verifica se há a criação de filhos e pai
			    // simultaneamente
			    if (!childReference.isEmpty()
				    && !parentReference.isEmpty()) {
				dentroRegraChild = verifyChildByParent(
					saveParameter, dentroRegra,
					childReferences, configChildrens, iac,
					childWI, manyChildrens);

				// verifica o tipo do item pai
				if (dentroRegraChild > 0) {
				    parentWIReference = parentReference.get(0);
				    parentWI = iac.resolveAuditable(
					    (IWorkItemHandle) parentWIReference
						    .resolve(),
					    IWorkItem.FULL_PROFILE, null);
				    parentType = parentWI.getWorkItemType();

				    dentroRegraParent = verifyParentByWorkItem(
					    dentroRegra, configElements,
					    configTypeParent,
					    configTypeChildren, parentType);

				}

				sumDentroRegra = dentroRegraChild
					+ dentroRegraParent;

				if (sumDentroRegra >= 2) {
				    dentroRegra = 1;
				}

			    }

			    // Verifica se o item é um pai e esta nos parametros
			    // para incluir filhos
			    else if (!childReference.isEmpty()
				    && wiType.equals(configTypeParent)) {

				dentroRegra = verifyChildByParent(
					saveParameter, dentroRegra,
					childReferences, configChildrens, iac,
					childWI, manyChildrens);

				// Verifica o inverso, se o filho incluir um pai
			    } else {
				dentroRegra = verifyParentByChild(
					saveParameter, iac, wiType,
					configTypeParent, configChildrens,
					dentroRegra, parentReferences, parentWI);
			    }
			}
			if (dentroRegra < 1) {
			    message(Messages.MSG_ERRO_INVALID_LINK,
				    Messages.MSG_ERRO_INVALID_LINK_DETAIL,
				    collector);
			    return;
			}
		    } else {
			message(Messages.MSG_ERRO_NO_LINKS,
				Messages.MSG_ERRO_NO_LINKS_DETAIL, collector);
			return;
		    }
		} catch (Exception e) {
		    message(Messages.MSG_ERRO_UNEXPECTED_LINKS, e.getMessage(),
			    collector);
		    throw new TeamRepositoryException(e);
		}
	    }
	}
    }

    private int verifyParentByChild(ISaveParameter saveParameter,
	    IAuditableCommon iac, String wiType, String configTypeParent,
	    String[] configChildrens, int dentroRegra,
	    List<IReference> parentReferences, IWorkItem parentWI)
	    throws TeamRepositoryException {

	for (int i = 0; i < configChildrens.length; i++) {
	    if (wiType.equals(configChildrens[i])
		    && saveParameter.getNewReferences().hasReferences(
			    (WorkItemEndPoints.PARENT_WORK_ITEM))) {

		parentReferences = saveParameter.getNewReferences()
			.getCreatedReferences(
				WorkItemEndPoints.PARENT_WORK_ITEM);
		if (!parentReferences.isEmpty()) {
		    IReference parentReference = parentReferences.get(0);
		    parentWI = iac.resolveAuditable(
			    (IWorkItemHandle) parentReference.resolve(),
			    IWorkItem.FULL_PROFILE, null);
		    if (configTypeParent.equals(parentWI.getWorkItemType())) {
			dentroRegra++;
		    }
		} else {
		    break;
		}
	    }
	}
	return dentroRegra;
    }

    private int verifyChildByParent(ISaveParameter saveParameter,
	    int dentroRegra, List<IReference> childReferences,
	    String[] configChildrens, IAuditableCommon iac, IWorkItem childWI,
	    int manyChildrens) throws TeamRepositoryException {

	childReferences = saveParameter.getNewReferences()
		.getCreatedReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);

	for (int i = 0; i < configChildrens.length; i++) {

	    for (IReference childReference : childReferences) {

		childWI = iac.resolveAuditable(
			(IWorkItemHandle) childReference.resolve(),
			IWorkItem.FULL_PROFILE, null);
		if (configChildrens[i].equals(childWI.getWorkItemType())) {
		    manyChildrens++;
		}
	    }
	}
	if (childReferences.size() == manyChildrens) {
	    dentroRegra++;
	}
	return dentroRegra;
    }

    private int verifyParentByWorkItem(int dentroRegra,
	    IProcessConfigurationElement[] configElements,
	    String configTypeParent, String configTypeChildren,
	    String parentType) {

	for (IProcessConfigurationElement configElement : configElements) {
	    configTypeParent = configElement.getAttribute("typeParent");
	    if (parentType.equals(configTypeParent)) {
		configTypeChildren = configElement.getAttribute("typeChildren");

		if (configTypeChildren != null) {
		    configChildrens = configTypeChildren.split(",");

		    for (int i = 0; i < configChildrens.length; i++) {
			if (configChildrens[i].equals(wiType)) {
			    dentroRegra++;
			    break;
			}
		    }
		} else {
		    break;
		}
	    }
	}
	return dentroRegra;
    }
}
