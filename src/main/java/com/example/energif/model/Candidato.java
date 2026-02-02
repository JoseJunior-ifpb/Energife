package com.example.energif.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
    private Campus campus; // Campus escolhido na inscrição

    private String turno;
    private Character genero;
    private LocalDate dataInscricao;
    private LocalTime horaInscricao;

    @ManyToOne
    @JoinColumn(name = "edital_id")
    private Edital edital;

    @Enumerated(EnumType.STRING)
    private SituacaoCandidato situacao = SituacaoCandidato.PENDENTE;
    
    private String motivoNaoClassificacao;

    @Enumerated(EnumType.STRING)
    private TipoVaga tipoVaga; // "RESERVADA" ou "AMPLA_CONCORRENCIA"

    // O candidato automaticamente concorre às vagas do seu campus

    // Método para obter as vagas disponíveis do campus
    public List<Vaga> getVagasDisponiveis() {
        return this.campus != null ? this.campus.getVagas() : new ArrayList<>();
    }

    // Métodos de conveniência para manter compatibilidade com código existente
    public Boolean getClassificado() {
        return situacao == SituacaoCandidato.CLASSIFICADO;
    }

    public void setClassificado(Boolean classificado) {
        if (Boolean.TRUE.equals(classificado)) {
            this.situacao = SituacaoCandidato.CLASSIFICADO;
        } else {
            this.situacao = SituacaoCandidato.PENDENTE;
        }
    }
    
    // ... outros campos

    // Explicit getters/setters so code doesn't rely only on Lombok annotation processing
    public Long getId() {
        return this.id;
    }

    
    public String getNome() {
        return this.nome;
    }

    public LocalDate getDataNascimento() {
        return this.dataNascimento;
    }

    public String getCpf() {
        return this.cpf;
    }

    public Edital getEdital() {
        return this.edital;
    }

    public void setEdital(Edital edital) {
        this.edital = edital;
    }

    public TipoVaga getTipoVaga() {
        return this.tipoVaga;
    }

    public void setTipoVaga(TipoVaga tipoVaga) {
        this.tipoVaga = tipoVaga;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setGenero(Character genero) {
        this.genero = genero;
    }

    public Character getGenero() {
        return this.genero;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public void setDataInscricao(LocalDate dataInscricao) {
        this.dataInscricao = dataInscricao;
    }

    public void setHoraInscricao(LocalTime horaInscricao) {
        this.horaInscricao = horaInscricao;
    }

    public SituacaoCandidato getSituacao() {
        return this.situacao;
    }

    public void setSituacao(SituacaoCandidato situacao) {
        this.situacao = situacao;
    }

    public Campus getCampus() {
        return this.campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public String getTurno() {
        return this.turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public LocalDate getDataInscricao() {
        return this.dataInscricao;
    }

    public LocalTime getHoraInscricao() {
        return this.horaInscricao;
    }

    public String getMotivoNaoClassificacao() {
        return this.motivoNaoClassificacao;
    }

    public void setMotivoNaoClassificacao(String motivo) {
        this.motivoNaoClassificacao = motivo;
    }


}