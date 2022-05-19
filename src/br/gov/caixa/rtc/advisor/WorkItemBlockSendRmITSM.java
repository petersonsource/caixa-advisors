package br.gov.caixa.rtc.advisor;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import br.gov.caixa.rtc.dto.ParserWorkItemDto;
import br.gov.caixa.rtc.dto.WorkItemDto;

import com.ibm.rational.services.rtc.advisor.messages.Messages;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.IItemQueryPage;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.internal.model.query.BaseAttributeQueryModel.AttributeQueryModel;
import com.ibm.team.workitem.common.internal.model.query.BaseWorkItemQueryModel.WorkItemQueryModel;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.service.IWorkItemServer;

public class WorkItemBlockSendRmITSM extends AbstractRTCService implements
	IOperationAdvisor {
	
	private String ERROR_MESSAGE_TYPE = "error";

	private IWorkItemCommon wiCommon = null;
	private IWorkItem workItem = null;
	private IWorkItemServer workItemService = null;
	private IQueryService queryService = null;
	private IRepositoryItemService repository = null;
	private IAuditableCommon auditableCommon = null;

    private ParserWorkItemDto wiParser;
	private WorkItemDto wiDto;
	private String wiType = null;
	
//	private List<IReference> linksAdicionados = null;
//	private List<IReference> linksDeletados = null;

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
		workItemService = getService(IWorkItemServer.class);
		queryService = getService(IQueryService.class);
		repository = getService(IRepositoryItemService.class);
		auditableCommon = saveParameter.getSaveOperationParameter().getAuditableCommon();
		
		wiType = workItem.getWorkItemType();
		try {
			IProcessConfigurationElement[] listConfigElements = advisorConfiguration.getChildren();
			if (listConfigElements != null) {
				for (IProcessConfigurationElement configElement : listConfigElements) {
					wiParser = new ParserWorkItemDto();
					wiDto = wiParser.parser(configElement);
					
					if (wiDto.getListaIDEstados() == null) {
						String alert = Messages.getStringWithArgs(Messages.MSG_ERRO_CONFIG_ESTADO,"Trava envio RM para o ITSM");
						IAdvisorInfo info = collector.createProblemInfo(alert,"",ERROR_MESSAGE_TYPE);
						collector.addInfo(info);
						return;
					}
					
					//Verifica se o item é RM
					if (!wiType.equals(wiDto.getType())) {
						continue;
					}
					//Verifica se o item está sendo criado agora
					if (workItem.isNewItem()) {
						continue;
					}
					
					//Verifica se o item é da classe emergencial
					Object idClassValue = getAttribute(workItem,workItemService, wiDto.getWiClassId(), monitor);
					if (idClassValue.equals(wiDto.getWiClassValue())) {
						continue;
					}
					
					//Verifica se o ambiente definido no item é HMP/PRD ou PRD
					Object idAmbienteValue = getAttribute(workItem,workItemService, wiDto.getWiAmbineteId(), monitor);
					if (idAmbienteValue.equals(wiDto.getWiAmbineteValue())) {
						continue;
					}
					
					if (!wiDto.getListaIDEstados().contains(workItem.getState2().getStringIdentifier().toLowerCase())) {
						continue;
					}

					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					cal.add(Calendar.HOUR, - wiDto.getIntervaloTempo());
					Date dateLimit = cal.getTime();
					
					IProjectAreaHandle projectArea = workItem.getProjectArea();
					
					WorkItemQueryModel model= WorkItemQueryModel.ROOT;
					AttributeQueryModel attributeModel = AttributeQueryModel.ROOT;
					IItemQuery query= IItemQuery.FACTORY.newInstance(model);
					IPredicate predicate = model.workItemType()._eq("FRM");
					predicate = predicate._and(model.projectArea()._eq(projectArea));
					predicate = predicate._and(model.stringExtensions().key()._eq("frm_classe_req_mudancas"))._and(model.stringExtensions().value()._eq("frm_classe_req_mudancas.literal.l6"));
					predicate = predicate._and(model.internalState()._eq(wiDto.getAttributeValue()));
					predicate = predicate._and(model.internalStateTransitions().targetState()._eq(wiDto.getAttributeValue())._and(model.internalStateTransitions().transitionDate()._ltOrEq(dateLimit)));
					
					System.out.println(new Date());
					System.out.println(dateLimit);
					
					query.filter(predicate);
					IItemQueryPage qPage = queryService.queryItemsInContext(query,IQueryService.EMPTY_PARAMETERS,IQueryService.ITEM_QUERY_MAX_PAGE_SIZE,new UUID[]{workItem.getProjectArea().getItemId()});
					
					System.out.println(qPage.getSize());
					List<IWorkItemHandle> handles = (List<IWorkItemHandle>)qPage.getItemHandles();
					for (IWorkItemHandle iWorkItemHandle : handles) {
						
						IWorkItem wiTemp = (IWorkItem) repository.fetchItem(iWorkItemHandle,IWorkItem.FULL_PROFILE.getPropertiesArray());
						IAttribute termoEmergencial = wiCommon.findAttribute(wiTemp.getProjectArea(),"termo_ciencia_implantacao_emergencial",monitor);
						List<Identifier> valor=  (List<Identifier>)wiTemp.getValue(termoEmergencial);

						if (!valor.contains(getLiteralEqualsString("termo_ciencia_implantacao_emergencial.literal.l2", termoEmergencial))) {
							 message(Messages.MSG_ERRO_TRAVA_ENVIO_RM_ITSM,Messages.MSG_ERRO_INVALID_LINK_DETAIL, collector);
							  return;
						}
					}
						
				}
			}
		} catch (Exception e) {
		    message(Messages.MSG_ERRO_UNEXPECTED_TRAVA_ENVIO_RM_ITSM, e.getMessage(),collector);
		    throw new TeamRepositoryException(e);
	}
    }

    private Identifier getLiteralEqualsString(String name, IAttributeHandle ia) throws TeamRepositoryException {

		Identifier literalID = null;
		IEnumeration enumeration = wiCommon.resolveEnumeration(ia, null); // or IWorkitemCommon
		List literals = enumeration.getEnumerationLiterals();
		for (Iterator iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();
			if (iLiteral.getIdentifier2().getStringIdentifier().equals(name.trim())) {
				literalID = iLiteral.getIdentifier2();
				break;
			}
		}
		return literalID;
	}
    
    
}
