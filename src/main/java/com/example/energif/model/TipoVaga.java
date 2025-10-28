package com.example.energif.model;




public enum TipoVaga {
    RESERVADA("Reservada"),
    AMPLA_CONCORRENCIA("Ampla Concorrência");
    
    private final String descricao;
    
    TipoVaga(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    // Método para converter de string para enum
    public static TipoVaga fromString(String text) {
        if (text != null) {
            for (TipoVaga tipo : TipoVaga.values()) {
                if (text.equalsIgnoreCase(tipo.name()) || 
                    text.equalsIgnoreCase(tipo.descricao) ||
                    text.toLowerCase().contains(tipo.name().toLowerCase().replace("_", " "))) {
                    return tipo;
                }
            }
        }
        return AMPLA_CONCORRENCIA; // padrão
    }
}