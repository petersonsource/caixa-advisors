package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IRole;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.process.service.IServerProcess;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.service.IWorkItemServer;

public class ValidadeRoleAttrValue extends AbstractRTCService implements
		IOperationAdvisor {

	private static final String ERROR_MESSAGE_TYPE = "error";

	public void run(AdvisableOperation operation,
			IProcessConfigurationElement advisorConfiguration,
			IAdvisorInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {

		boolean roleAccept = false;
		String roles[] = null;

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
		IWorkItem oldWorkItem = (IWorkItem) oldAuditable;
		IWorkItemServer workItemService = getService(IWorkItemServer.class);

		try {
			IProcessConfigurationElement[] configElements = advisorConfiguration
					.getChildren();
			if (configElements != null) {
				String wiType = workItem.getWorkItemType();
				for (IProcessConfigurationElement configElement : configElements) {

					// INPUT PARAMETER
					ParserWorkItemDto parser = new ParserWorkItemDto();
					WorkItemDto dto = parser.parser(configElement);
					// END INPUT PARAMETER

					// CHECK
					if (!wiType.equals(dto.getType())) {
						continue;
					}

					// caso seja item novo
					Object idValue = getAttribute(workItem, workItemService,
							dto.getQualified(), monitor);
					if (oldWorkItem != null) {
						Object idOldValue = getAttribute(oldWorkItem,
								workItemService, dto.getQualified(), monitor);

						if (idValue.equals(idOldValue)) {
							continue;
						}
					}

					// verifica se o valor foi alterado

					if (!(idValue.equals(dto.getQualifiedValue()))) {
						continue;
					}

					// verifica a possibilidade de mais de um perfil para o
					// atributo
					if (dto.getIdRole() != null) {
						roles = dto.getIdRole().split(";");
					}

					// FIM Check

					for (int r = 0; r < roles.length; r++) {

						IContributorHandle loggedIn = this
								.getAuthenticatedContributor();

						IProcessServerService processService = getService(IProcessServerService.class);
						IProcessArea processArea = operation.getProcessArea();
						IServerProcess serverProcess = processService
								.getServerProcess(processArea);
						IRole[] memberRoles = serverProcess
								.getContributorRoles(loggedIn, processArea);

						for (int i = 0; i < memberRoles.length; i++) {
							if (memberRoles[i].getId().equals(roles[r])) {
								roleAccept = true;
								break;
							}
						}
					}
					if (!roleAccept) {
						String alert = Messages
								.getStringWithArgs(Messages.MSG_ERRO_PERFIL);
						throw new ApplicationException(alert);
					}
				}

			}
		} catch (ApplicationException e) {
			message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		} catch (Exception e) {
			message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	}
}
