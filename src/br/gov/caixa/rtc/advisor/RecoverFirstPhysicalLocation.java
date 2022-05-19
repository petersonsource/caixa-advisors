package br.gov.caixa.rtc.advisor;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.advisor.util.IAdvisorsDefinitions;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.service.IWorkItemServer;

public class RecoverFirstPhysicalLocation extends AbstractRTCService implements
	IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";

    private IRepositoryItemService repositoryService = null;
    private IWorkItemServer workItemService = null;

    private ParserWorkItemDto wiParser;
    private WorkItemDto wiDto;

    private String wiType = null;

    private IWorkItem workItem = null;
    private IDUserRTC userRTC;
    private IContributor contributor = null;
    private LdapConnection ldapConnection;
    private UserLdap userLdap;

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
	if (!(saveParameter.getNewState() instanceof IWorkItem)) {
	    return;
	}

	if (saveParameter.getAdditionalSaveParameters().contains(
		IAdvisorsDefinitions.RECUPERA_CENTRO_CUSTO_UNICO)) {
	    return;
	}

	workItem = (IWorkItem) saveParameter.getNewState().getWorkingCopy();

	// Inicializando os serviços que serão utilizados
	repositoryService = getService(IRepositoryItemService.class);
	workItemService = getService(IWorkItemServer.class);

	wiType = workItem.getWorkItemType();
	IProcessConfigurationElement[] listConfigElements = advisorConfiguration
		.getChildren();
	if (listConfigElements != null) {
	    for (IProcessConfigurationElement configElement : listConfigElements) {
		wiParser = new ParserWorkItemDto();
		wiDto = wiParser.parser(configElement);

		if (!wiType.equals(wiDto.getType())) {
		    continue;
		}

		IAttribute attributeCode = workItemService.findAttribute(
			workItem.getProjectArea(), wiDto.getAttribute(),
			monitor);
		if (attributeCode != null
			&& workItem.hasAttribute(attributeCode) == true) {
		    Object codigoValue = workItem.getValue(attributeCode);
		    if (codigoValue.toString() != "") {
			continue;
		    }
		} else {
		    message(Messages.MSG_ERRO_ATRIBUTO_NAO_SINCRONIZADO,
			    ERROR_MESSAGE_TYPE, collector);
		    return;
		}

		try {
		    limparVariaveis();

		    IAttribute responsavelCentroCusto = (IAttribute) workItemService
			    .findAttribute(workItem.getProjectArea(),
				    wiDto.getQualified(), monitor)
			    .getWorkingCopy();

		    // Campo Codigo Centro de custo
		    IAttribute codigoCentroCusto = (IAttribute) (workItemService
			    .findAttribute(workItem.getProjectArea(),
				    wiDto.getAttribute(), monitor)
			    .getWorkingCopy());

		    IContributorHandle handleResponsavelCentroCusto = (IContributorHandle) workItem
			    .getValue(responsavelCentroCusto);
		    userRTC = new IDUserRTC(handleResponsavelCentroCusto,
			    repositoryService);
		    IAuditableCommon auditableCommon = saveParameter
			    .getSaveOperationParameter().getAuditableCommon();
		    IContributor defaultValueCont = (IContributor) responsavelCentroCusto
			    .getDefaultValue(auditableCommon, workItem, monitor);
		    IContributor nullValueContr = (IContributor) responsavelCentroCusto
			    .getNullValue(auditableCommon, monitor);

		    if (handleResponsavelCentroCusto != null) {
			contributor = userRTC
				.getUser(handleResponsavelCentroCusto);
		    }

		    if ((contributor == null
			    || contributor.getItemId().equals(
				    defaultValueCont.getItemId()) || contributor
			    .getItemId().equals(nullValueContr.getItemId()))) {
			if (workItem.isNewItem()) {
			    contributor = (IContributor) repositoryService
				    .fetchItem(workItemService
					    .getAuditableCommon().getUser(),
					    IRepositoryItemService.COMPLETE);
			} else {
			    contributor = userRTC
				    .getUser(workItem.getCreator());
			}
		    }
		    ldapConnection = new LdapConnection();

		    userLdap = ldapConnection
			    .recoverPhysicalLocationUserbyID(contributor
				    .getUserId());

		    if (userLdap == null) {
			message(Messages.MSG_ERRO_USUARIO_NAO_FUNCIONARIO_RESPONSAVEL_CUSTO,
				ERROR_MESSAGE_TYPE, collector);
			return;
		    }
		    String centroCusto;
		    if (userLdap.getLotacaoAdministrativa().isEmpty()
			    || userLdap.getSiglaNomeLotacao().isEmpty()) {
			centroCusto = "Sem valor";
		    } else {
			centroCusto = userLdap.getSiglaNomeLotacao()
				+ userLdap.getLotacaoAdministrativa();
		    }

		    workItem.setValue(codigoCentroCusto, centroCusto);
		    workItem.setValue(responsavelCentroCusto, contributor);

		} catch (ApplicationException e) {
		    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		    e.printStackTrace();
		} catch (TeamRepositoryException e) {
		    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		    e.printStackTrace();
		} catch (Exception e) {
		    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	    }
	}
    }

    private void limparVariaveis() {
	userRTC = null;
	contributor = null;
	ldapConnection = null;
	userLdap = null;

    }

}
