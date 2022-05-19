package br.gov.caixa.rtc.alcadas.parser;

public interface Parser<T, O> {
	T parser(O object);
}
