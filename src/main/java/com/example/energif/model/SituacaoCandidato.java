package com.example.energif.model;

/**
 * Enum que representa as possíveis situações de um candidato no processo seletivo.
 */
public enum SituacaoCandidato {
    CLASSIFICADO("Classificado"),
    HABILITADO("Habilitado"),
    NAO_CLASSIFICADO("Não classificado"),
    CADASTRO_RESERVA("Cadastro de Reserva"),
    ELIMINADO("Eliminado");

    private final String descricao;

    SituacaoCandidato(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
