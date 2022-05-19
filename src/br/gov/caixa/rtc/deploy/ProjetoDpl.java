package br.gov.caixa.rtc.deploy;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ProjetoDpl {

    @JacksonXmlProperty(localName = "data", isAttribute = true)
    private String nome;

    @JacksonXmlProperty(localName = "tipoDeploy")
    private String tipoDeploy;

    public ProjetoDpl() {
    }

    public ProjetoDpl(String nome, String tipoDeploy) {
	this.nome = nome;
	this.tipoDeploy = tipoDeploy;
    }

    public String getName() {
	return nome;
    }

    public void setName(String nome) {
	this.nome = nome;
    }

    public String getTipoDeploy() {
	return tipoDeploy;
    }

    public void setTipoDeploy(String tipoDeploy) {
	this.tipoDeploy = tipoDeploy;
    }

    @Override
    public String toString() {
	return "Projeto{" + "nome='" + nome + '\'' + ", tipoDeploy='"
		+ tipoDeploy + '\'' + '}';
    }

}
