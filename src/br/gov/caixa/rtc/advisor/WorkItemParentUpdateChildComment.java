package br.gov.caixa.rtc.advisor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.advisor.util.IAdvisorsDefinitions;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.LdapConnection;
import br.gov.caixa.rtc.ldap.UserLdap;
import br.gov.caixa.rtc.util.TipoAcaoLink;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.common.service.ILinkService;
import com.ibm.team.links.service.ILinkServiceLibrary;
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
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemParentUpdateChildComment extends AbstractRTCService implements IOperationAdvisor {

    private IWorkItem workItemAntigo = null;
    private IWorkItem workItem = null;

    private static final String ERROR_MESSAGE_TYPE = "error";

    // private static final String MOCK_CONTRIBUTOR_ID = "f540022";

    private IContributor usuarioLogado = null;
    private UserLdap userLdap;
    private IDUserRTC userRTC;

    private IRepositoryItemService repositoryService = null;
    private IWorkItemServer workItemService = null;
    private IProgressMonitor monitor = null;
    private IAuditableCommon auditableCommon = null;
    private ILinkService linkService = null;
    private IReferenceFactory refFactory = null;
    private ILinkServiceLibrary linkServiceLibrary = null;
    private ISaveParameter saveParameter = null;
    private IAdvisorInfoCollector collector = null;

    private ParserWorkItemDto wiParser;
    private WorkItemDto wiDto;

    private String wiType = null;

    private List<IReference> filhosExistentes = null;
    private List<IReference> filhosNovos = null;
    private List<IReference> filhosExcluidos = null;

    private IAttribute atributoPai = null;
    private List<IAttribute> listaAtributosPai = null;
    private IAttribute atributoCentralizadaDMS = null;
    private IWorkItem workItemFilho = null;
    private ILink link = null;

    private IAttribute centroCustoDMS = null;
    private IAttribute respCentroCusto = null;
    private String nomeCategoriaPai = null;

    private boolean atualizaAtributo = false;
    private boolean atributoPaiModificado = false;
    private boolean novoItem = false;

    @Override
    public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	saveParameter = (ISaveParameter) data;
	if (!(saveParameter.getNewState() instanceof IWorkItem || saveParameter.getOldState() instanceof IWorkItem)) {
	    return;
	}

	/**
	 * the code below to prevent the code from recursive updates
	 */
	if (saveParameter.getAdditionalSaveParameters().contains(IAdvisorsDefinitions.PARENT_UPDATE_CHILD_COMMENT)) {
	    return;
	}

	auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();

	workItemAntigo = (IWorkItem) saveParameter.getOldState();
	workItem = (IWorkItem) saveParameter.getNewState().getWorkingCopy();

	filhosNovos = saveParameter.getNewReferences().getCreatedReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
	filhosExistentes = saveParameter.getNewReferences().getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
	filhosExcluidos = saveParameter.getNewReferences().getDeletedReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);

	if (filhosExistentes.size() == 0 && filhosNovos.size() == 0 && filhosExcluidos.size() == 0) {
	    return;
	}

	inicializarServicos(monitor, collector);

	wiType = workItem.getWorkItemType();
	usuarioLogado = (IContributor) repositoryService.fetchItem(auditableCommon.getUser(), null);

	IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
	if (listConfigElements != null) {
	    for (IProcessConfigurationElement configElement : listConfigElements) {
		wiParser = new ParserWorkItemDto();
		wiDto = wiParser.parser(configElement);

		if (!wiType.equals(wiDto.getParentWIType())) {
		    continue;
		}

		nomeCategoriaPai = null;
		atributoPai = null;
		novoItem = false;
		atualizaAtributo = Boolean.valueOf(wiDto.getAttributeValue());

		try {

		    if (filhosNovos.size() != 0 && workItem.isNewItem()) {
			novoItem = true;
			verificarPaiModificado();
			atualizaFilhos(filhosNovos, TipoAcaoLink.NOVO);
		    } else if (verificarPaiModificado()) {

			if (filhosNovos.size() != 0) {
			    atualizaFilhos(filhosNovos, TipoAcaoLink.ADD);
			}

			if (filhosExistentes.size() != 0 || atributoPaiModificado) {
			    atualizaFilhos(filhosExistentes, TipoAcaoLink.UPDATE);
			}

			if (filhosExcluidos.size() != 0) {
			    atualizaFilhos(filhosExcluidos, TipoAcaoLink.DEL);
			}
		    }

		} catch (ApplicationException e) {
		    e.printStackTrace();
		} catch (TeamRepositoryException e) {
		    e.printStackTrace();
		} catch (Exception e) {
		    e.printStackTrace();
		}

	    }
	}

    }

    private void preencheCentroCustoCentralizado() throws TeamRepositoryException {
	respCentroCusto = (IAttribute) workItemService.findAttribute(workItemFilho.getProjectArea(), "responsavel", monitor).getWorkingCopy();
	centroCustoDMS = (IAttribute) (workItemService.findAttribute(workItemFilho.getProjectArea(), "centro_custo", monitor).getWorkingCopy());
	nomeCategoriaPai = workItemService.resolveHierarchicalName(workItem.getCategory(), monitor);
	workItemFilho.setValue(centroCustoDMS, formataCodCenttroCustoPai(nomeCategoriaPai));
	workItemFilho.setValue(respCentroCusto, respCentroCusto.getNullValue(auditableCommon, monitor));
    }

    private void preencheCentroCustoDescentralizado() throws TeamRepositoryException, ApplicationException {
	respCentroCusto = (IAttribute) workItemService.findAttribute(workItemFilho.getProjectArea(), "responsavel", monitor).getWorkingCopy();
	centroCustoDMS = (IAttribute) (workItemService.findAttribute(workItemFilho.getProjectArea(), "centro_custo", monitor).getWorkingCopy());

	IContributorHandle handleResponsavelCentroCusto = (IContributorHandle) workItemFilho.getValue(respCentroCusto);
	userRTC = new IDUserRTC(handleResponsavelCentroCusto, repositoryService);

	IContributor usuarioResponsavel = userRTC.getUser(handleResponsavelCentroCusto);
	IContributor defaultValueCont = (IContributor) respCentroCusto.getDefaultValue(auditableCommon, workItemFilho, monitor);
	IContributor nullValueContr = (IContributor) respCentroCusto.getNullValue(auditableCommon, monitor);

	if ((usuarioResponsavel == null || usuarioResponsavel.getItemId().equals(defaultValueCont.getItemId()) || usuarioResponsavel.getItemId().equals(nullValueContr.getItemId()))) {
	    if (workItemFilho.isNewItem()) {
		usuarioResponsavel = (IContributor) repositoryService.fetchItem(workItemService.getAuditableCommon().getUser(), IRepositoryItemService.COMPLETE);
	    } else {
		usuarioResponsavel = userRTC.getUser(workItemFilho.getCreator());
	    }
	}
	LdapConnection ldapConnection = new LdapConnection();

	userLdap = ldapConnection.recoverPhysicalLocationUserbyID(usuarioResponsavel.getUserId());
	// userLdap =
	// ldapConnection.recoverPhysicalLocationUserbyID(MOCK_CONTRIBUTOR_ID);

	if (userLdap == null) {
	    message(Messages.MSG_ERRO_USUARIO_NAO_FUNCIONARIO_RESPONSAVEL_CUSTO, ERROR_MESSAGE_TYPE, collector);
	    return;
	}
	String centroCusto = userLdap.getSiglaNomeLotacao() + userLdap.getLotacaoAdministrativa();
	workItemFilho.setValue(centroCustoDMS, centroCusto);
	workItemFilho.setValue(respCentroCusto, usuarioResponsavel);
    }

    private boolean verificarPaiModificado() throws TeamRepositoryException, ApplicationException {
	listaAtributosPai = new LinkedList<IAttribute>();
	for (String nomeAtributoPai : wiDto.getListaAtributosPaiVerificar()) {
	    if (atributoPai == null) {
		atributoPai = workItemService.findAttribute(workItem.getProjectArea(), nomeAtributoPai, monitor);
	    }
	    atributoPaiModificado = false;
	    IAttribute attribTemp = workItemService.findAttribute(workItem.getProjectArea(), nomeAtributoPai, monitor);
	    if (!workItem.hasAttribute(attribTemp)) {
		Messages.getStringWithArgs("Atributo configurado na lista do Advisor não existe no neste Item de Trabalho", "Advisor:Alteração atributo no Pai atualiza Filho");
	    }
	    listaAtributosPai.add(attribTemp);

	    if (!novoItem && !(workItem.getValue(attribTemp).toString().equalsIgnoreCase(workItemAntigo.getValue(attribTemp).toString()))) {
		atributoPaiModificado = true;
		return true;
	    }
	    if (filhosNovos.size() != 0 || filhosExcluidos.size() != 0 || novoItem) {
		return true;
	    }
	}
	return false;

    }

    private void inicializarServicos(IProgressMonitor monitor2, IAdvisorInfoCollector collector2) {
	monitor = monitor2;
	this.collector = collector2;
	repositoryService = getService(IRepositoryItemService.class);
	workItemService = getService(IWorkItemServer.class);
	refFactory = IReferenceFactory.INSTANCE;
	linkService = getService(ILinkService.class);
	linkServiceLibrary = (ILinkServiceLibrary) linkService.getServiceLibrary(ILinkServiceLibrary.class);

    }

    private void atualizaFilhos(List<IReference> listaFilhos, TipoAcaoLink tipoAcaoLinkEnum) throws TeamRepositoryException, ApplicationException {

	if (listaFilhos == null || listaFilhos.size() == 0) {
	    return;
	}
	for (IReference itemFilho : listaFilhos) {
	    IWorkItemHandle wiParentHandle = (IWorkItemHandle) itemFilho.resolve();
	    workItemFilho = (IWorkItem) repositoryService.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy();
	    if (!wiDto.getItensFilhos().contains(workItemFilho.getWorkItemType())) {
		atualizarAtributoNeto(tipoAcaoLinkEnum);
		atualizaAtributo = false;
	    }
	    workItemFilho = (IWorkItem) repositoryService.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy();
	    if (!novoItem && tipoAcaoLinkEnum.equals(TipoAcaoLink.UPDATE) && (filhosNovos.contains(itemFilho) && filhosExistentes.contains(itemFilho))) {
		return;
	    }

	    if (atualizaAtributo) {
		atributoCentralizadaDMS = workItemService.findAttribute(workItemFilho.getProjectArea(), wiDto.getAttribute(), monitor);
		if (!workItemFilho.hasCustomAttribute(atributoCentralizadaDMS)) {
		    continue;
		}

		atualizarAtributoFilho();
		adicionaComentarioFilho();
	    }

	    switch (tipoAcaoLinkEnum) {
	    case ADD:
		criaLinkPai();
		adicionaLink();
		salvaItemFilho();
		break;
	    case DEL:
		if (atualizaAtributo) {
		    workItemFilho.setValue(atributoCentralizadaDMS, false);
		}
		salvaItemFilho();
		break;
	    case UPDATE:
		salvaItemFilho();
		break;
	    case NOVO:
		if (atualizaAtributo) {
		    preencheCentroCustoCentralizado();
		}
		salvarFilhoPaiNovo();
		break;
	    default:
		break;
	    }
	}
    }

    private void atualizarAtributoNeto(TipoAcaoLink tipoAcaoNeto) throws TeamRepositoryException, ApplicationException {
	IWorkItemReferences listaReferencias = workItemService.resolveWorkItemReferences(workItemFilho, null);
	List<IReference> netos = listaReferencias.getReferences(WorkItemEndPoints.CHILD_WORK_ITEMS);
	if (netos != null && netos.size() != 0) {
	    for (IReference itemNeto : netos) {
		IWorkItem workItemNeto = null;
		IWorkItemHandle wiGrapaHandle = (IWorkItemHandle) itemNeto.resolve();
		workItemNeto = (IWorkItem) repositoryService.fetchItem(wiGrapaHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy();
		if (!wiDto.getItensFilhos().contains(workItemNeto.getWorkItemType())) {
		    return;
		}
		IAttribute atributoAvo = atributoPai;
		if (atributoAvo != null) {
		    Identifier<?> idTipoCentroCusto = (Identifier<?>) workItem.getValue(atributoAvo);
		    atributoCentralizadaDMS = workItemService.findAttribute(workItemNeto.getProjectArea(), wiDto.getAttribute(), monitor);
		    if (idTipoCentroCusto.getStringIdentifier().equalsIgnoreCase(wiDto.getValueTipoCentroCusto()) && !tipoAcaoNeto.equals(TipoAcaoLink.DEL)) {
			workItemNeto.setValue(atributoCentralizadaDMS, true);
			workItemFilho = workItemNeto;
			preencheCentroCustoCentralizado();
		    } else {
			workItemNeto.setValue(atributoCentralizadaDMS, false);
			workItemFilho = workItemNeto;
			preencheCentroCustoDescentralizado();
		    }
		    adicionaComentarioFilho();
		}
		salvaItemFilho();
	    }
	}
    }

    private void salvarFilhoPaiNovo() throws TeamRepositoryException {
	Set<String> additionalParams = new HashSet<String>();
	additionalParams.add(IAdvisorsDefinitions.RECUPERA_CENTRO_CUSTO);
	IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItemFilho, null);
	workItemService.saveWorkItem3(workItemFilho, references, null, additionalParams);
    }

    private void adicionaComentarioFilho() {
	String valorComentarioFilho = null;
	valorComentarioFilho = "O Item de Trabalho " + workItem.getId() + ", atualmente Pai/ Av\u00F4 deste item, teve o valor de um campo alterado ou um link inclu\u00EDdo/exclu\u00EDdo.";

	XMLString commentContent = XMLString.createFromXMLText(valorComentarioFilho);
	IComments comments = workItemFilho.getComments();
	IComment newComment = comments.createComment(usuarioLogado, commentContent);
	comments.append(newComment);
    }

    private boolean atualizarAtributoFilho() throws TeamRepositoryException, ApplicationException {

	if (atributoPai != null) {
	    Identifier<?> idTipoCentroCusto = (Identifier<?>) workItem.getValue(atributoPai);
	    if (idTipoCentroCusto.getStringIdentifier().equalsIgnoreCase(wiDto.getValueTipoCentroCusto())) {
		workItemFilho.setValue(atributoCentralizadaDMS, true);
		preencheCentroCustoCentralizado();
		return true;
	    }
	}
	workItemFilho.setValue(atributoCentralizadaDMS, false);
	preencheCentroCustoDescentralizado();
	return false;
    }

    private void criaLinkPai() throws TeamRepositoryException {
	IReference source = refFactory.createReferenceToItem(workItemFilho);
	IReference target = refFactory.createReferenceToItem(workItem);
	link = linkServiceLibrary.createLink(WorkItemLinkTypes.PARENT_WORK_ITEM, source, target);
    }

    private void removeLink() throws TeamRepositoryException {
	linkServiceLibrary.deleteLink(link);
    }

    private void adicionaLink() throws TeamRepositoryException {
	linkServiceLibrary.saveLink(link);
    }

    private void salvaItemFilho() throws TeamRepositoryException {
	nomeCategoriaPai = workItemService.resolveHierarchicalName(workItem.getCategory(), monitor);
	Set<String> additionalParams = new HashSet<String>();
	additionalParams.add(IAdvisorsDefinitions.RECUPERA_CENTRO_CUSTO);
	workItemService.resolveWorkItemReferences(workItemFilho, monitor);
	IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(workItemFilho, null);
	workItemService.saveWorkItem3(workItemFilho, copyReferences, null, additionalParams);
    }

    private String formataCodCenttroCustoPai(String codHerdado) {
	String[] lista = codHerdado.split("/");
	int lastIndex = lista.length - 1;
	return lista[lastIndex];
    }

}
