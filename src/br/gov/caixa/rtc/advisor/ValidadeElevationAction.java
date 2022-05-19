package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.AlcadaException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;
import br.gov.caixa.rtc.validation.ValidationStatusChange;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IWorkItem;

public class ValidadeElevationAction extends AbstractRTCService implements
		IOperationAdvisor {

	private static final String ERROR_MESSAGE_TYPE = "error";

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
					// INPUT PARAMETER
					ParserWorkItemDto parser = new ParserWorkItemDto();
					WorkItemDto dto = parser.parser(configElement);
					// END INPUT PARAMETER
					// CHECK
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

					// FIM Check
					// Consulta do Usuário no AD
					IDUserRTC userRTC = new IDUserRTC(this, repositoryService);
					IContributor contributor = userRTC.getUserLogado();
					LdapConnection ldapConnection = new LdapConnection();
					UserLdap userLdap = ldapConnection.findUserById(contributor
							.getUserId());
					if (userLdap == null) {
						message(Messages.ACAO_NAO_PERMITIDA,
								Messages.MSG_ERRO_USUARIO_NAO_FUNCIONARIO,
								collector);
					} else {

						if (userLdap.getIdCargo().equals(dto.getIdCargo())) {
							continue;
						} else {
							String alert = Messages.getStringWithArgs(
									Messages.MSG_ERRO_ALCADA_STATUS,
									userLdap.getNomeCargo());
							throw new AlcadaException(alert);
						}

					}

				}
			}
		} catch (ApplicationException e) {
			message(e.getMessage(), Messages.MSG_ERRO_LIBERACAO_TI_GESTOR,
					ERROR_MESSAGE_TYPE, collector);
		} catch (AlcadaException e) {
			message(e.getMessage(), Messages.MSG_ERRO_LIBERACAO_TI_GESTOR,
					ERROR_MESSAGE_TYPE, collector);
		} catch (Exception e) {
			message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	}

}
