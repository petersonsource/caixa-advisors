package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.validation.ValidationStatusChange;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;

public class ValidadeUserAction extends AbstractRTCService implements
	IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";

    @Override
    public void run(AdvisableOperation operation,
	    IProcessConfigurationElement advisorConfiguration,
	    IAdvisorInfoCollector collector, IProgressMonitor monitor)
	    throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	IAuditable auditable = saveParameter.getNewState();
	IAuditable oldAuditable = saveParameter.getOldState();
	if (!(auditable instanceof IWorkItem)
		&& !(oldAuditable instanceof IWorkItem)) {
	    return;
	}
	IWorkItem workItem = (IWorkItem) auditable;
	ValidationStatusChange regraStatusAlterado = new ValidationStatusChange(
		(IWorkItem) auditable, (IWorkItem) oldAuditable);
	if (!regraStatusAlterado.isValid()) {
	    return;
	}
	try {
	    IProcessConfigurationElement[] configElements = advisorConfiguration
		    .getChildren();
	    if (configElements != null) {
		IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
		String wiType = workItem.getWorkItemType();
		for (IProcessConfigurationElement configElement : configElements) {
		    ParserWorkItemDto parser = new ParserWorkItemDto();
		    WorkItemDto dto = parser.parser(configElement);
		    if (!wiType.equals(dto.getType())) {
			continue;
		    }
		    if (!dto.getState().equalsIgnoreCase(
			    workItem.getState2().getStringIdentifier())) {
			dto.setState(dto.getState().replace("s", ""));
			if (!dto.getState().equalsIgnoreCase(
				workItem.getState2().getStringIdentifier())) {
			    continue;
			}
		    }
		    IDUserRTC userRTC = new IDUserRTC(this, repositoryService);
		    if (!dto.getContributor().equalsIgnoreCase(
			    userRTC.getUserLogado().getName())) {
			String msg = new String(
				"Esta ação é de responsabilidade do serviço relacionado à integração com o ITSM."
					.getBytes(), "UTF-8");
			IAdvisorInfo info = collector.createProblemInfo(
				"Erro!!! ", msg, ERROR_MESSAGE_TYPE);
			collector.addInfo(info);
		    } else {
			continue;
		    }
		}
	    }
	} catch (Exception e) {
	    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	}
    }

}
