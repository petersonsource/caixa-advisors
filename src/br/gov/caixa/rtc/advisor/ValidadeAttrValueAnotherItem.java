package br.gov.caixa.rtc.advisor;

import java.math.BigDecimal;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.alcadas.Alcada;
import br.gov.caixa.rtc.alcadas.AlcadasFactory;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.AlcadaException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;
import br.gov.caixa.rtc.util.NumberFormater;
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
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.service.IWorkItemServer;

public class ValidadeAttrValueAnotherItem extends AbstractRTCService implements
		IOperationAdvisor {

	private static final String ERROR_MESSAGE_TYPE = "error";

	@SuppressWarnings("unchecked")
	public void run(AdvisableOperation operation,
			IProcessConfigurationElement advisorConfiguration,
			IAdvisorInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {
		// TODO Auto-generated method stub
		Alcada alcada = null;
		IWorkItem workItemParent = null;
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
		ValidationStatusChange regraStatusAlterado =new ValidationStatusChange((IWorkItem) auditable,(IWorkItem) oldAuditable);
		if (!regraStatusAlterado.isValid()) {
			return;
		}
		try {
			IProcessConfigurationElement[] configElements = advisorConfiguration
					.getChildren();
			if (configElements != null) {
				IRepositoryItemService repositoryService = getService(IRepositoryItemService.class);
				IWorkItemServer workItemService = getService(IWorkItemServer.class);
				IRepositoryItemService repository = getService(IRepositoryItemService.class);

				String wiType = workItem.getWorkItemType();

				for (IProcessConfigurationElement configElement : configElements) {
					ParserWorkItemDto parser = new ParserWorkItemDto();
					WorkItemDto dto= parser.parser(configElement);
					// CHECK
					if (!wiType.equals(dto.getType())) {
						continue;
					}
					if (!dto.getState().equalsIgnoreCase(workItem.getState2()
							.getStringIdentifier())) {
						dto.setState(dto.getState().replace("s", ""));
						if (!dto.getState().equalsIgnoreCase(workItem
								.getState2().getStringIdentifier())) {
							continue;
						}
					}
					Object idValue = getAttribute(workItem, workItemService,
							dto.getQualified(), monitor);
					if (!idValue.equals(dto.getQualifiedValue())) {
						continue;
					}
					workItemParent = getParent(saveParameter, repository);
					
					if (workItemParent==null || !workItemParent
							.getWorkItemType().equals(dto.getParentWIType())) {
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
						
						alcada = AlcadasFactory.getInstance().getAlacada(
								userLdap.getIdCargo());
						if (dto.getParentAttr() != null) {
							IAttribute changeAttribute = workItemService
									.findAttribute(
											workItemParent.getProjectArea(),
											dto.getParentAttr(), monitor);
							String valorString = (String) workItemParent
									.getValue(changeAttribute);

							BigDecimal valorProjeto = NumberFormater
									.parserStringToNumber(valorString);
							
							alcada.liberaAprovacao(valorProjeto, userLdap, valorString);							

						}

					}

				}
			}
		} catch (ApplicationException e) {
			message(e.getMessage()
					+ " Verificar o atributo relacionado ao custo.",
					ERROR_MESSAGE_TYPE, collector);
		} catch (TeamRepositoryException e) {
			message(e, collector);
		} catch (AlcadaException e) {
			message(e.getMessage(),	Messages.MSG_ERRO_TITLE_VALOR_SUPERIOR,	ERROR_MESSAGE_TYPE, collector);
		} catch (Exception e) {
			message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	}

}
