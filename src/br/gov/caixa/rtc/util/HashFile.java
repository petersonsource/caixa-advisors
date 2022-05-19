package br.gov.caixa.rtc.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import br.gov.caixa.rtc.alcadas.AlcadasFactory;
import br.gov.caixa.rtc.exception.ApplicationException;

public class HashFile {
	private static final String ERRO_AO_GERAR_ARQUIVO_HASH = "Erro ao gerar arquivo HASH";
	private static char[] hexDigits = "0123456789ABCDEF".toCharArray();

	public static String hex(byte[] digest) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			sb.append(hexDigits[(digest[i] >> 4) & 0xF]);
			sb.append(hexDigits[digest[i] & 0xF]);
		}
		return sb.toString();
	}

	public static final String hash(String algorithm, String filename)
			throws ApplicationException {
		MessageDigest digest;
		DigestInputStream dis = null;
		try {
			digest = MessageDigest.getInstance(algorithm);
			dis = new DigestInputStream(new FileInputStream(filename), digest);
			byte[] bytes = new byte[1024];
			while (dis.read(bytes) > 0)
				;
			return hex(dis.getMessageDigest().digest());
		} catch (NoSuchAlgorithmException e) {

			throw new ApplicationException(ERRO_AO_GERAR_ARQUIVO_HASH+" "+AlcadasFactory.FILE_CSV+"- "+e.getMessage(),e);
		} catch (FileNotFoundException e) {

			throw new ApplicationException(ERRO_AO_GERAR_ARQUIVO_HASH+" "+AlcadasFactory.FILE_CSV+"- "+e.getMessage(),e);
		} catch (IOException e) {

			throw new ApplicationException(ERRO_AO_GERAR_ARQUIVO_HASH+" "+AlcadasFactory.FILE_CSV+"- "+e.getMessage(),e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					throw new ApplicationException(ERRO_AO_GERAR_ARQUIVO_HASH+" Fechar "+AlcadasFactory.FILE_CSV+"- ",e);
				}
			}
		}

	}

}