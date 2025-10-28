package com.example.energif.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;


// No Campus.java - garanta que os métodos estão assim:
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
    
    // VAGAS OCUPADAS - IMPORTANTE: inicializar com 0
    private Integer vagasReservadasOcupadas = 0;
    private Integer vagasAmplaOcupadas = 0;

    // ... outros campos e relações

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

    public List<Vaga> getVagas() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVagas'");
    }

    public Object getCandidatos() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCandidatos'");
    }
}