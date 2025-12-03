package com.example.energif.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
    private Integer numeroVagasCadastroReserva;

    @OneToMany(mappedBy = "campusEdital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampusEditalTurno> turnos = new ArrayList<>();

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

    public Integer getNumeroVagasCadastroReserva() {
        return numeroVagasCadastroReserva;
    }

    public void setNumeroVagasCadastroReserva(Integer numeroVagasCadastroReserva) {
        this.numeroVagasCadastroReserva = numeroVagasCadastroReserva;
    }

    public List<CampusEditalTurno> getTurnos() {
        return turnos;
    }

    public void setTurnos(List<CampusEditalTurno> turnos) {
        this.turnos = turnos;
    }
}
