package br.gov.caixa.rtc.alcadas;

import java.util.HashMap;

import br.gov.caixa.rtc.alcadas.parser.ParserAlcada;
import br.gov.caixa.rtc.exception.ApplicationException;
import br.gov.caixa.rtc.exception.DomainException;
import br.gov.caixa.rtc.reader.Reader;
import br.gov.caixa.rtc.util.HashFile;

public final class AlcadasFactory {
	public static final String FILE_CSV;
	private static final String MD5 = "md5";

	public static AlcadasFactory alcadas;
	private static String hashAtual;
	private HashMap<String, Alcada> hashAlcada = new HashMap<String, Alcada>();

	static {
		String pathFile = System.getenv("REPOSITORY_ADVISOR");
		if(pathFile==null){
			pathFile="/opt/IBM/JazzTeamServer/6.0.4/server/conf/ccm/sites/";
		}
		FILE_CSV = pathFile + "cargo.csv";
	}

	private AlcadasFactory() {

	}

	public static AlcadasFactory getInstance() throws DomainException,
			ApplicationException {

		if (alcadas == null) {
			alcadas = new AlcadasFactory();
		}
		String hashFiles;

		hashFiles = HashFile.hash(MD5, FILE_CSV);
		if (hashAtual == null || !hashAtual.equals(hashFiles)) {
			alcadas.inicializaHashAlcada();
			hashAtual = hashFiles;
		}
		return alcadas;

	}

	public Alcada getAlacada(final String id) throws ApplicationException {
		String hashFiles = HashFile.hash(MD5, FILE_CSV);
		if (hashAtual == null || !hashAtual.equals(hashFiles)) {
			alcadas.inicializaHashAlcada();
			hashAtual = hashFiles;
		}
		Alcada alcada = hashAlcada.get(id);
		if (alcada == null) {
			alcada = hashAlcada.get("Todos");
		}
		return alcada;
	}

	private void inicializaHashAlcada() throws ApplicationException {
		hashAlcada = new HashMap<String, Alcada>();
		Reader<ParserAlcada, Alcada> reader = new Reader<ParserAlcada, Alcada>(
				FILE_CSV, new ParserAlcada());
		Alcada alcada = reader.getLine();
		while (alcada != null) {
			hashAlcada.put(alcada.getId(), alcada);
			alcada = reader.getLine();
		}
		reader.close();
	}
}
