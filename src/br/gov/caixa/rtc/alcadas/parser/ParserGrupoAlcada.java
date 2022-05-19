//lpf

package br.gov.caixa.rtc.alcadas.parser;

import br.gov.caixa.rtc.alcadas.Alcada;

public class ParserGrupoAlcada implements Parser<Alcada, String> {

	private static final String SEPARADOR = ";";
	private static final String BLANCK_SPACE = "";

	
	public Alcada parser(String linha){
		String[] valores = linha.toString().split(SEPARADOR);
		Alcada alcada = new Alcada();
		int grupo = Integer.parseInt(valores[0]);
		alcada.setGrupo(grupo);
		if (valores.length > 1 && !valores[1].equalsIgnoreCase(BLANCK_SPACE)) {
			alcada.setId(new String(valores[1]));
		}
		return alcada;
	}



}
