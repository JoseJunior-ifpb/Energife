package com.example.energif.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "campus")
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    private Integer numeroVagasReservadas = 0;
    private Integer numeroVagasAmplaConcorrencia = 0;
    private Integer numeroVagasCadastroReserva = 0;
    
    // VAGAS OCUPADAS - IMPORTANTE: inicializar com 0
    private Integer vagasReservadasOcupadas = 0;
    private Integer vagasAmplaOcupadas = 0;

    // RELAÇÃO COM CANDIDATOS - CORRIGIDA
    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Candidato> candidatos = new ArrayList<>();

    // RELAÇÃO COM VAGAS - cada campus possui várias vagas
    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vaga> vagas = new ArrayList<>();

    // MÉTODOS CORRIGIDOS - garantir que nunca retornem null
    public boolean temVagaReservadaDisponivel() {
        return getVagasReservadasDisponiveis() > 0;
    }
    
    public boolean temVagaAmplaDisponivel() {
        return getVagasAmplaDisponiveis() > 0;
    }
    
    public Integer getVagasReservadasDisponiveis() {
        int total = getNumeroVagasReservadas();
        int ocupadas = getVagasReservadasOcupadas();
        return Math.max(0, total - ocupadas);
    }
    
    public Integer getVagasAmplaDisponiveis() {
        int total = getNumeroVagasAmplaConcorrencia();
        int ocupadas = getVagasAmplaOcupadas();
        return Math.max(0, total - ocupadas);
    }

    // GETTERS E SETTERS SEGUROS - sempre retornam valores, nunca null
    public Integer getNumeroVagasReservadas() {
        return numeroVagasReservadas != null ? numeroVagasReservadas : 0;
    }

    public void setNumeroVagasReservadas(Integer numeroVagasReservadas) {
        this.numeroVagasReservadas = numeroVagasReservadas != null ? numeroVagasReservadas : 0;
    }

    public Integer getNumeroVagasAmplaConcorrencia() {
        return numeroVagasAmplaConcorrencia != null ? numeroVagasAmplaConcorrencia : 0;
    }

    public void setNumeroVagasAmplaConcorrencia(Integer numeroVagasAmplaConcorrencia) {
        this.numeroVagasAmplaConcorrencia = numeroVagasAmplaConcorrencia != null ? numeroVagasAmplaConcorrencia : 0;
    }

    public Integer getNumeroVagasCadastroReserva() {
        return numeroVagasCadastroReserva != null ? numeroVagasCadastroReserva : 0;
    }

    public void setNumeroVagasCadastroReserva(Integer numeroVagasCadastroReserva) {
        this.numeroVagasCadastroReserva = numeroVagasCadastroReserva != null ? numeroVagasCadastroReserva : 0;
    }

    public Integer getVagasReservadasOcupadas() {
        return vagasReservadasOcupadas != null ? vagasReservadasOcupadas : 0;
    }

    public void setVagasReservadasOcupadas(Integer vagasReservadasOcupadas) {
        this.vagasReservadasOcupadas = vagasReservadasOcupadas != null ? vagasReservadasOcupadas : 0;
    }

    public Integer getVagasAmplaOcupadas() {
        return vagasAmplaOcupadas != null ? vagasAmplaOcupadas : 0;
    }

    public void setVagasAmplaOcupadas(Integer vagasAmplaOcupadas) {
        this.vagasAmplaOcupadas = vagasAmplaOcupadas != null ? vagasAmplaOcupadas : 0;
    }

    // MÉTODO getCandidatos() CORRIGIDO
    public List<Candidato> getCandidatos() {
        return candidatos;
    }

    public void setCandidatos(List<Candidato> candidatos) {
        this.candidatos = candidatos;
    }

    // GETTERS E SETTERS PARA VAGAS
    public List<Vaga> getVagas() {
        return vagas;
    }

    public void setVagas(List<Vaga> vagas) {
        this.vagas = vagas != null ? vagas : new ArrayList<>();
    }

    // Explicit getters for id and nome to avoid relying entirely on Lombok in all environments
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    // MÉTODO PARA ADICIONAR CANDIDATO
    public void addCandidato(Candidato candidato) {
        candidatos.add(candidato);
        candidato.setCampus(this);
    }

    // MÉTODO PARA REMOVER CANDIDATO
    public void removeCandidato(Candidato candidato) {
        candidatos.remove(candidato);
        candidato.setCampus(null);
    }
}