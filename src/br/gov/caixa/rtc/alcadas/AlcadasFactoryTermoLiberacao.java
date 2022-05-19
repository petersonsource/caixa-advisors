//lpf

package br.gov.caixa.rtc.alcadas;

import java.util.HashMap;

import br.gov.caixa.rtc.alcadas.parser.ParserGrupoAlcada;
import br.gov.caixa.rtc.exception.AlcadaException;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.exception.DomainException;
import br.gov.caixa.rtc.reader.Reader;
import br.gov.caixa.rtc.util.HashFile;

import com.ibm.rational.services.rtc.advisor.messages.Messages;

public final class AlcadasFactoryTermoLiberacao {
	public static final String FILE_CSV;
	private static final String MD5 = "md5";

	public static AlcadasFactoryTermoLiberacao alcadas;
	private static String hashAtual;
	private HashMap<String, Alcada> hashAlcada = new HashMap<String, Alcada>();

	static {
		String pathFile = System.getenv("REPOSITORY_ADVISOR");
		if(pathFile==null){
			pathFile="/opt/IBM/JazzTeamServer/6.0.4/server/conf/ccm/sites/";
		}
		FILE_CSV = pathFile + "alcadas_liberacao.csv";
	}

	private AlcadasFactoryTermoLiberacao() {

	}

	public static AlcadasFactoryTermoLiberacao getInstance() throws DomainException,
			ApplicationException {

		if (alcadas == null) {
			alcadas = new AlcadasFactoryTermoLiberacao();
		}
		String hashFiles;

		hashFiles = HashFile.hash(MD5, FILE_CSV);
		if (hashAtual == null || !hashAtual.equals(hashFiles)) {
			alcadas.inicializaHashAlcada();
			hashAtual = hashFiles;
		}
		return alcadas;

	}

	public Alcada getGrupo(final String id) throws ApplicationException, AlcadaException {
		String hashFiles = HashFile.hash(MD5, FILE_CSV);
		if (hashAtual == null || !hashAtual.equals(hashFiles)) {
			alcadas.inicializaHashAlcada();
			hashAtual = hashFiles;
		}
		Alcada alcada = hashAlcada.get(id);
		if (alcada == null) {
			String alert = Messages.getStringWithArgs(
			Messages.MSG_ERRO_LIBERACAO_TI_GESTOR);
			throw new AlcadaException(alert);
		}
		return alcada;
	}
	
	private void inicializaHashAlcada() throws ApplicationException {
		hashAlcada = new HashMap<String, Alcada>();
		Reader<ParserGrupoAlcada, Alcada> reader = new Reader<ParserGrupoAlcada, Alcada>(
				FILE_CSV, new ParserGrupoAlcada());
		Alcada alcada = reader.getLine();
		while (alcada != null) {
			hashAlcada.put(alcada.getId(), alcada);
			alcada = reader.getLine();
		}
		reader.close();
	}
}
