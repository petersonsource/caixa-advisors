package br.gov.caixa.rtc.advisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.advisor.util.IAdvisorsDefinitions;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
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
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

public class RecoverPhysicalLocation extends AbstractRTCService implements IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";

//    private static final String MOCK_CONTRIBUTOR_ID = "f540022";

    private IRepositoryItemService repositoryService = null;
    private IWorkItemServer workItemService = null;
//    private IProgressMonitor monitor = null;

    private ParserWorkItemDto wiParser;
    private WorkItemDto wiDto;

    private String wiType = null;

    private IWorkItem workItem = null;
    private IDUserRTC userRTC;
    private IContributor contributor = null;
    private Identifier<?> idTipoCentroCusto = null;
    private IAttribute tipoCentroCustoPai = null;
    private IAttribute paiDMSCentralizado  = null;
    private LdapConnection ldapConnection;
    private UserLdap userLdap;
    private IWorkItem workItemParent = null;
    private  Map<String, IWorkItem> mapHierarquiaItens = null;

    @Override
    public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	if (!(saveParameter.getNewState() instanceof IWorkItem)) {
	    return;
	}

	/**
	 * the code below to prevent the code from recursive updates
	 */
	if (saveParameter.getAdditionalSaveParameters().contains(IAdvisorsDefinitions.RECUPERA_CENTRO_CUSTO)) {
	    return;
	}

	workItem = (IWorkItem) saveParameter.getNewState().getWorkingCopy();

	// Inicializando os serviços que serão utilizados
	repositoryService = getService(IRepositoryItemService.class);
	workItemService = getService(IWorkItemServer.class);

	wiType = workItem.getWorkItemType();
	IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
	if (listConfigElements != null) {
	    for (IProcessConfigurationElement configElement : listConfigElements) {
		wiParser = new ParserWorkItemDto();
		wiDto = wiParser.parser(configElement);

		if (!wiType.equals(wiDto.getType())) {
		    continue;
		}
		try {
		    limparVariaveis();

		    // Campo Responsavel Centro de custo
		    IAttribute responsavelCentroCusto = (IAttribute) workItemService.findAttribute(workItem.getProjectArea(), wiDto.getQualified(), monitor).getWorkingCopy();
//		    if (!workItem.hasAttribute(responsavelCentroCusto)) {
//			continue;
//		    }

		    // Campo Codigo Centro de custo
		    IAttribute codigoCentroCusto = (IAttribute) (workItemService.findAttribute(workItem.getProjectArea(), wiDto.getAttribute(), monitor).getWorkingCopy());
//		    if (!workItem.hasAttribute(codigoCentroCusto)) {
//			continue;
//		    }

		    IContributorHandle handleResponsavelCentroCusto = (IContributorHandle) workItem.getValue(responsavelCentroCusto);
		    userRTC = new IDUserRTC(handleResponsavelCentroCusto, repositoryService);
		    IAuditableCommon auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
		    IContributor defaultValueCont = (IContributor) responsavelCentroCusto.getDefaultValue(auditableCommon, workItem, monitor);
		    IContributor nullValueContr = (IContributor) responsavelCentroCusto.getNullValue(auditableCommon, monitor);

		    // Recuperando lista informações do Pai
		    IWorkItemReferences references = saveParameter.getNewReferences();
		    List<IReference> parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
		    /**
		     * Valida se o item de trabalho tem pai
		     * 
		     * */
		    if (wiDto.isValidateParent() && parentReferences != null && !parentReferences.isEmpty()) {
			mapHierarquiaItens = new HashMap<String, IWorkItem>();
			mapHierarquiaItens.put("filho", workItem);
			
			for (IReference parent : parentReferences) {
			    IWorkItemHandle wiParentHandle = (IWorkItemHandle) parent.resolve();
			    workItemParent = (IWorkItem) (repositoryService.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy());
			    mapHierarquiaItens.put("pai", workItemParent);

			    if (!workItemParent.getWorkItemType().equalsIgnoreCase(wiDto.getParentWIType())) {
				
				IWorkItemReferences referencesGrandpa = workItemService.resolveWorkItemReferences(workItemParent, monitor);
				List<IReference> grandpaReferences = referencesGrandpa.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
				if (grandpaReferences != null && !grandpaReferences.isEmpty()) {
				    for (IReference grandpa : grandpaReferences) {
					IWorkItemHandle wiGrandpaHandle = (IWorkItemHandle) grandpa.resolve();
					IWorkItem workItemGrandpa = (IWorkItem) (repositoryService.fetchItem(wiGrandpaHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy());

					if (workItemGrandpa.getWorkItemType().equalsIgnoreCase(wiDto.getParentWIType())) {
					    mapHierarquiaItens.put("avo", workItemGrandpa);
					    workItemParent = null;
					    workItemParent = workItemGrandpa;
					}
				    }
				}
			    }

			    tipoCentroCustoPai = workItemService.findAttribute(workItemParent.getProjectArea(), wiDto.getIdTipoCentroCusto(), monitor);
			    paiDMSCentralizado = workItemService.findAttribute(workItem.getProjectArea(), wiDto.getParentAttr(), monitor);
			    
			    if (workItemParent != null && paiPossuiAtributo() && centroCustoPaiCentralizado(workItemParent)) {
				String nomeCategoria = workItemService.resolveHierarchicalName(workItemParent.getCategory(), monitor).trim();
				if ((boolean)paiDMSCentralizado.getValue(auditableCommon, workItem, monitor)) {
				    workItem.setValue(codigoCentroCusto, getCodCentroCusto(nomeCategoria));
				    workItem.setValue(responsavelCentroCusto, nullValueContr);
				}
				return;
			    }
			}
		    }

		    if (handleResponsavelCentroCusto != null) {
			contributor = userRTC.getUser(handleResponsavelCentroCusto);
		    }

		    if ((contributor == null || contributor.getItemId().equals(defaultValueCont.getItemId()) || contributor.getItemId().equals(nullValueContr.getItemId()))) {
			if (workItem.isNewItem()) {
			    contributor = (IContributor) repositoryService.fetchItem(workItemService.getAuditableCommon().getUser(), IRepositoryItemService.COMPLETE);
			} else {
			    contributor = userRTC.getUser(workItem.getCreator());
			}
		    }
		    ldapConnection = new LdapConnection();

		    userLdap = ldapConnection.recoverPhysicalLocationUserbyID(contributor.getUserId());
//		    userLdap = ldapConnection.recoverPhysicalLocationUserbyID(MOCK_CONTRIBUTOR_ID);

		    if (userLdap == null) {
			message(Messages.MSG_ERRO_USUARIO_NAO_FUNCIONARIO_RESPONSAVEL_CUSTO, ERROR_MESSAGE_TYPE, collector);
			return;
		    }
		    String centroCusto = userLdap.getSiglaNomeLotacao() + userLdap.getLotacaoAdministrativa();
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

    private boolean paiPossuiAtributo() throws TeamRepositoryException  {
	if (tipoCentroCustoPai != null) {
	    return workItemParent.hasAttribute(tipoCentroCustoPai);
	}
	return false;
    }

    private void limparVariaveis() {
	userRTC = null;
	contributor = null;
	ldapConnection = null;
	userLdap = null;

    }

    private boolean centroCustoPaiCentralizado(IWorkItem wiTemp) throws TeamRepositoryException {
	idTipoCentroCusto = (Identifier<?>) workItemParent.getValue(tipoCentroCustoPai);
	if (idTipoCentroCusto.getStringIdentifier().equalsIgnoreCase(wiDto.getValueTipoCentroCusto())&& paiDMSCentralizado!= null ) {
	    workItem.setValue(paiDMSCentralizado, true);
	    return true;
	}
	workItem.setValue(paiDMSCentralizado, false);
	return false;
    }

    private String getCodCentroCusto(String codHerdado) {
	String[] lista = codHerdado.split("/");
	int lastIndex = lista.length - 1;
	
	return lista[lastIndex];
    }

}
