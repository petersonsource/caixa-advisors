package br.gov.caixa.rtc.alcadas.parser;

import java.math.BigDecimal;

import br.gov.caixa.rtc.alcadas.Alcada;


public class ParserAlcada implements Parser<Alcada, String> {

	private static final String SEPARADOR = ";";
	private static final String BLANCK_SPACE = "";

	
	public Alcada parser(String linha) {
		String[] valores = linha.toString().split(SEPARADOR);
		Alcada alcada = new Alcada();
		alcada.setId(valores[1]);
		if (valores.length > 2 && !valores[2].equalsIgnoreCase(BLANCK_SPACE)) {
			alcada.setValor(new BigDecimal(valores[2]));
		}
		return alcada;
	}

}
