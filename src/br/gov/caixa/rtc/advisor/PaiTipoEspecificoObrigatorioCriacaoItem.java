package br.gov.caixa.rtc.advisor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;



//	<precondition id="br.gov.caixa.rtc.advisor.PaiTipoEspecificoObrigatorioCriacaoItem" name="Link Pai de um Tipo Específico Obrigatório para a Criação do Item de Trabalho">
//		<config workItemType="com.ibm.team.apt.workItemType.story" stateId="com.ibm.team.apt.storyWorkflow.state.s4" parentWorkitemType="entrega_vitrine" attribute="tipo_item_backlog" attributeValue="tipo_item_backlog.literal.l2"/>
//	</precondition>


public class PaiTipoEspecificoObrigatorioCriacaoItem extends AbstractRTCService implements IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";

    private IRepositoryItemService repositoryService = null;
    private IWorkItemServer workItemService = null;
//    private IProgressMonitor monitor = null;

    private ParserWorkItemDto wiParser;
    private WorkItemDto wiDto;

    private String wiType = null;
    private IWorkItem workItem = null;
    private IWorkItem workItemParent = null;
    private IAttribute tipoItemBacklog = null;
    private Identifier<?> idTipoItemBacklog = null;

    @Override
    public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {

//	System.out.println("################################################################################################################################################");
//	System.out.println("Inicio Advisor: PaiTipoEspecificoObrigatorioCriacaoItem - Link Pai de um Tipo Específico Obrigatório para a Criação do Item de Trabalho");
//	System.out.println("################################################################################################################################################");
	
	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	if (!(saveParameter.getNewState() instanceof IWorkItem)) {
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

		
//		System.out.println("Tipo do WI: " + wiType);
//		System.out.println("Parametro Tipo do WI configurado: " + wiDto.getType());
//		System.out.println("Resultado comparação wiType: " + !wiType.equals(wiDto.getType()));
		if (!wiType.equals(wiDto.getType())) {
		    continue;
		}
		
		try {

		    //Verifica se o item é novo ou o status que está indo é o configurado no advisor
//		    if (workItem.isNewItem() || workItem.getState2().getStringIdentifier().equalsIgnoreCase(wiDto.getState())) {
		    
		  //Verifica se o item está indo para o status configurado no advisor
//		    System.out.println("Status destino do WI: " + workItem.getState2().getStringIdentifier());
//		    System.out.println("Parametro Status do WI configurado: " + wiDto.getState());
//		    System.out.println("Resultado comparação status: " + workItem.getState2().getStringIdentifier().equalsIgnoreCase(wiDto.getState()));
		    if (workItem.getState2().getStringIdentifier().equalsIgnoreCase(wiDto.getState())) {
			
			tipoItemBacklog = (IAttribute) workItemService.findAttribute(workItem.getProjectArea(), wiDto.getAttribute(), monitor).getWorkingCopy();
//			System.out.println("Verificando se o Atributo Tipo do Item de Backlog existe no WI ");
//			System.out.println(workItem.hasAttribute(tipoItemBacklog));
			// Atributo Tipo do Item de Backlog
//        		    if (!workItem.hasAttribute(tipoItemBacklog)) {
//        			message(Messages.MSG_ERRO_ATRIBUTO_NAO_SINCRONIZADO, ERROR_MESSAGE_TYPE, collector);
//        			return;
//        		    }
        		    
        		    idTipoItemBacklog = (Identifier<?>) workItem.getValue(tipoItemBacklog);
//        		    System.out.println("Tipo do Item de Backlog do WI: " + idTipoItemBacklog.getStringIdentifier());
//        		    System.out.println("Parametro Tipo do Item de Backlog configurado: " + wiDto.getAttributeValue());
//        		    System.out.println("Resultado comparação Tipo do Item de Backlog: " + idTipoItemBacklog.getStringIdentifier().equalsIgnoreCase(wiDto.getAttributeValue()));
        		    if (idTipoItemBacklog.getStringIdentifier().equalsIgnoreCase(wiDto.getAttributeValue())) {
        			
        		  
        		    // Recuperando lista informações do Pai
        		    IWorkItemReferences references = saveParameter.getNewReferences();
        		    List<IReference> parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
        		    /* Valida se o item de trabalho tem pai */
//        		    System.out.println("Validando se o item de trabalho tem pai ");
//        		    System.out.println("parentReferences != null: " + parentReferences != null);
//        		    System.out.println("!parentReferences.isEmpty(): " + !parentReferences.isEmpty());
//        		    System.out.println("Resultado da Validação se o WI tem pai: " + parentReferences != null && !parentReferences.isEmpty());
        		    if (parentReferences != null && !parentReferences.isEmpty()) {
        			for (IReference parent : parentReferences) {
        			    IWorkItemHandle wiParentHandle = (IWorkItemHandle) parent.resolve();
        			    workItemParent = (IWorkItem) (repositoryService.fetchItem(wiParentHandle, IWorkItem.FULL_PROFILE.getPropertiesArray()).getWorkingCopy());
        
        			   
//        			    System.out.println("Validando se o pai do WI é DIFERENTE do tipo configurado");
//                		    System.out.println("Tipo do pai do WI: " + workItemParent.getWorkItemType());
//                		    System.out.println("Tipo configurado para o wi Pai: " + wiDto.getParentWIType());
//                		    System.out.println("Resultado da comparação do Tipo do Pai: " + !workItemParent.getWorkItemType().equalsIgnoreCase(wiDto.getParentWIType()));
        			    
        			    if (!workItemParent.getWorkItemType().equalsIgnoreCase(wiDto.getParentWIType())) {
        				message(Messages.MSG_ERRO_PAI_ESPECIFICO_OBRIGATORIO_PARA_WI, ERROR_MESSAGE_TYPE, collector);
        				return;
        				    }
        				}
        			    }else{
        				message(Messages.MSG_ERRO_PAI_ESPECIFICO_OBRIGATORIO_PARA_WI, ERROR_MESSAGE_TYPE, collector);
        				return;
        			    }
//        
//        				return;
        			    }
		    }

		} catch (TeamRepositoryException e) {
		    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		    e.printStackTrace();
		} catch (Exception e) {
		    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		}
	    }
	}
    }

      

    

}
