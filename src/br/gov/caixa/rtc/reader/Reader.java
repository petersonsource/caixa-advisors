package br.gov.caixa.rtc.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import br.gov.caixa.rtc.alcadas.parser.Parser;
import br.gov.caixa.rtc.exception.ApplicationException;


public class Reader<P extends Parser<V, String>, V> {
	private BufferedReader br;
	private FileReader fr;
	private P parser;

	public Reader(String fileName, P parser) throws ApplicationException {
		super();
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
			this.parser = parser;
		} catch (FileNotFoundException e) {

			throw new ApplicationException("Não pode ler o arquvi csv", e);
		}
	}

	public V getLine() throws ApplicationException {
		try {
			String linha = br.readLine();

			V objeto = null;
			if (linha != null) {
				objeto = parser.parser(linha);
			}
			return objeto;
		} catch (IOException e) {

			throw new ApplicationException("Não pode ler a linah do arquivo", e);
		}
	}

	public void close() throws ApplicationException {

		try {
			br.close();
		} catch (IOException e) {

			throw new ApplicationException("Erro ao fechar arquivo", e);
		}
	}

}
