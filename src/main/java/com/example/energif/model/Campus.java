package com.example.energif.model;

import jakarta.persistence.*;
 
import lombok.Data;

@Data
@Entity
@Table(name = "campus")
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    private Integer numeroVagasReservadas;
    private Integer numeroVagasAmplaConcorrencia;

    // Removed collection mapping to Vaga because Vaga no longer contains a 'campus' property


    public Integer getNumeroVagasReservadas() {
        return numeroVagasReservadas;
    }

    public void setNumeroVagasReservadas(Integer numeroVagasReservadas) {
        this.numeroVagasReservadas = numeroVagasReservadas;
    }

    public Integer getNumeroVagasAmplaConcorrencia() {
        return numeroVagasAmplaConcorrencia;
    }

    public void setNumeroVagasAmplaConcorrencia(Integer numeroVagasAmplaConcorrencia) {
        this.numeroVagasAmplaConcorrencia = numeroVagasAmplaConcorrencia;
    }

    // Explicit getters/setters for 'id' and 'nome' to ensure Thymeleaf/SPEL can access them at runtime
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
