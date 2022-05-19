package br.gov.caixa.rtc.ldap;

import br.gov.caixa.rtc.alcadas.Alcada;

public class UserLdap {

	private String nome;
	private String idCargo;
	private String nomeCargo;
	private Alcada alcada;
	private String lotacaoFisica;
	private String lotacaoAdministrativa;
	private String nomeLotacaoFisica;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getIdCargo() {
		return idCargo;
	}

	public void setIdCargo(String idCargo) {
		this.idCargo = idCargo;
	}

	public String getNomeCargo() {
		return nomeCargo;
	}

	public void setNomeCargo(String nomeCargo) {
		this.nomeCargo = nomeCargo;
	}

	public Alcada getAlcada() {
		return alcada;
	}

	public void setAlcada(Alcada alcada) {
		this.alcada = alcada;
	}

	public String getLotacaoFisica() {
		return lotacaoFisica;
	}

	public void setLotacaoFisica(String lotacaoFisica) {
		this.lotacaoFisica = lotacaoFisica;
	}
	
	public String getCentroCustoLotacaoFisica() {
		String codigo = this.getLotacaoFisica().trim();
		int indice = codigo.length();
		indice = indice-4;
		return codigo.substring(indice);
	}
	
	public String getCentroCustoLotacaoAdministrativa() {
		String codigo = this.getLotacaoAdministrativa().trim();
		int indice = codigo.length();
		indice = indice-4;
		return codigo.substring(indice);
	}
	
	public String getSiglaNomeLotacao(){
		String sigla = getNomeLotacaoFisica().trim();
		if (sigla != null && sigla.contains("-")) {
			return sigla.subSequence(0, sigla.indexOf("-")).toString();
		}
		
		return getNomeLotacaoFisica();
		
	}
	
	public String getNomeLotacaoFisica() {
		return nomeLotacaoFisica;
	}

	public void setNomeLotacaoFisica(String nomeLotacaoFisica) {
		this.nomeLotacaoFisica = nomeLotacaoFisica;
	}

	public String getLotacaoAdministrativa() {
		return lotacaoAdministrativa;
	}

	public void setLotacaoAdministrativa(String lotacaoAdministrativa) {
		this.lotacaoAdministrativa = lotacaoAdministrativa;
	}
	
	

}
