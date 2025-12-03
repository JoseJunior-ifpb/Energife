package com.example.energif.model;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_edital_turno", uniqueConstraints = @UniqueConstraint(columnNames = {"campus_edital_id","turno"}))
public class CampusEditalTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campus_edital_id")
    private CampusEdital campusEdital;

    @Column(nullable = false)
    private String turno; // e.g. MANHA, TARDE, NOITE, UNICO

    private Integer numeroVagasReservadas = 0;
    private Integer numeroVagasAmplaConcorrencia = 0;
    private Integer numeroVagasCadastroReserva = 0;

    private Integer vagasReservadasOcupadas = 0;
    private Integer vagasAmplaOcupadas = 0;
    private Integer vagasCadastroReservaOcupadas = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CampusEdital getCampusEdital() {
        return campusEdital;
    }

    public void setCampusEdital(CampusEdital campusEdital) {
        this.campusEdital = campusEdital;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

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

    public Integer getVagasCadastroReservaOcupadas() {
        return vagasCadastroReservaOcupadas != null ? vagasCadastroReservaOcupadas : 0;
    }

    public void setVagasCadastroReservaOcupadas(Integer vagasCadastroReservaOcupadas) {
        this.vagasCadastroReservaOcupadas = vagasCadastroReservaOcupadas != null ? vagasCadastroReservaOcupadas : 0;
    }

    public int getVagasReservadasDisponiveis() {
        return Math.max(0, getNumeroVagasReservadas() - getVagasReservadasOcupadas());
    }

    public int getVagasAmplaDisponiveis() {
        return Math.max(0, getNumeroVagasAmplaConcorrencia() - getVagasAmplaOcupadas());
    }

    public int getVagasCadastroReservaDisponiveis() {
        return Math.max(0, getNumeroVagasCadastroReserva() - getVagasCadastroReservaOcupadas());
    }
}
