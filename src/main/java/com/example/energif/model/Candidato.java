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

    private Boolean habilitado = Boolean.FALSE;
    private String motivoNaoHabilitacao;

    // O candidato automaticamente concorre às vagas do seu campus

    // Método para obter as vagas disponíveis do campus
    public List<Vaga> getVagasDisponiveis() {
        return this.campus != null ? this.campus.getVagas() : new ArrayList<>();
    }

    @Enumerated(EnumType.STRING)
    private TipoVaga tipoVaga; // "RESERVADA" ou "AMPLA_CONCORRENCIA"
    
    // ... outros campos

}