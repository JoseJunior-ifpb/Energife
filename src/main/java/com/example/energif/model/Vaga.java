package com.example.energif.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @ManyToOne
    @JoinColumn(name = "edital_id")
    private Edital edital;

    // Total de vagas a oferecer
    private Integer quantidade;

    // Quota mínima de mulheres entre as vagas classificadas
    private Integer quotaMulheresClassificadas = 0;

    // Vagas por tipo (alocadas dinamicamente)
    private Integer vagasClassificadosMasculino = 0;
    private Integer vagasClassificadosFeminino = 0;
    private Integer vagasHabilitadosMasculino = 0;
    private Integer vagasHabilitadosFeminino = 0;
    private Integer vagasReservadas = 0;

    // Contadores de vagas ocupadas (preenchidas por candidatos)
    private Integer ocupadasClassificadosMasculino = 0;
    private Integer ocupadasClassificadosFeminino = 0;
    private Integer ocupadasHabilitadosMasculino = 0;
    private Integer ocupadasHabilitadosFeminino = 0;
    private Integer ocupadasReservadas = 0;

    /**
     * Calcula e aloca as vagas dinamicamente conforme os candidatos são processados
     */
    public synchronized void alocarVagas(int totalClassificados, int classificadosFeminino, int totalHabilitados) {
        if (quantidade == null || quantidade <= 0) {
            return;
        }

        int classificadosMasculino = totalClassificados - classificadosFeminino;
        int quota = quotaMulheresClassificadas != null ? quotaMulheresClassificadas : 0;

        // Garantir quota mínima de mulheres
        int vagasParaMulheres = Math.max(quota, (int) Math.ceil(quantidade * 0.2)); // Mínimo 20% mulheres
        vagasParaMulheres = Math.min(vagasParaMulheres, totalClassificados); // Não pode exceder total de classificados

        // Distribuir vagas para classificados
        if (vagasParaMulheres <= classificadosFeminino) {
            // Há mulheres suficientes para atingir quota
            vagasClassificadosFeminino = vagasParaMulheres;
            vagasClassificadosMasculino = Math.min(classificadosMasculino, quantidade - vagasClassificadosFeminino);
        } else {
            // Não há mulheres suficientes, aloca todas as mulheres classificadas
            vagasClassificadosFeminino = classificadosFeminino;
            vagasClassificadosMasculino = Math.min(classificadosMasculino, quantidade - vagasClassificadosFeminino);
        }

        // Vagas restantes para habilitados
        int vagasDisponiveisParaHabilitados = quantidade - vagasClassificadosMasculino - vagasClassificadosFeminino;
        
        int habilitadosFeminino = totalHabilitados / 2; // Aproximadamente
        int habilitadosMasculino = totalHabilitados - habilitadosFeminino;

        vagasHabilitadosMasculino = Math.min(habilitadosMasculino, vagasDisponiveisParaHabilitados / 2);
        vagasHabilitadosFeminino = Math.min(habilitadosFeminino, vagasDisponiveisParaHabilitados - vagasHabilitadosMasculino);

        // Resto é para reservados
        vagasReservadas = quantidade - vagasClassificadosMasculino - vagasClassificadosFeminino - vagasHabilitadosMasculino - vagasHabilitadosFeminino;
    }

    public boolean temVagaDisponivel(TipoVaga tipo) {
        if (tipo == TipoVaga.CLASSIFICADO_MASCULINO) {
            return ocupadasClassificadosMasculino < vagasClassificadosMasculino;
        } else if (tipo == TipoVaga.CLASSIFICADO_FEMININO) {
            return ocupadasClassificadosFeminino < vagasClassificadosFeminino;
        } else if (tipo == TipoVaga.HABILITADO_MASCULINO) {
            return ocupadasHabilitadosMasculino < vagasHabilitadosMasculino;
        } else if (tipo == TipoVaga.HABILITADO_FEMININO) {
            return ocupadasHabilitadosFeminino < vagasHabilitadosFeminino;
        } else if (tipo == TipoVaga.RESERVADO) {
            return ocupadasReservadas < vagasReservadas;
        }
        return false;
    }

    public synchronized void preencherVaga(TipoVaga tipo) {
        if (tipo == TipoVaga.CLASSIFICADO_MASCULINO) {
            ocupadasClassificadosMasculino++;
        } else if (tipo == TipoVaga.CLASSIFICADO_FEMININO) {
            ocupadasClassificadosFeminino++;
        } else if (tipo == TipoVaga.HABILITADO_MASCULINO) {
            ocupadasHabilitadosMasculino++;
        } else if (tipo == TipoVaga.HABILITADO_FEMININO) {
            ocupadasHabilitadosFeminino++;
        } else if (tipo == TipoVaga.RESERVADO) {
            ocupadasReservadas++;
        }
    }

    public int getTotalOcupadas() {
        return ocupadasClassificadosMasculino + ocupadasClassificadosFeminino + 
               ocupadasHabilitadosMasculino + ocupadasHabilitadosFeminino + 
               ocupadasReservadas;
    }

    public int getTotalVagasAlocadas() {
        return vagasClassificadosMasculino + vagasClassificadosFeminino + 
               vagasHabilitadosMasculino + vagasHabilitadosFeminino + 
               vagasReservadas;
    }

    // Explicit getters to avoid relying solely on Lombok annotation processing
    public Integer getQuantidade() {
        return this.quantidade;
    }

    public Campus getCampus() {
        return this.campus;
    }

    public Edital getEdital() {
        return this.edital;
    }

    public Integer getVagasClassificadosMasculino() {
        return this.vagasClassificadosMasculino;
    }

    public Integer getVagasClassificadosFeminino() {
        return this.vagasClassificadosFeminino;
    }

    public Integer getVagasHabilitadosMasculino() {
        return this.vagasHabilitadosMasculino;
    }

    public Integer getVagasHabilitadosFeminino() {
        return this.vagasHabilitadosFeminino;
    }

    public Integer getVagasReservadas() {
        return this.vagasReservadas;
    }
}