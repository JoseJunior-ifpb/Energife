package com.example.energif.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
@Entity
@Table(name = "candidato")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "candidato_seq")
    @SequenceGenerator(name = "candidato_seq", sequenceName = "candidato_seq", allocationSize = 1)
    private Long id;

    private String nome;
    private String cpf;
    private LocalDate dataNascimento;

    @ManyToOne
    @JoinColumn(name = "campus_id")
    private Campus campus;

    private String turno;

    private Character genero;

    private LocalDate dataInscricao;

    private LocalTime horaInscricao;

    @ManyToOne
    @JoinColumn(name = "edital_id")
    private Edital edital;

    // whether the candidate is habilitado for the edital/process
    private Boolean habilitado = Boolean.FALSE;
    // motivo quando não habilitado
    private String motivoNaoHabilitacao;

    @ManyToMany
    @JoinTable(name = "candidato_vaga",
            joinColumns = @JoinColumn(name = "candidato_id"),
            inverseJoinColumns = @JoinColumn(name = "vaga_id"))
    private Set<Vaga> vagasApplied = new HashSet<>();

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public Campus getCampus() {
        return this.campus;
    }

    public void setEdital(Edital edital) {
        this.edital = edital;
    }

    public Edital getEdital() {
        return this.edital;
    }

    public Long getId() {
        return this.id;
    }

    // explicit getters/setters for form-bound properties
    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return this.dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTurno() {
        return this.turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public Character getGenero() {
        return this.genero;
    }

    public void setGenero(Character genero) {
        this.genero = genero;
    }

    public LocalDate getDataInscricao() {
        return this.dataInscricao;
    }

    public void setDataInscricao(LocalDate dataInscricao) {
        this.dataInscricao = dataInscricao;
    }

    public LocalTime getHoraInscricao() {
        return this.horaInscricao;
    }

    public void setHoraInscricao(LocalTime horaInscricao) {
        this.horaInscricao = horaInscricao;
    }

    public Boolean getHabilitado() {
        return this.habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getMotivoNaoHabilitacao() {
        return this.motivoNaoHabilitacao;
    }

    public void setMotivoNaoHabilitacao(String motivoNaoHabilitacao) {
        this.motivoNaoHabilitacao = motivoNaoHabilitacao;
    }

}
