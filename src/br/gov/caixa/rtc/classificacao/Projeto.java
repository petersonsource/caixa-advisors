package br.gov.caixa.rtc.classificacao;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Projeto {

	@JacksonXmlProperty(localName = "data", isAttribute = true)
	private String nome;

	@JacksonXmlProperty(localName = "classificacao")
	private String classificacao;

	public Projeto() {
	}

	public Projeto(String nome, String classificacao) {
		this.nome = nome;
		this.classificacao = classificacao;
	}

	public String getName() {
		return nome;
	}

	public void setName(String nome) {
		this.nome = nome;
	}

	public String getClassificacao() {
		return classificacao;
	}

	public void setClassificacao(String classificacao) {
		this.classificacao = classificacao;
	}

	@Override
	public String toString() {
		return "Projeto{" + "nome='" + nome + '\'' + ", classificacao='"
				+ classificacao + '\'' + '}';
	}

}
