package br.gov.caixa.rtc.classificacao;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "root")
public class ReportableListClassificacao {

	@JacksonXmlProperty(localName = "projeto")
	private List<Projeto> projects;

	public ReportableListClassificacao() {
	}

	public List<Projeto> getProjects() {
		return projects;
	}

	public void setProjects(List<Projeto> projects) {
		this.projects = projects;
	}

	@Override
	public String toString() {
		return "ProjectList: " + (projects != null ? projects.size() : null);
	}
}
