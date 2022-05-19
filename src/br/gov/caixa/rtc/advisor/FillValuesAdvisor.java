package br.gov.caixa.rtc.advisor;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.process.service.IProcessServerService;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.service.IWorkItemServer;

public class FillValuesAdvisor extends AbstractService
  implements IOperationAdvisor
{
  public void run(AdvisableOperation operation, IProcessConfigurationElement advisorConfiguration, IAdvisorInfoCollector collector, IProgressMonitor monitor)
    throws TeamRepositoryException
  {
    Object data = operation.getOperationData();

    if (data instanceof ISaveParameter) {
      ISaveParameter param = (ISaveParameter)data;
      IAuditable auditable = param.getNewState();
      if (auditable instanceof IWorkItem)
      {
        IWorkItem sourceworkItem = (IWorkItem)auditable;

        if (sourceworkItem.getWorkItemType().equals("pendência"))
          pendencia(param, sourceworkItem, monitor, collector);

        if (sourceworkItem.getWorkItemType().equals(
          "demanda_de_negocio")) {
          demandeDeNegocio(param, sourceworkItem, monitor, collector);
        }

        if (sourceworkItem.getWorkItemType().equals(
          "ocorrencia_de_erro")) {
          servico(param, sourceworkItem, monitor, collector);
        }

        if (sourceworkItem.getWorkItemType().equals("SolicitaServ"))
          servico(param, sourceworkItem, monitor, collector);
      }
    }
  }

  private void pendencia(ISaveParameter param, IWorkItem sourceworkItem, IProgressMonitor monitor, IAdvisorInfoCollector collector)
    throws TeamRepositoryException
  {
    IAuditableCommon auditableCommon = param.getSaveOperationParameter()
      .getAuditableCommon();
    IWorkItemCommon workItemCommon = 
      (IWorkItemCommon)auditableCommon
      .getPeer(IWorkItemCommon.class);

    IRepositoryItemService repositoryService = (IRepositoryItemService)getService(IRepositoryItemService.class);

    IContributor usuario = (IContributor)repositoryService.fetchItem(
      auditableCommon.getUser(), null);

    IProcessServerService processService = (IProcessServerService)getService(IProcessServerService.class);

    ITeamArea[] teamAreas = processService.findTeamAreas(
      usuario, sourceworkItem.getProjectArea(), null);

    IWorkItemServer workItemService = (IWorkItemServer)getService(IWorkItemServer.class);
    List<ICategory> listaCategorias = workItemService.findCategories(
      sourceworkItem.getProjectArea(), ICategory.FULL_PROFILE, 
      monitor);

    if (sourceworkItem.isNewItem())
    {
     IAdvisorInfo info;
      if (teamAreas.length == 0) {
        info = collector.createProblemInfo(
          "Usuário sem Associação.", 
          "Usuário precisa estar associado a uma GN", "");
        collector.addInfo(info);
        return;
      }

      label248: for (ICategory categoria : listaCategorias) {
        if (!(categoria.getName().equals(teamAreas[0].getName()))) break label248;
        IAttribute demandante = workItemCommon.findAttribute(
          sourceworkItem.getProjectArea(), 
          "pendencia.demandante", monitor);
        if ((demandante != null) && (sourceworkItem.hasAttribute(demandante)))
          sourceworkItem.setValue(demandante, categoria);
        return;
      }

      info = collector.createProblemInfo(
        "Categoria não Existente", teamAreas[0].getName(), "");
      collector.addInfo(info);
    }
  }

  private void servico(ISaveParameter param, IWorkItem sourceworkItem, IProgressMonitor monitor, IAdvisorInfoCollector collector)
    throws TeamRepositoryException
  {
    IAuditableCommon auditableCommon = param.getSaveOperationParameter()
      .getAuditableCommon();
    IWorkItemCommon workItemCommon = 
      (IWorkItemCommon)auditableCommon
      .getPeer(IWorkItemCommon.class);

    IRepositoryItemService repositoryService = (IRepositoryItemService)getService(IRepositoryItemService.class);

    IContributor usuario = (IContributor)repositoryService.fetchItem(
      auditableCommon.getUser(), null);

    IProcessServerService processService = (IProcessServerService)getService(IProcessServerService.class);

    ITeamArea[] teamAreas = processService.findTeamAreas(
      usuario, sourceworkItem.getProjectArea(), null);

    IWorkItemServer workItemService = (IWorkItemServer)getService(IWorkItemServer.class);
    List<ICategory> listaCategorias = workItemService.findCategories(
      sourceworkItem.getProjectArea(), ICategory.FULL_PROFILE, 
      monitor);

    if (sourceworkItem.isNewItem())
    {
      IAdvisorInfo info;
      ICategory categoriaASetar = null;

      if (teamAreas.length == 0) {
        info = collector.createProblemInfo(
          "Usuário sem Associação.", 
          "Usuário precisa estar associado a uma GN", "");
        collector.addInfo(info);
        return;
      }

      label213: for (ICategory categoria : listaCategorias) {
        if (!(categoria.getName().equals(teamAreas[0].getName()))) break label213;
        categoriaASetar = categoria;
        break;
      }

      if (categoriaASetar == null) {
        info = collector.createProblemInfo(
          "Categoria não Existente", teamAreas[0].getName(), "");
        collector.addInfo(info);
      }

      sourceworkItem.setCategory(categoriaASetar);
    }

    IAttribute produto = workItemCommon.findAttribute(sourceworkItem.getProjectArea(), "ocor_erro.produto", monitor);

    String nomeProduto = "";

    if (sourceworkItem.hasAttribute(produto)) {
      nomeProduto = (String)produto.getValue(auditableCommon, 
        sourceworkItem, monitor);
    }

    if ((nomeProduto.contains("(")) && (nomeProduto.contains(")"))) {
      ILiteral gecProdutoLiteral;
      IAttribute gecProduto = workItemCommon.findAttribute(sourceworkItem
        .getProjectArea(), "gec.produto", monitor);
      String idGecProduto = nomeProduto.substring(nomeProduto
        .indexOf("(") + 1, nomeProduto.indexOf(")"));

      if (sourceworkItem.hasAttribute(gecProduto)) {
        IEnumeration gecProdutoEnumeration = workItemCommon
          .resolveEnumeration(gecProduto, monitor);
        gecProdutoLiteral = null;

        Iterator localIterator2 = gecProdutoEnumeration
          .getEnumerationLiterals().iterator();

        while (localIterator2.hasNext()) {
          ILiteral literal = (ILiteral)localIterator2.next();
          if (literal.getName().equals(idGecProduto)) {
            gecProdutoLiteral = literal;
            break;
          }
        }

        if (gecProdutoLiteral != null) {
          sourceworkItem.setValue(gecProduto, gecProdutoLiteral
            .getIdentifier2());
          sourceworkItem.setValue(produto, nomeProduto.substring(0, 
            nomeProduto.indexOf(40) - 1));
        }
      }

      label589: for (ICategory categoria : listaCategorias) {
        if (!(categoria.getName().equals(idGecProduto))) break label589;
        IAttribute gecProdutoComoEnumeration = workItemCommon
          .findAttribute(sourceworkItem.getProjectArea(), 
          "gecproduto", monitor);
        if (!(sourceworkItem.hasAttribute(gecProdutoComoEnumeration))) break;
        sourceworkItem.setValue(gecProdutoComoEnumeration, 
          categoria);
        break;
      }

    }

    String nomeSistema = "";

    IAttribute sistema = workItemCommon.findAttribute(sourceworkItem
      .getProjectArea(), "sistema.catalogo", monitor);

    if (sourceworkItem.hasAttribute(sistema)) {
      nomeSistema = (String)sistema.getValue(auditableCommon, 
        sourceworkItem, monitor);
    }

    String gecCPM = "null";
    String matCPM = "null";

    if ((nomeSistema.contains("(")) && (nomeSistema.contains(")")) && 
      (nomeSistema.contains(","))) {
      gecCPM = nomeSistema.substring(nomeSistema.indexOf(40) + 1, 
        nomeSistema.indexOf(44));
      matCPM = nomeSistema.substring(nomeSistema.indexOf(",") + 1, 
        nomeSistema.indexOf(")"));
    }

    if (!(gecCPM.equals("null")))
    {
      ILiteral gecSistemaLiteral;
      IAttribute gecSistema = workItemCommon.findAttribute(sourceworkItem
        .getProjectArea(), "gec.sistema", monitor);
      if (sourceworkItem.hasAttribute(gecSistema)) {
        IEnumeration gecSistemasEnumeration = workItemCommon
          .resolveEnumeration(gecSistema, monitor);

        gecSistemaLiteral = null;

        Iterator localIterator3 = gecSistemasEnumeration
          .getEnumerationLiterals().iterator();

        while (localIterator3.hasNext()) {
          ILiteral literal = (ILiteral)localIterator3.next();
          if (literal.getName().equals(gecCPM)) {
            gecSistemaLiteral = literal;
            break;
          }

        }

        if (gecSistemaLiteral != null) {
          sourceworkItem.setValue(gecSistema, gecSistemaLiteral
            .getIdentifier2());
          sourceworkItem.setValue(sistema, nomeSistema.substring(0, 
            nomeSistema.indexOf("(") - 1));
        }
      }
      label979: for (ICategory categoria : listaCategorias) {
        if (!(categoria.getName().equals(gecCPM))) break label979;
        IAttribute gecSistemaComoEnumeration = workItemCommon
          .findAttribute(sourceworkItem.getProjectArea(), 
          "gecsistema", monitor);
        if (!(sourceworkItem.hasAttribute(gecSistemaComoEnumeration))) break;
        sourceworkItem.setValue(gecSistemaComoEnumeration, 
          categoria);
        break;
      }

    }

    if (!(matCPM.equals("null"))) {
      int i;
      IContributor[] contributors = processService
        .fetchMembersSortedByUserName(param.getNewProcessArea(), 
        512);
      for (i = 0; i < contributors.length; ++i)
        if (contributors[i].getUserId().equalsIgnoreCase(matCPM))
          sourceworkItem.getSubscriptions().add(
            (IContributorHandle)contributors[i]
            .getItemHandle());
    }
  }

  private void demandeDeNegocio(ISaveParameter param, IWorkItem sourceworkItem, IProgressMonitor monitor, IAdvisorInfoCollector collector)
    throws TeamRepositoryException
  {
    IAuditableCommon auditableCommon = param.getSaveOperationParameter()
      .getAuditableCommon();
    IWorkItemCommon workItemCommon = 
      (IWorkItemCommon)auditableCommon
      .getPeer(IWorkItemCommon.class);

    IRepositoryItemService repositoryService = (IRepositoryItemService)getService(IRepositoryItemService.class);

    IContributor usuario = (IContributor)repositoryService.fetchItem(
      auditableCommon.getUser(), null);

    IProcessServerService processService = (IProcessServerService)getService(IProcessServerService.class);

    ITeamArea[] teamAreas = processService.findTeamAreas(
      usuario, sourceworkItem.getProjectArea(), null);

    IWorkItemServer workItemService = (IWorkItemServer)getService(IWorkItemServer.class);
    List<ICategory> listaCategorias = workItemService.findCategories(
      sourceworkItem.getProjectArea(), ICategory.FULL_PROFILE, 
      monitor);

    if (sourceworkItem.isNewItem())
    {
      IAdvisorInfo info;
      ICategory categoriaASetar = null;

      if (teamAreas.length == 0) {
        info = collector.createProblemInfo(
          "Usuário sem Associação.", 
          "Usuário precisa estar associado a uma GN", "");
        collector.addInfo(info);
        return;
      }

      label213: for (ICategory categoria : listaCategorias) {
    	  if (categoria.getName().equals(teamAreas[0].getName())){
    		  categoriaASetar = categoria;
    	      break label213;	  
    	  }
//    	  
//        if (!(categoria.getName().equals(teamAreas[0].getName()))) break label213;
//        categoriaASetar = categoria;
//        break;
      }

      if (categoriaASetar == null) {
        info = collector.createProblemInfo(
          "Categoria não Existente", teamAreas[0].getName(), "");
        collector.addInfo(info);
      }

      sourceworkItem.setCategory(categoriaASetar);
    }

    IAttribute produto = workItemCommon.findAttribute(sourceworkItem
      .getProjectArea(), "ocor_erro.produto", monitor);

    String nomeProduto = "";

    if (sourceworkItem.hasAttribute(produto)) {
      nomeProduto = (String)produto.getValue(auditableCommon, 
        sourceworkItem, monitor);
    }

    if ((nomeProduto.contains("(")) && (nomeProduto.contains(")"))) {
      ILiteral gecProdutoLiteral;
      IAttribute gecProduto = workItemCommon.findAttribute(sourceworkItem
        .getProjectArea(), "gec.produto", monitor);

      String idGecProduto = nomeProduto.substring(nomeProduto
        .indexOf("(") + 1, nomeProduto.indexOf(")"));

      if (sourceworkItem.hasAttribute(gecProduto)) {
        IEnumeration gecProdutoEnumeration = workItemCommon
          .resolveEnumeration(gecProduto, monitor);
        gecProdutoLiteral = null;

        Iterator localIterator2 = gecProdutoEnumeration
          .getEnumerationLiterals().iterator();

        while (localIterator2.hasNext()) {
          ILiteral literal = (ILiteral)localIterator2.next();
          if (literal.getName().equals(idGecProduto)) {
            gecProdutoLiteral = literal;
            break;
          }
        }

        if (gecProdutoLiteral != null) {
          sourceworkItem.setValue(gecProduto, gecProdutoLiteral
            .getIdentifier2());
          sourceworkItem.setValue(produto, nomeProduto.substring(0, 
            nomeProduto.indexOf(40) - 1));
        }
      }

      label589: for (ICategory categoria : listaCategorias) {
        if (!(categoria.getName().equals(idGecProduto))) break label589;
        IAttribute gecProdutoComoEnumeration = workItemCommon
          .findAttribute(sourceworkItem.getProjectArea(), 
          "gecproduto", monitor);
        if (!(sourceworkItem.hasAttribute(gecProdutoComoEnumeration))) return;
        sourceworkItem.setValue(gecProdutoComoEnumeration, 
          categoria);
        return;
      }
    }
  }
}