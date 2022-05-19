package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.alcadas.Alcada;
import br.gov.caixa.rtc.alcadas.AlcadasFactoryTermoLiberacao;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.AlcadaException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;

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
import com.ibm.team.workitem.service.IWorkItemServer;

public class ValidadeElevation extends AbstractRTCService implements
		IOperationAdvisor {

	private static final String ERROR_MESSAGE_TYPE = "error";

	public void run(AdvisableOperation operation,
			IProcessConfigurationElement advisorConfiguration,
			IAdvisorInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {

		Alcada alcada = null;
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

		try {
			IProcessConfigurationElement[] configElements = advisorConfiguration
					.getChildren();
			if (configElements != null) {
				IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
				IWorkItemServer workItemService = getService(IWorkItemServer.class);

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

					// verifica se o valor foi alterado
					Object idValue = getAttribute(workItem, workItemService,
							dto.getQualified(), monitor);

					//caso seja item novo
					if (oldWorkItem != null && idValue != null) {
						Object idOldValue = getAttribute(oldWorkItem,
								workItemService, dto.getQualified(), monitor);

						if (!idValue.equals(idOldValue)) {
							if (!idValue.equals(dto.getQualifiedValue())) {
								continue;
							}
						} else {
							continue;
						}
					}else{
						continue;
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

						if (dto.getGrupoAlcada() != null) {
							alcada = AlcadasFactoryTermoLiberacao.getInstance()
									.getGrupo(userLdap.getIdCargo());

							int numGrupoAlcada = Integer.parseInt(dto
									.getGrupoAlcada());
							alcada.liberaTermo(numGrupoAlcada,
									dto.getQualified(), userLdap);
						}

					}

				}
			}
		} catch (ApplicationException e) {
			message(e.getMessage(), Messages.MSG_ERRO_LIBERACAO_TI_GESTOR,
					ERROR_MESSAGE_TYPE, collector);
		} catch (TeamRepositoryException e) {
			message(e, collector);
		} catch (AlcadaException e) {
			message(e.getMessage(), Messages.MSG_ERRO_LIBERACAO_TI_GESTOR,
					ERROR_MESSAGE_TYPE, collector);
		} catch (Exception e) {
			message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	}

}
