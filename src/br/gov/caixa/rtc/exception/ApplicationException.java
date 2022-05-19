package br.gov.caixa.rtc.exception;

public class ApplicationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5615077552892818415L;

	public ApplicationException() {
		super();

	}

	public ApplicationException(String message, Throwable cause) {
		super(message, cause);

	}

	public ApplicationException(String message) {
		super(message);

	}

	public ApplicationException(Throwable cause) {
		super(cause);

	}

}
