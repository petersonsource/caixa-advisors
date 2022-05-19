package br.gov.caixa.rtc.deploy;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "root")
public class ReportableListDeploy {

	@JacksonXmlProperty(localName = "projeto")
	private List<ProjetoDpl> projects;

	public ReportableListDeploy() {
	}

	public List<ProjetoDpl> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjetoDpl> projects) {
		this.projects = projects;
	}

	@Override
	public String toString() {
		return "ProjectList: " + (projects != null ? projects.size() : null);
	}
}
