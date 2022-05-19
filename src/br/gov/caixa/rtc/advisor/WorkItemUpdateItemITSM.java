package br.gov.caixa.rtc.advisor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.user.IDUserRTC;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.integracao.dao.CMDBDAO;
import br.gov.caixa.rtc.integracao.entidades.NotaAtividades;
import br.gov.caixa.rtc.util.Formatters;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttachmentHandle;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemUpdateItemITSM extends AbstractRTCService implements IOperationAdvisor {

    private static final String ERROR_MESSAGE_TYPE = "error";
    public static final String STRING_TYPE_HTML = "HTML";
    
    private IRepositoryItemService repositoryService = null;
    private IWorkItemServer workItemService = null;
    private IAuditableCommon auditableCommon = null;
//    private IProgressMonitor monitor = null;
    
    private ParserWorkItemDto wiParser;
    private WorkItemDto wiDto;

    private String wiType = null;
    private String idInternoIncidenteITSM = null;
    private String tipoNotaTrabalhoITSM ="General Information";
    private String requestIdIncidente = null;
    private List<IReference> anexos = null;
    private List <File> listaArquivos = null;
    private String attributeQualifiedText = null;
    private IAttribute attributeQualified = null;
    private IAttribute atributoIncidenteInterno = null;
    private String projectAreaName = null;

    private IWorkItem workItem = null;
    private IWorkItem workItemOldState = null;
    private IDUserRTC userRTC;

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
	
	anexos = null;
	anexos = new ArrayList<IReference>();
	anexos = saveParameter.getNewReferences().getCreatedReferences(WorkItemEndPoints.ATTACHMENT);
	
	workItem = (IWorkItem) saveParameter.getNewState();
	if (workItem.isNewItem()) {
	    return;
	}
	workItemOldState = (IWorkItem) saveParameter.getOldState();
	
	if (workItem.equals(workItemOldState)) {
	    return;
	}
	
	// Inicializando os serviços que serão utilizados
	repositoryService = getService(IRepositoryItemService.class);
	workItemService = getService(IWorkItemServer.class);
	auditableCommon = getService(IAuditableCommon.class);
	userRTC = new IDUserRTC(this, repositoryService);
//	this.monitor = monitor;
	
	wiType = workItem.getWorkItemType();
	IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
	if (listConfigElements != null) {
	    for (IProcessConfigurationElement configElement : listConfigElements) {
		wiParser = new ParserWorkItemDto();
		wiDto = wiParser.parser(configElement);

		if (!wiType.equals(wiDto.getType())) {
		    continue;
		}
		if (anexos != null && anexos.size()> 1){  
		    message("Limite excedido. Permitido 1 anexo até 50MB. Favor zipar caso precise incluir mais de 1 arquivo como anexo", ERROR_MESSAGE_TYPE, collector);
		    return;
		}		
		if (workItem.getState2().getStringIdentifier().equalsIgnoreCase(workItemOldState.getState2().getStringIdentifier())) {
		    continue;
		}
		
		 String idEstadoLimpar = wiDto.getIdEstadoLimpar();	
		 System.out.println(idEstadoLimpar);
		    
		if (idEstadoLimpar != null) {
		    if (idEstadoLimpar.equalsIgnoreCase(workItem.getState2().getStringIdentifier().toString())) {
			if (wiDto.getQualified() != null) {
			    attributeQualified = (IAttribute) workItemService.findAttribute(workItem.getProjectArea(), wiDto.getQualified(), monitor);

			    	if (!workItem.hasAttribute(attributeQualified)) {
				    continue;
				}
			    }

//			workItem.setValue(attributeQualified, null);
			workItem.setValue(attributeQualified, "");
			continue; 
		    }
		    
		}
			
		if (!wiDto.getState().equalsIgnoreCase(workItem.getState2().getStringIdentifier().toLowerCase())) {
		    continue;
		}else{
		   
		    tipoNotaTrabalhoITSM = wiDto.getTipoNotaTrabalhoITSM();
		    
		    if (wiDto.getQualified() != null ) {
		    attributeQualified = (IAttribute) workItemService.findAttribute(workItem.getProjectArea(), wiDto.getQualified(), monitor);

		    	if (!workItem.hasAttribute(attributeQualified)) {
			    continue;
			}
		    }

//		    attributeQualifiedText = new String(Formatters.parseHTML2String(workItem.getValue(attributeQualified).toString()).getBytes(),"UTF-8");
		    if (workItem.getValue(attributeQualified) == null) {
			workItem.setValue(attributeQualified, "");
		    }
		    attributeQualifiedText = Formatters.parseHTML2String(workItem.getValue(attributeQualified).toString());
		    
		    System.out.println(attributeQualifiedText);
		    
		if (wiDto.getAttribute() != null) {
		    atributoIncidenteInterno = workItemService.findAttribute(workItem.getProjectArea(), wiDto.getAttribute(), monitor);
		    if (!workItem.hasAttribute(atributoIncidenteInterno)) {
			continue;
		    }
		}
		    String valorIncidenteInterno = workItem.getValue(atributoIncidenteInterno).toString();
		    
		    if (attributeQualifiedText.isEmpty() || valorIncidenteInterno.isEmpty()) {
			message(Messages.MSG_INCIDENTE_CAMPO_NAO_PREENCHIDO +" "+ attributeQualified.getDisplayName(), ERROR_MESSAGE_TYPE, collector);
			return;
		    }
		    idInternoIncidenteITSM = workItem.getValue(atributoIncidenteInterno).toString().trim();
		    if (idInternoIncidenteITSM.isEmpty()) {
			message(Messages.MSG_FALTA_ID_INTERNO_INCIDENTE, ERROR_MESSAGE_TYPE, collector);
			return;
		    }
			requestIdIncidente = idInternoIncidenteITSM +"|" + idInternoIncidenteITSM;
		}
		
		IProjectArea ipa = (IProjectArea) repositoryService.fetchItem(workItem.getProjectArea(), null);
		projectAreaName = ipa.getName();
		
		
		 try { 
		    CMDBDAO itsmDao = new CMDBDAO();
		    String incidente = itsmDao.getIncidente(requestIdIncidente);
		    if (incidente == null || incidente.isEmpty()) {
			throw new ApplicationException(Messages.MSG_INCIDENTE_NAO_ENCONTRADO);
//			message(, ERROR_MESSAGE_TYPE, collector);
//			return;
		    } 
		   
		    if (anexarArquivo()) {
			itsmDao.atualizaIncidenteComAnexo(requestIdIncidente,getDadosNotaIncidenteComAnexo(collector, monitor),listaArquivos);
		    }else{
			itsmDao.atualizaIncidente(requestIdIncidente,getDadosNotaIncidente(collector,monitor));
		    }
		    
		} catch (ApplicationException e) {
		    message(Messages.MSG_ERRO_ATUALIZAR_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
		      e.printStackTrace();		    
		} catch (IllegalStateException | IOException e1) {
		    message(Messages.MSG_ERRO_ATUALIZAR_INCIDENTE,e1.getMessage(),ERROR_MESSAGE_TYPE, collector);
		    e1.printStackTrace();
		} finally {
		    anexos = null;
		    listaArquivos = null;
//		    System.out.println((String)workItem.getValue((IAttribute) workItemService.findAttribute(workItem.getProjectArea(), wiDto.getQualified(), monitor)));
//		    itsmDao.logout();
		}
	    }
	}
    }

   
    private boolean anexarArquivo() {
	if (anexos == null  || anexos.isEmpty() ) {
	    return false;
	}
	return true;
    }

    private String getDadosNotaIncidente(IAdvisorInfoCollector collector,IProgressMonitor monitor) {
	
	String body = null;
	
	try {
	    body = createJSON();
	   System.out.println(attributeQualifiedText);
	} catch (ApplicationException e) {
	    message(Messages.MSG_USUARIO_NAO_ENCONTRADO,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (TeamRepositoryException e) {
	    message(Messages.MSG_ERRO_TEAM_REPOSITORY_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (JsonProcessingException e) {
	    message(Messages.MSG_ERRO_CRIACAO_JSON_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {
	    message(Messages.MSG_ERRO_ENCODING_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} 

	return body ;
    }
    
private String getDadosNotaIncidenteComAnexo(IAdvisorInfoCollector collector, IProgressMonitor monitor)  {
	
    	String body = null;
	listaArquivos = new ArrayList<File>();
	
	try {
	for (IReference referenceTemp : anexos) {
	  IAttachmentHandle  attachHandle = (IAttachmentHandle) referenceTemp.resolve();
	  IAttachment  anexoTemp = (IAttachment) auditableCommon.resolveAuditable((IAttachmentHandle) attachHandle,IAttachment.FULL_PROFILE, null);
	  File arquivoTemp = new File(anexoTemp.getName());
	  OutputStream out = new FileOutputStream(arquivoTemp);
	  auditableCommon.retrieveContent(anexoTemp.getContent(), out, null);
	  listaArquivos.add(arquivoTemp);
	}
	body = createJSON();
	} catch (ApplicationException e) {
	    message(Messages.MSG_USUARIO_NAO_ENCONTRADO, "ERRO", collector);
	    e.printStackTrace();
	} catch (TeamRepositoryException e) {
	    message(Messages.MSG_ERRO_TEAM_REPOSITORY_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (JsonProcessingException e) {
	    message(Messages.MSG_ERRO_CRIACAO_JSON_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {
	    message(Messages.MSG_ERRO_ENCODING_INCIDENTE,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    message(Messages.MSG_ERRO_ARQUIVO_NAO_ENCONTRADO,e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	    e.printStackTrace();
	
	} 
	
	return body;
    }


    private String getCurrentDate() {
	SimpleDateFormat formatter= new SimpleDateFormat("dd/MM/yyyy 'as' HH:mm:ss");
	Date date = new Date(System.currentTimeMillis());
	return formatter.format(date);
    }
    
    private String createJSON() throws ApplicationException, TeamRepositoryException, JsonProcessingException, UnsupportedEncodingException{
	String body = null;
	String responsavelAlteracao = userRTC.getUserLogado().getName() + "(" + userRTC.getUserLogado().getUserId() +")";
	
	String msg = null;
	msg = responsavelAlteracao + " atualizou, " +  getCurrentDate() + ", o Incidente(" + workItem.getId() + "), que pertence ao Projeto " + projectAreaName + " no RTC." + "\n\n"+ attributeQualifiedText;

	ObjectMapper mapper = new ObjectMapper();
	mapper.enable(SerializationFeature.INDENT_OUTPUT);
	mapper.setSerializationInclusion(Include.NON_EMPTY);
	mapper.setSerializationInclusion(Include.NON_NULL);
	
	if (listaArquivos != null && listaArquivos.size()>=1 ) {
	    NotaAtividades notaITSM = new NotaAtividades(tipoNotaTrabalhoITSM, msg,listaArquivos.get(0).getName());
	    body = "{\"values\":"+ mapper.writeValueAsString(notaITSM)+"}";
	    String tempComArquivo = new String(body.getBytes(),"UTF-8");
	    System.out.println(tempComArquivo);
	    return tempComArquivo;
	}
	
	NotaAtividades notaITSM = new NotaAtividades(tipoNotaTrabalhoITSM, msg,null);
	body = "{\"values\":"+ mapper.writeValueAsString(notaITSM)+"}";
	String tempSemArquivo = new String(body.getBytes(),"UTF-8");
	System.out.println(tempSemArquivo);
	return tempSemArquivo;
	
    }

}
