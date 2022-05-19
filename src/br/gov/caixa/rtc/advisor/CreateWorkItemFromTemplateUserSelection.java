package br.gov.caixa.rtc.advisor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.internal.links.Reference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.IProcessReport;
import com.ibm.team.process.common.advice.IReportInfo;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.internal.IWorkItemTemplateService;
import com.ibm.team.workitem.common.internal.template.AttributeVariable;
import com.ibm.team.workitem.common.internal.template.WorkItemTemplateSerializable;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.template.IAttributeVariable;
import com.ibm.team.workitem.service.IWorkItemServer;

public class CreateWorkItemFromTemplateUserSelection extends AbstractRTCService implements
IOperationAdvisor{

	private static final String MESSAGE_TYPE_ERROS = "error";
	private static final String MESSAGE_TYPE_INFO = "info";
	
	private IRepositoryItemService repositoryService = null;
	private IWorkItemServer workItemService  = null;
	private IWorkItemTemplateService templateService = null;
	
	private ParserWorkItemDto wiParser;
	private WorkItemDto wiDto;
	
	private String wiType = null;
	private String templateSelected = null;
	private String templateId = null;
	
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
			
		IWorkItem workItem = (IWorkItem)saveParameter.getNewState();
		
		
		// Verifica se é Item de Trabalho Novo, pois só deve executar na criação.
		if (!workItem.isNewItem()) {
			return;
		}
		
		//Inicializando os serviços que serão utilizados
		repositoryService = getService(IRepositoryItemService.class);
		workItemService = getService(IWorkItemServer.class);
		templateService = getService(IWorkItemTemplateService.class);
		
		wiType = workItem.getWorkItemType();
		
		IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
		if (listConfigElements != null) {
			
			for (IProcessConfigurationElement configElement : listConfigElements) {
				wiParser = new ParserWorkItemDto();
				wiDto = wiParser.parser(configElement);
				
				if (!wiType.equals(wiDto.getType())) {
					continue;
				}
			
				
			templateSelected = wiDto.getAttrTemplate();
			if (templateSelected == null) {
				message("Não existe um Modelo predefinido para este tipo de item de trabalho", MESSAGE_TYPE_ERROS, collector);
		       	return;
			}
			templateSelected = templateSelected.trim();
				
			//Campo Booleano CriarTarefa
			IAttribute atributoCriarTarefa = workItemService.findAttribute(workItem.getProjectArea(), wiDto.getAttribute(),monitor);
//			if (!workItem.hasCustomAttribute(atributoCriarTarefa) ) {
//				workItem.addCustomAttribute(atributoCriarTarefa);
//			}
				
			//Campo data de Criação das Tarefas
			IAttribute atributoDtaCriacaoTarefas = workItemService.findAttribute(workItem.getProjectArea(), wiDto.getQualified(),monitor);
//			if (!workItem.hasCustomAttribute(atributoDtaCriacaoTarefas) ) {
//				workItem.addCustomAttribute(atributoDtaCriacaoTarefas);
//				
//			}
			
			boolean boolCriaTarefa =(boolean)workItem.getValue(atributoCriarTarefa);
			Timestamp dtCriacaoTarefa =(Timestamp) workItem.getValue(atributoDtaCriacaoTarefas);

			if (dtCriacaoTarefa != null && boolCriaTarefa ) {
				return;
			}
		
			if (!boolCriaTarefa) {
				return;
			}
		
			try{
				// Criar Itens de Trabalho
		        Map<IAttributeVariable,Object> workItemTemplateVariables = new HashMap<IAttributeVariable,Object>();
		        IAttribute attrPlannedFor = workItemService.findAttribute(workItem.getProjectArea(), IWorkItem.TARGET_PROPERTY, monitor);
		        IAttributeVariable varPlannedFor = (IAttributeVariable)new AttributeVariable(attrPlannedFor);			        			        
		        workItemTemplateVariables.put(varPlannedFor, workItem.getTarget());
		        
		       	// Selecionar o Modelo de Itens de Trabalho
		        Object[] templates = templateService.getAvailableTemplates2(workItem.getProjectArea());
		        int i=0;
		        for (Object obj : templates) {
		        	if (obj != null && obj instanceof String) {
			        	String t = (String)obj;
			        	
			        	if (t.equals(templateSelected)) { 
			        		templateId = (String)templates[i-1];
			        		break;
			        	}
		        	}
		        	i++;
		        }
        
		        if (templateId == null) {
		        	message("Não foi possível encontrar o template "+templateSelected, MESSAGE_TYPE_ERROS, collector);
		        	return;
		        }				        
        
		        int[] arrayWi =	templateService.instantiateTemplate(templateId, WorkItemTemplateSerializable.serializeVariableAndParameterValues(workItemTemplateVariables, null), workItem.getProjectArea());      
        
		        if (arrayWi == null || arrayWi.length == 0) {
		        	message("Não foi possível criar os itens de trabalho a partir do template", MESSAGE_TYPE_ERROS, collector);
		        	return;
		        }
		        
		        List<Integer> listWi = new ArrayList<Integer>();
		        for (int wiId : arrayWi) {
		        	listWi.add(wiId);
		        }			       
        
		        // Obter lista dos Itens de Trabalho criados à partir do Modelo selecionado.
		        List<IWorkItemHandle> wisCreated = workItemService.findWorkItemsById(listWi, monitor);
        
		        //Criar link entre os itens criados e o item de trabalho Pai (i.e.: Demanda de Manutenção de Sistemas)
		        IWorkItemReferences refs = saveParameter.getNewReferences();								
					for (IWorkItemHandle wiCreated : wisCreated) {
						IWorkItemReferences ref= workItemService.resolveWorkItemReferences(wiCreated, monitor);
						if (!ref.hasReferences(WorkItemEndPoints.PARENT_WORK_ITEM)){
							Reference reference = (Reference)IReferenceFactory.INSTANCE.createReferenceToItem(wiCreated);
							refs.add(WorkItemEndPoints.CHILD_WORK_ITEMS, reference); 	
						}
					}
		
				// Propagar o ID do Item de Trabalho Pai para os filhos.
				if (wiDto.getPropagateParentTitle()) {
					for (IWorkItemHandle wiCreatedHandle : wisCreated) {
						IWorkItem wiCreated = (IWorkItem)repositoryService.fetchItem(wiCreatedHandle, new String[] {IWorkItem.SUMMARY_PROPERTY});
						XMLString xmlSummary = wiCreated.getHTMLSummary();
						String summary = xmlSummary.getPlainText();
						String parentId = String.valueOf(workItem.getId());
						
						StringBuffer newSummary = new StringBuffer();
						newSummary.append(summary).append(" (").append(parentId).append(")");
						
						IWorkItem wiCreatedCopy = (IWorkItem)wiCreated.getWorkingCopy();
						wiCreatedCopy.setHTMLSummary(XMLString.createFromPlainText(newSummary.toString()));							
						IWorkItemReferences copyReferences = workItemService.resolveWorkItemReferences(wiCreatedCopy, null);
						
						try {
							workItemService.saveWorkItem2(wiCreatedCopy, copyReferences, null);	
						} catch (Exception e) {
							IReportInfo info = collector.createProblemInfo("Título do WI "+wiCreated.getId()+" não alterado", e.getMessage(), MESSAGE_TYPE_INFO); //$NON-NLS-1$
							info.setSeverity(IProcessReport.OK);
							collector.addInfo(info);
						}
					}						
				}
				
				workItem.setValue(atributoDtaCriacaoTarefas, new Timestamp(System.currentTimeMillis()));
				
			

			} catch (Exception e) {
				IAdvisorInfo info = collector.createProblemInfo("Erro inesperado", e.getMessage(), MESSAGE_TYPE_ERROS);
				collector.addInfo(info);
				throw new TeamRepositoryException(e);
				}
			}
		

		}
	}
	
}
