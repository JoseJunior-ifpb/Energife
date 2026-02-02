package com.example.energif.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "motivo")
@Data
public class Motivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    // Getter explícito para garantir acesso ao Thymeleaf
    public String getDescricao() {
        return this.descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
