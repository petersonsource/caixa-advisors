package br.gov.caixa.rtc.exception;

/**
 * Todas as exeptions que a API lanca em sua maioria sao desse tipo, as exeptions do java ainda sao
 * lancadas normalmente, porem erros de configuracao, argumentos ou mesmo de logica sao lancados como
 * {@link APIException}
 * 
 * @author David da Silva Teles
 * @version 2.0
 */
public class APIException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public APIException() {}
	public APIException(String msg) {
		super(msg);
	}
	
	public APIException(Throwable e) {
		super(e);
	}
	
	public APIException(String msg, Throwable e) {
		super(msg, e);
	}
}
