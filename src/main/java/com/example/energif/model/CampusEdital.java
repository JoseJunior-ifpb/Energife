package com.example.energif.model;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_edital", uniqueConstraints = @UniqueConstraint(columnNames = {"campus_id","edital_id"}))
public class CampusEdital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "edital_id")
    private Edital edital;

    private Integer numeroVagasReservadas;
    private Integer numeroVagasAmplaConcorrencia;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public Edital getEdital() {
        return edital;
    }

    public void setEdital(Edital edital) {
        this.edital = edital;
    }

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
}
