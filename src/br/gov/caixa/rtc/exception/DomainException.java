package br.gov.caixa.rtc.exception;

public class DomainException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5781027115165338780L;

	public DomainException() {
		super();

	}

	public DomainException(String message, Throwable cause) {
		super(message, cause);

	}

	public DomainException(String message) {
		super(message);

	}

	public DomainException(Throwable cause) {
		super(cause);

	}

}
