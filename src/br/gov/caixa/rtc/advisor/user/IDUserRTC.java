package br.gov.caixa.rtc.advisor.user;

import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.model.ContributorHandle;
import com.ibm.team.repository.service.AbstractService;
import com.ibm.team.repository.service.IRepositoryItemService;

public class IDUserRTC {
	private IItemHandle iItemHandle;
	private IRepositoryItemService repositoryServer;

	public IDUserRTC(AbstractService abstractService,
			IRepositoryItemService repositoryServer) {
		super();
		this.iItemHandle = abstractService.getAuthenticatedContributor();
		this.repositoryServer = repositoryServer;
	}

	public IDUserRTC(IItemHandle iItemHandle,
			IRepositoryItemService repositoryServer) {
		super();
		this.iItemHandle = iItemHandle;
		this.repositoryServer = repositoryServer;
	}

	public IContributor getUserLogado() throws ApplicationException {
		try {
			ContributorHandle userHandle = (ContributorHandle) iItemHandle;
			IContributor user = (IContributor) repositoryServer.fetchItem(
					userHandle, IRepositoryItemService.COMPLETE);
			return user;
		} catch (TeamRepositoryException e) {

			throw new ApplicationException("Erro ao obter usuário logado", e);
		}
	}
	
	public IContributor getUser(IContributorHandle usuario) throws ApplicationException  {
		try {
			return (IContributor) repositoryServer.fetchItem(usuario, IRepositoryItemService.COMPLETE);
		}catch (TeamRepositoryException e) {
			throw new ApplicationException("Erro ao obter usuário ", e);
			
		}
	}
	
	
}
