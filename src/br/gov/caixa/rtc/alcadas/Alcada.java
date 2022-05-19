package br.gov.caixa.rtc.alcadas;

import java.math.BigDecimal;

import br.gov.caixa.rtc.exception.AlcadaException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.ldap.UserLdap;

import com.ibm.rational.services.rtc.advisor.messages.Messages;

public class Alcada {

	private String id;
	private BigDecimal valor;
	// lpf
	private int grupo;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	// lpf
	public int getGrupo() {
		return grupo;
	}

	public void setGrupo(int grupo) {
		this.grupo = grupo;
	}

	// fim lpf
	public void liberaAprovacao(BigDecimal valorProjeto, UserLdap user,
			String valorString) throws ApplicationException, AlcadaException {
		if (this.getValor() != null
				&& valorProjeto.compareTo(this.getValor()) > 0) {
			String alert = Messages.getStringWithArgs(
					Messages.MSG_ERRO_DETALHE_VALOR_SUPERIOR_ID, valorString,
					user.getNomeCargo());
			throw new AlcadaException(alert);

		}
	}

	// lpf
	public void liberaTermo(int grupo, String atributo, UserLdap user)
			throws ApplicationException, AlcadaException {
		if (this.getGrupo() < grupo) {
			String alert = Messages.getStringWithArgs(
					Messages.MSG_ERRO_DETALHE_LIBERACAO_TI_GESTOR, atributo,
					user.getNomeCargo());
			throw new AlcadaException(alert);
		}
	}
	// fim lpf

	// public Boolean liberaAprovacao(BigDecimal valorProjeto, UserLdap user){
	// return (this.getValor() != null &&
	// valorProjeto.compareTo(this.getValor()) > 0);
	// }

}
