package com.example.energif.model;

public enum Genero {
    MASCULINO('M', "Masculino"),
    FEMININO('F', "Feminino"),
    OUTRO('O', "Outro");

    private final Character codigo;
    private final String descricao;

    Genero(Character codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public Character getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static Genero fromCodigo(Character codigo) {
        if (codigo != null) {
            for (Genero genero : Genero.values()) {
                if (genero.codigo.equals(codigo) || genero.codigo.equals(Character.toUpperCase(codigo))) {
                    return genero;
                }
            }
        }
        return OUTRO;
    }

    public static Genero fromString(String text) {
        if (text != null) {
            for (Genero genero : Genero.values()) {
                if (text.equalsIgnoreCase(genero.name()) || 
                    text.equalsIgnoreCase(genero.descricao)) {
                    return genero;
                }
            }
        }
        return OUTRO;
    }
}
