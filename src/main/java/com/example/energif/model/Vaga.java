package com.example.energif.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // Tipo da vaga: "RESERVADA" ou "AMPLA_CONCORRENCIA"
    private String tipoVaga;

    // REMOVIDO: @ManyToMany com Candidato
    // A relação é indireta através do Campus
}