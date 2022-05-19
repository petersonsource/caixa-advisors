package br.gov.caixa.rtc.participant;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;

import javax.mail.MessagingException;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.advisor.AbstractRTCService;
import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;
import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.team.links.common.IReference;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.runtime.IOperationParticipant;
import com.ibm.team.process.common.advice.runtime.IParticipantInfoCollector;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.Location;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.util.NLS;
import com.ibm.team.repository.service.IMailerService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.repository.service.MailSender;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.service.IWorkItemServer;

public class SendMailForState extends AbstractRTCService implements IOperationParticipant {

    private static final String ERROR_MESSAGE_TYPE = "error";
    private static final String UNASSIGNED_USER_ID = null;
    private static final String UNASSIGNED_MAIL = null;

    // <config workItemType="demanda_de_negocio" stateId="DemandaNeg.state.s5"
    // parentWIType="" contributor="" mailSummary="" bodyMessage="" />
    public void run(AdvisableOperation operation, IProcessConfigurationElement participantConfig, IParticipantInfoCollector collector, IProgressMonitor monitor) throws TeamRepositoryException {

	Object data = operation.getOperationData();
	if (!(data instanceof ISaveParameter)) {
	    return;
	}

	ISaveParameter saveParameter = (ISaveParameter) data;
	IAuditable auditable = saveParameter.getNewState();
	IAuditable oldAuditable = saveParameter.getOldState();
	if (!(auditable instanceof IWorkItem) && !(oldAuditable instanceof IWorkItem)) {
	    return;
	}
	IWorkItem workItem = (IWorkItem) auditable;
	IWorkItem workItemOldState = (IWorkItem)saveParameter.getOldState();
	IWorkItemServer workItemService = getService(IWorkItemServer.class);
	IWorkItemReferences references = workItemService.resolveWorkItemReferences(workItem, monitor);
	IRepositoryItemService repository = getService(IRepositoryItemService.class);
	Location location = Location.namedLocation(workItem, this.getRequestRepositoryURL());
	IProjectAreaHandle ipah = operation.getProcessArea().getProjectArea();
	IProjectArea ipa = (IProjectArea) repository.fetchItem(ipah, null);
	List<IReference> parentReferences = null;
	IWorkItem workItemParent;
	String wiParentType;
	IWorkItemHandle wiParentHandle;
	String contributorParentMail;
	String contributorMail;
	String mailSummaryFmt;
	String mailBodyMessageFmt;
	
	if (workItem.equals(workItemOldState)) {
	    return;
	}

	try {
	    IProcessConfigurationElement[] configElements = participantConfig.getChildren();
	    if (configElements != null) {

		String wiType = workItem.getWorkItemType();

		for (IProcessConfigurationElement configElement : configElements) {
		    // INPUT PARAMETER
		    ParserWorkItemDto parser = new ParserWorkItemDto();
		    WorkItemDto dto = parser.parser(configElement);
		    // END INPUT PARAMETER
		    // CHECK
		    if (!wiType.equals(dto.getType())) {
			System.out.println("Tipo do item diferente do item configurado para este ADVISOR");
			continue;
		    }
		    
		    if (workItem.getState2().getStringIdentifier().equalsIgnoreCase(workItemOldState.getState2().getStringIdentifier())) {
			    continue;
		    }

		    if (!dto.getState().equalsIgnoreCase(workItem.getState2().getStringIdentifier())) {
			dto.setState(dto.getState().replace("s", ""));
			if (!dto.getState().equalsIgnoreCase(workItem.getState2().getStringIdentifier())) {
			    System.out.println("Estado não configurado para este ADVISOR");
			    continue;
			}
		    }

		    if (dto.getContributor().isEmpty() || dto.getContributor() == null) {
			System.out.println("Contributor não configurado ou vazio");
			continue;
		    }

		    if (dto.getMailSummary().isEmpty() || dto.getMailSummary() == null) {
			System.out.println("Título do e-mail não configurado ou vazio");
			continue;
		    } else {
			System.out.println("Montando o Titulo do e-mail");
			System.out.println("Titulo configuracao: " + dto.getMailSummary() );
			System.out.println("ID: " + workItem.getId() );
			System.out.println("Titulo WI: " + workItem.getHTMLSummary().getPlainText());
			System.out.println("Area Projeto WI: " + ipa.getName());
			
			// Ordem de apresentacao {0} id, {1} resumo {2} area
			mailSummaryFmt = getStringWithArgs(dto.getMailSummary(), String.valueOf(workItem.getId()), workItem.getHTMLSummary().getPlainText(), ipa.getName());
			
			System.out.println("Titulo do e-mail Montado: " );
			System.out.println(mailSummaryFmt);

		    }

			
			if (dto.getBodyMessage().isEmpty() || dto.getBodyMessage() == null) {
			    System.out.println("Mensagem do e-mail não configurado ou vazio");
			continue;
		    } else {
			System.out.println("Montando a Memsagem do e-mail");
			System.out.println("Mensagem configuracao: " + dto.getBodyMessage() );
			System.out.println("ID: " + workItem.getId() );
			System.out.println("Area Projeto WI:: " + ipa.getName());
			// Ordem de apresentacao {0} link, {1} id {2} area
			mailBodyMessageFmt = getStringWithArgs(dto.getBodyMessage(), location.toString(), String.valueOf(workItem.getId()), ipa.getName());
			
			System.out.println("Mensagem do e-mail Montado: " );
			System.out.println(mailBodyMessageFmt);
		    }

			parentReferences = references.getReferences(WorkItemEndPoints.PARENT_WORK_ITEM);
		    // FIM Check

		    // envio do email
		    // without parent
		    if ((dto.getParentWIType().isEmpty() || dto.getParentWIType() == null) && (parentReferences == null || parentReferences.isEmpty())) {
			System.out.println("Envio do e-mail SEM pai");
			System.out.println("Recuperando e-mail do usuário");
			contributorMail = getContributorMail(monitor, workItem, workItemService, repository, dto);
			System.out.println("E-mail do usuário recuperado com sucesso");
//			System.out.println(contributorMail);
			System.out.println("Iniciando a criacao da Notificacao do email ");
			createNotificationMail(workItem, mailBodyMessageFmt, mailSummaryFmt, contributorMail);
			System.out.println("Notificacao do email criada com sucesso");
			

			// with parent
		    } else {
			System.out.println("Envio do e-mail COM pai");
			
			if (!dto.isValidateParent()) {
			    if (parentReferences != null || parentReferences.size() > 0) {
				for (IReference parentReference : parentReferences) {
				    wiParentHandle = (IWorkItemHandle) parentReference.resolve();
				    workItemParent = (IWorkItem) repository.fetchItem(wiParentHandle, IWorkItem.MEDIUM_PROFILE.getPropertiesArray());
				    if (workItemParent != null) {
					// check parent type
					System.out.println("Validando tipo do Pai");
					wiParentType = workItemParent.getWorkItemType();
					if (wiParentType.equals(dto.getParentWIType())) {
					    System.out.println("Recuperando e-mail do usuário");
					    contributorParentMail = getContributorMailParent(monitor, workItemService, repository, workItemParent, dto);
					    System.out.println("E-mail do usuário recuperado com sucesso");
					    System.out.println("Iniciando a criacao da Notificacao do email ");
					    createNotificationMail(workItem, mailBodyMessageFmt, mailSummaryFmt, contributorParentMail);
					    System.out.println("Notificacao do email criada com sucesso");
					    break;
					}
				    }
				}
			    } else {
				continue;
			    }
			} else {
			    if (parentReferences == null || parentReferences.size() < 1) {
				System.out.println("Link pai nulo ou 0");
				contributorMail = getContributorMail(monitor, workItem, workItemService, repository, dto);
				createNotificationMail(workItem, mailBodyMessageFmt, mailSummaryFmt, contributorMail);
			    } else {
				continue;
			    }
			}

		    }
		    // fim envio do email
		    	System.out.println("fim envio do email");
		}
	    }
	} catch (Exception e) {
	    message(e.getMessage(), ERROR_MESSAGE_TYPE, collector);
	}
    }

    private String getContributorMailParent(IProgressMonitor monitor, IWorkItemServer workItemService, IRepositoryItemService repository, IWorkItem workItemParent, WorkItemDto dto) throws TeamRepositoryException, MessagingException {
	String contributorMail = null;
	IAttribute attribute = workItemService.findAttribute(workItemParent.getProjectArea(), dto.getContributor(), monitor);
	IContributorHandle contributorHandle = (IContributorHandle) workItemParent.getValue(attribute);
	IContributor contributorValue = (IContributor) repository.fetchItem(contributorHandle, null);
	try {
	    contributorMail = contributorValue.getEmailAddress();
	    System.out.println("Usuario sem email - getContributorMailParent() ");
	} catch (Exception e) {
	    throw new MessagingException("Usuario sem email.");
	}
	return contributorMail;
    }

    private String getContributorMail(IProgressMonitor monitor, IWorkItem workItem, IWorkItemServer workItemService, IRepositoryItemService repository, WorkItemDto dto) throws TeamRepositoryException, MessagingException {
	String contributorMail = null;
	IAttribute attribute = workItemService.findAttribute(workItem.getProjectArea(), dto.getContributor(), monitor);
	IContributorHandle contributorHandle = (IContributorHandle) workItem.getValue(attribute);
	IContributor contributorValue = (IContributor) repository.fetchItem(contributorHandle, null);
	try {
	    contributorMail = contributorValue.getEmailAddress();
	    System.out.println(contributorValue.getEmailAddress());
	} catch (Exception e) {
	    System.out.println("Usuario sem email - getContributorMail");
	    throw new MessagingException("Usuario sem email.");
	}
	return contributorMail;
    }

    private void createNotificationMail(IWorkItem workItem, String mailBody, String mailSummary, String contributorMail) throws UnsupportedEncodingException, TeamRepositoryException {
	System.out.println("Solicitando envio do e-mail");
	sendInfoMail(workItem, contributorMail, mailSummary, mailBody);
    }

    private void sendInfoMail(IWorkItem workItem, String mail, String bodySubject, String bodyMessage) throws TeamRepositoryException, UnsupportedEncodingException {
	System.out.println("Inicializando servico para envio do e-mail" );
	MailSender sender = getMailService().getDefaultSender();
	System.out.println("Servico Inicializado:");
	System.out.println(sender.toString());

	try {
	    System.out.println("Enviando o email");
	    getMailService().sendMail(sender, mail, bodySubject, bodyMessage, null);
	    System.out.println("E-mail enviado");

	} catch (MessagingException e) {
	    String warningMessage = NLS.bind("Falha ao enviar o email", workItem.getHTMLSummary().toString(), mail);
	    System.out.println("Erro ao enviar E-mail");
	    System.out.println(workItem.getHTMLSummary().toString());
	    System.out.println(mail);	   
	    getLog().warn(warningMessage, e);
	    System.out.println(e.toString());
	}

    }

    private IMailerService getMailService() {
	return getService(IMailerService.class);
    }

    private IRepositoryItemService getRepositoryService() {
	return getService(IRepositoryItemService.class);
    }

    private String getStringWithArgs(String desc, String... key) throws ApplicationException {
	try {
	    System.out.println("Montando msg com argumentos");
	    MessageFormat messageFormat = new MessageFormat(desc);
	    return messageFormat.format(key);
	} catch (MissingResourceException e) {
	    System.out.println("Erro ao obter o texto.");
	    throw new ApplicationException("Erro ao obter o texto.", e);
	}
    }

}
