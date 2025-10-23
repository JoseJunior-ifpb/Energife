package com.example.energif.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "edital")
public class Edital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer numeroInscritos;

    @OneToMany(mappedBy = "edital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.example.energif.model.CampusEdital> campusEditais = new ArrayList<>();

    @OneToMany(mappedBy = "edital")
    private List<com.example.energif.model.Candidato> candidatos = new ArrayList<>();

    // descrição textual do edital (p.ex. "edital n°25 2025.1")
    private String descricao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumeroInscritos() {
        return numeroInscritos;
    }

    public void setNumeroInscritos(Integer numeroInscritos) {
        this.numeroInscritos = numeroInscritos;
    }

    public List<com.example.energif.model.CampusEdital> getCampusEditais() {
        return campusEditais;
    }

    public List<com.example.energif.model.Candidato> getCandidatos() {
        return candidatos;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

}
