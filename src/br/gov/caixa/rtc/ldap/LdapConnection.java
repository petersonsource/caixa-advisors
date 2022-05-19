package br.gov.caixa.rtc.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import br.gov.caixa.rtc.exception.ApplicationException;

public class LdapConnection {

	private static final String PARAMETER_NAME = "cn";
	private static final String PARAMETER_NAME_CARGO = "title";
	private static final String PARAMETER_ID_CARGO = "extensionattribute12";
	private static final String PARAMETER_LOTACAO_ADMINISTRATIVA = "extensionattribute1";
	private static final String PARAMETER_LOTACAO_FISICA = "extensionattribute10";
	private static final String PARAMETER_NOME_LOTACAO_FISICA = "Department";	
	private static final String ERRO_CONSULTAR_LDAP = "Erro ao realizar consulta no LDAP";
	private static final String USUARIO_SEM_ID_CARGO = "Usuário selecionado não possui um ID de função atribuído a ele no LDAP";
	private static final String ERRO_LDAP = "Erro ao se comunicar com o LDAP";
//	private static final String URL_CONNECTION = "ldap://df0000sr750.corp.caixa.gov.br:389";
	private static final String URL_CONNECTION = "ldap://corp.caixa.gov.br:389";
	private static final String CLAZZ_LDAP_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String PASSWORD = "Senha01";
	private static final String USUARIO = "s000071";
	private static final String AUTENTICATION_TYPE = "simple";
	private static final String QUERY = "OU=Empregados,OU=Usuarios,OU=CAIXA,DC=corp,DC=caixa,DC=gov,DC=br";
	private static final String QUERY_ALL = "OU=Usuarios,OU=CAIXA,DC=corp,DC=caixa,DC=gov,DC=br";
	private final DirContext ctx;
	public Hashtable<String, Object> env = new Hashtable<String, Object>(5,
			0.75f);

	public LdapConnection() throws ApplicationException {
		super();
		try {
			env.put(Context.SECURITY_AUTHENTICATION, AUTENTICATION_TYPE);
			env.put(Context.SECURITY_PRINCIPAL, USUARIO);
			env.put(Context.SECURITY_CREDENTIALS, PASSWORD);
			env.put(Context.INITIAL_CONTEXT_FACTORY, CLAZZ_LDAP_FACTORY);
			env.put(Context.PROVIDER_URL, URL_CONNECTION);
			ctx = new InitialDirContext(env);
		} catch (NamingException e) {
			throw new ApplicationException(ERRO_LDAP, e);

		}
	}

	public UserLdap findUserById(final String login)
			throws ApplicationException {

		try {
			SearchControls search = new SearchControls();
			search.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration results = ctx.search(QUERY,
					"(&(objectClass=user)(sAMAccountName=" + login + "))",
					search);
			UserLdap usuarioLdap = null;
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();
				Attributes attributes = searchResult.getAttributes();
				usuarioLdap = new UserLdap();
				if (attributes.get(PARAMETER_ID_CARGO) != null ) {
					usuarioLdap.setIdCargo(attributes.get(PARAMETER_ID_CARGO).get().toString());
					usuarioLdap.setNome(attributes.get(PARAMETER_NAME).get().toString());
					usuarioLdap.setNomeCargo(attributes.get(PARAMETER_NAME_CARGO).get().toString());
					usuarioLdap.setLotacaoFisica(attributes.get(PARAMETER_LOTACAO_FISICA).get().toString());
					usuarioLdap.setNomeLotacaoFisica(attributes.get(PARAMETER_NOME_LOTACAO_FISICA).get().toString());					

//				}
				} else{
					throw new ApplicationException(USUARIO_SEM_ID_CARGO, new Throwable("Usuário selecionado não possui um ID de função atribuído a ele no LDAP. Favor verificar"));
				}
			}
			return usuarioLdap;
		} catch (NamingException e) {
			throw new ApplicationException(ERRO_CONSULTAR_LDAP, e);

		}finally{
			try {
				ctx.close();
			} catch (NamingException e) {
				throw new ApplicationException(ERRO_CONSULTAR_LDAP, e);
			}
		}

	}
	
	public UserLdap recoverPhysicalLocationUserbyID(final String login)
			throws ApplicationException {

		try {
			SearchControls search = new SearchControls();
			search.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration results = ctx.search(QUERY_ALL,
					"(&(objectClass=user)(sAMAccountName=" + login + "))",
					search);
			UserLdap usuarioLdap = null;
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();
				Attributes attributes = searchResult.getAttributes();
				usuarioLdap = new UserLdap();
				usuarioLdap.setLotacaoAdministrativa(attributes.get(PARAMETER_LOTACAO_ADMINISTRATIVA).get().toString());
				usuarioLdap.setLotacaoFisica(attributes.get(PARAMETER_LOTACAO_FISICA).get().toString());
				usuarioLdap.setNomeLotacaoFisica(attributes.get(PARAMETER_NOME_LOTACAO_FISICA).get().toString());					
				
			}
			return usuarioLdap;
		} catch (NamingException e) {
			throw new ApplicationException(ERRO_CONSULTAR_LDAP, e);

		}finally{
			try {
				ctx.close();
			} catch (NamingException e) {
				throw new ApplicationException(ERRO_CONSULTAR_LDAP, e);
			}
		}

	}
	
}
