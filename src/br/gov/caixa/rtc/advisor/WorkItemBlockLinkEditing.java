package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class WorkItemBlockLinkEditing extends AbstractRTCService implements
	IOperationAdvisor {
	
	private String ERROR_MESSAGE_TYPE = "error";

	private IWorkItemCommon wiCommon = null;
	private IRepositoryItemService repositoryService = null;
	
	private IWorkItem workItem = null;

    private ParserWorkItemDto wiParser;
	private WorkItemDto wiDto;
	private String wiType = null;
	
	private List<IReference> linksAdicionadosRM = null;
	private List<IReference> linksDeletadosRM = null;
	
	

    @Override
    public void run(AdvisableOperation operation,IProcessConfigurationElement advisorConfiguration,IAdvisorInfoCollector collector, IProgressMonitor monitor)
    		throws TeamRepositoryException {

	Object data = operation.getOperationData();

	if (!(data instanceof ISaveParameter)) {
		return;
		}
	    ISaveParameter saveParameter = (ISaveParameter) data;
	    IAuditable auditable = saveParameter.getNewState();
	    
	    if (!(auditable instanceof IWorkItem)) {
	    	return;
	    }
		workItem = (IWorkItem) auditable;

		//Inicializando os serviços que serão utilizados
		wiCommon = getService(IWorkItemCommon.class);
		repositoryService = getService(IRepositoryItemService.class);
		
		wiType = workItem.getWorkItemType();
		try {
			IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
			if (listConfigElements != null) {
				for (IProcessConfigurationElement configElement : listConfigElements) {
					wiParser = new ParserWorkItemDto();
					wiDto = wiParser.parser(configElement);
					
					if (wiDto.getListaIDEstados() == null) {
						String alert = Messages.getStringWithArgs(Messages.MSG_ERRO_CONFIG_ESTADO,"Bloqueia Inclusão/Exclusão de Link");
						IAdvisorInfo info = collector.createProblemInfo(alert,"",ERROR_MESSAGE_TYPE);
						collector.addInfo(info);
						return;
					}
				
					
					if (workItem.isNewItem()&& !wiDto.getItensRelacionados().contains(wiType)) {
						continue;
					}
					
					if (!wiType.equals(wiDto.getType())&& !wiDto.getItensRelacionados().contains(wiType)) {
						continue;
					}
					
					if (wiDto.getQualified() != null && !wiDto.getQualified().isEmpty()) {
						IAttribute crqRequisicao = wiCommon.findAttribute(workItem.getProjectArea(), wiDto.getQualified(),monitor);
						String valorCrqReq = (String) workItem.getValue(crqRequisicao);
						if (valorCrqReq == null || valorCrqReq.length()== 0 ) {
							continue;
						}
					}
					
					if (wiType.equals(wiDto.getType())) {
						
						if (!wiDto.getListaIDEstados().contains(workItem.getState2().getStringIdentifier().toLowerCase())) {
							continue;
						}
						
						
						linksAdicionadosRM = saveParameter.getNewReferences().getCreatedReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
						linksDeletadosRM = saveParameter.getNewReferences().getDeletedReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
						if ((linksAdicionadosRM == null || !linksAdicionadosRM.isEmpty())|| (linksDeletadosRM == null || !linksDeletadosRM.isEmpty())) {
							message(Messages.MSG_ERRO_BLOQUEIO_EDICAO_LINK_RELACIONADO,Messages.MSG_ERRO_INVALID_LINK_DETAIL, collector);
							return;
						}
						
					}else if (wiDto.getItensRelacionados().contains(wiType)) {						
					
						linksDeletadosRM = saveParameter.getNewReferences().getDeletedReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
						linksAdicionadosRM = saveParameter.getNewReferences().getCreatedReferences(WorkItemEndPoints.RELATED_WORK_ITEM);
						
						if (linksDeletadosRM != null && linksDeletadosRM.size()> 0) {
							
							for (IReference itemDeletado : linksDeletadosRM) {
								verificaEdicaoLink(itemDeletado,collector);
							}
						} else{
							
							for (IReference itemAdicionado : linksAdicionadosRM){
								verificaEdicaoLink(itemAdicionado,collector);
								}
						}
					}
				}
			}
		} catch (Exception e) {
		    message(Messages.MSG_ERRO_UNEXPECTED_BLOQUEIO_EDICAO_LINK_RELACIONADO, e.getMessage(),collector);
		    throw new TeamRepositoryException(e);
	}
    }


	private void verificaEdicaoLink(IReference iReference,IAdvisorInfoCollector collector) throws TeamRepositoryException {
		IWorkItemHandle wiRelacionadoHandle = (IWorkItemHandle)iReference.resolve();
		IWorkItem itemRelacionado = (IWorkItem)repositoryService.fetchItem(wiRelacionadoHandle, IWorkItem.LARGE_PROFILE.getPropertiesArray());
		
		if (itemRelacionado.getWorkItemType().equals(wiDto.getType()) && wiDto.getListaIDEstados().contains(itemRelacionado.getState2().getStringIdentifier().toLowerCase())) {
			message(Messages.MSG_ERRO_BLOQUEIO_EDICAO_LINK_RELACIONADO,Messages.MSG_ERRO_INVALID_LINK_DETAIL, collector);
			return;
		}	
	}
		
	}


