package br.gov.caixa.rtc.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.rational.services.rtc.advisor.messages.Messages;

public class NumberFormater {
	private static final String PATTNER_INVALIDO = "^(?:[1-9](?:[\\d]{0,2}(?:\\.[\\d]{3})*)|0)(?:,[\\d]{2})$";

	private static void validaPattner(String valor) throws ApplicationException {
		Pattern pattern = Pattern.compile(PATTNER_INVALIDO);
		Matcher matcher = pattern.matcher(valor);
		if (!matcher.matches()) {
			throw new ApplicationException(Messages.MSG_ERRO_FORMATO_MONETARIO);
		}
	}

	public static BigDecimal parserStringToNumber(String valor)

	throws ApplicationException {
		try {
			validaPattner(valor);
			DecimalFormat nf = new DecimalFormat("#,##0.00",
					new DecimalFormatSymbols(new Locale("pt", "BR")));
			return new BigDecimal(nf.parse(valor).toString());
		} catch (java.lang.NumberFormatException e) {
			throw new ApplicationException(Messages.MSG_ERRO_FORMATO_MONETARIO);
		} catch (ParseException e) {
			throw new ApplicationException(Messages.MSG_ERRO_FORMATO_MONETARIO);
		}

	}
}
