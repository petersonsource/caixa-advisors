package com.ibm.rational.services.rtc.advisor.messages;

import java.text.MessageFormat;
import java.util.MissingResourceException;

import br.gov.caixa.rtc.exception.ApplicationException;

import com.ibm.team.repository.service.ClientMessages;

public class Messages {
    private static final String BUNDLE_NAME = "com.ibm.rational.services.rtc.advisor.messages.messages"; //$NON-NLS-1$
    private static final Messages SELF = new Messages();
    public static final String ACAO_NAO_PERMITIDA = getString("br.gov.caixa.mensagem.erro.acao.nao.permitida");
    public static final String MSG_ERRO_USUARIO_NAO_FUNCIONARIO = getString("br.gov.caixa.mensagem.erro.usuari.nao.funcionario");
    public static final String MSG_ERRO_USUARIO_NAO_FUNCIONARIO_RESPONSAVEL_CUSTO = getString("br.gov.caixa.mensagem.erro.usuari.nao.funcionario.responsavel.custo");
    public static final String MSG_ERRO_FUNCIONARIO_RESPONSAVEL_CUSTO_HERDADO = getString("br.gov.caixa.mensagem.erro.responsavel.custo.valor.herdado");
    public static final String MSG_ERRO_TITLE_VALOR_SUPERIOR = getString("br.gov.caixa.mensagem.erro.title.valor.superior");
    public static final String MSG_ERRO_DETALHE_VALOR_SUPERIOR_ID = "br.gov.caixa.mensagem.erro.detalhe.valor.superior";
    public static final String MSG_ERRO_FORMATO_MONETARIO = getString("br.gov.caixa.mensagem.erro.formato.monetario");
    // lpf
    public static final String MSG_ERRO_LIBERACAO_TI_GESTOR = getString("br.gov.caixa.mensagem.erro.liberacao.ti.gestor");
    public static final String MSG_ERRO_DETALHE_LIBERACAO_TI_GESTOR = getString("br.gov.caixa.mensagem.erro.detalhe.liberacao.ti.gestor");
    public static final String MSG_ERRO_ITENS_NAO_VALIDADOS = getString("br.gov.caixa.mensagem.erro.itens.nao.validados");
    public static final String MSG_ERRO_RELACIONAMENTO_PRD = getString("br.gov.caixa.mensagem.erro.relacionamento.prd");
    public static final String MSG_ALERTA_CONTATO_QUALIDADE = getString("br.gov.caixa.mensagem.alerta.contato.qualidade");
    public static final String MSG_ERRO_ALCADA_STATUS = getString("br.gov.caixa.mensagem.erro.alcada.status");
    public static final String MSG_ERRO_PERFIL = getString("br.gov.caixa.mensagem.erro.perfil");
    public static final String MSG_ERRO_RELACIONAMENTO_ERRADO = getString("br.gov.caixa.mensagem.erro.relacionamento.errado");

    // Msgs Advisor Criar Tarefas a partir do modelo - Solicitação do Usuário
    public static final String MSG_ERRO_CONFIG_TEMPLATE = getString("br.gov.caixa.mensagem.erro.template.configuracao");
    public static final String MSG_ERRO_CRIAR_TAREFAS = getString("br.gov.caixa.mensagem.erro.criar.tarefas");

    // Verifica hierarquia
    public static final String MSG_ERRO_INVALID_LINK = getString("br.gov.caixa.mensagem.erro.link.invalid");
    public static final String MSG_ERRO_INVALID_LINK_DETAIL = getString("br.gov.caixa.mensagem.erro.link.invalid.detail");
    public static final String MSG_ERRO_NO_LINKS = getString("br.gov.caixa.mensagem.erro.no.links");
    public static final String MSG_ERRO_NO_LINKS_DETAIL = getString("br.gov.caixa.mensagem.erro.no.links.detail");
    public static final String MSG_ERRO_UNEXPECTED_LINKS = getString("br.gov.caixa.mensagem.erro.unexpected.links");

    public static final String MSG_ERRO_CONFIG_ESTADO = getString("br.gov.caixa.mensagem.erro.configuracao.estado");
    public static final String MSG_ERRO_CONFIG_ITEM_RELACIONADO = getString("br.gov.caixa.mensagem.erro.configuracao.item.relacionado");
    public static final String MSG_ERRO_BLOQUEIO_EDICAO_LINK_RELACIONADO = getString("br.gov.caixa.mensagem.erro.bloqueio.edicao.link.relacionado");
    public static final String MSG_ERRO_UNEXPECTED_BLOQUEIO_EDICAO_LINK_RELACIONADO = getString("br.gov.caixa.mensagem.erro.unexpected.bloqueio.edicao.link.relacionado");
    public static final String MSG_ERRO_TRAVA_ENVIO_RM_ITSM = getString("br.gov.caixa.mensagem.erro.trava.envio.rm.itsm");
    public static final String MSG_ERRO_UNEXPECTED_TRAVA_ENVIO_RM_ITSM = getString("br.gov.caixa.mensagem.erro.unexpected.trava.envio.rm.itsm");

    // INCIDENTE
    public static final String MSG_INCIDENTE_NAO_ENCONTRADO = getString("br.gov.caixa.mensagem.erro.incidente.nao.encontrado");
    public static final String MSG_ERRO_ATUALIZAR_INCIDENTE = getString("br.gov.caixa.mensagem.erro.atualizar.incidente");
    public static final String MSG_USUARIO_NAO_ENCONTRADO = getString("br.gov.caixa.mensagem.erro.usuario.nao.encontrado");
    public static final String MSG_INCIDENTE_CAMPO_NAO_PREENCHIDO = getString("br.gov.caixa.mensagem.erro.campo.nao.preenchido");
    public static final String MSG_FALTA_ID_INTERNO_INCIDENTE = getString("br.gov.caixa.mensagem.erro.incidente.falta.id.interno");
    public static final String MSG_ERRO_TEAM_REPOSITORY_INCIDENTE = getString("br.gov.caixa.mensagem.erro.teamRepository.incidente");
    public static final String MSG_ERRO_CRIACAO_JSON_INCIDENTE = getString("br.gov.caixa.mensagem.erro.criacao.json.incidente");
    public static final String MSG_ERRO_ENCODING_INCIDENTE = getString("br.gov.caixa.mensagem.erro.encoding.incidente");
    public static final String MSG_ERRO_ARQUIVO_NAO_ENCONTRADO = getString("br.gov.caixa.mensagem.erro.arquivo.nao.encontrado");

    // Erro por falta de sincronismo

    public static final String MSG_ERRO_ATRIBUTO_NAO_SINCRONIZADO = getString("br.gov.caixa.mensagem.erro.nao.possivel.recuperar.valor");
    
    // Erro advisor PaiTipoEspecificoObrigatorioCriacaoItem
    public static final String MSG_ERRO_PAI_ESPECIFICO_OBRIGATORIO_PARA_WI = getString("br.gov.caixa.mensagem.erro.pai.especifico.obrigatorio");

    // fim lpf
    private Messages() {
    }

    public static String getString(String key) {
	try {
	    return ClientMessages.getString(SELF, BUNDLE_NAME, key);
	} catch (MissingResourceException e) {
	    return key;
	}
    }

    public static String getStringWithArgs(String idMsg, String... key)
	    throws ApplicationException {
	try {
	    String msgSt = getString(idMsg);
	    MessageFormat messageFormat = new MessageFormat(msgSt);
	    return messageFormat.format(key);
	} catch (MissingResourceException e) {
	    throw new ApplicationException("Erro ao obter a mensagem", e);
	}
    }

}
