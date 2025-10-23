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
    @JoinColumn(name = "cpf_candidato")
    private Candidato cpfCandidato;

}
