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

    // Novas categorias de vagas (classificados e habilitados)
    private Integer numeroVagasClassificadoMasculino = 0;
    private Integer numeroVagasClassificadoFeminino = 0;
    private Integer numeroVagasHabilitadoMasculino = 0;
    private Integer numeroVagasHabilitadoFeminino = 0;
    private Integer numeroVagasReservado = 0;

    // Contadores de ocupação para as novas categorias
    private Integer vagasClassificadoMasculinoOcupadas = 0;
    private Integer vagasClassificadoFemininoOcupadas = 0;
    private Integer vagasHabilitadoMasculinoOcupadas = 0;
    private Integer vagasHabilitadoFemininoOcupadas = 0;
    private Integer vagasReservadoOcupadas = 0;

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

    // Getters e Setters para as novas categorias de vagas
    public Integer getNumeroVagasClassificadoMasculino() {
        return numeroVagasClassificadoMasculino != null ? numeroVagasClassificadoMasculino : 0;
    }

    public void setNumeroVagasClassificadoMasculino(Integer numeroVagasClassificadoMasculino) {
        this.numeroVagasClassificadoMasculino = numeroVagasClassificadoMasculino != null ? numeroVagasClassificadoMasculino : 0;
    }

    public Integer getNumeroVagasClassificadoFeminino() {
        return numeroVagasClassificadoFeminino != null ? numeroVagasClassificadoFeminino : 0;
    }

    public void setNumeroVagasClassificadoFeminino(Integer numeroVagasClassificadoFeminino) {
        this.numeroVagasClassificadoFeminino = numeroVagasClassificadoFeminino != null ? numeroVagasClassificadoFeminino : 0;
    }

    public Integer getNumeroVagasHabilitadoMasculino() {
        return numeroVagasHabilitadoMasculino != null ? numeroVagasHabilitadoMasculino : 0;
    }

    public void setNumeroVagasHabilitadoMasculino(Integer numeroVagasHabilitadoMasculino) {
        this.numeroVagasHabilitadoMasculino = numeroVagasHabilitadoMasculino != null ? numeroVagasHabilitadoMasculino : 0;
    }

    public Integer getNumeroVagasHabilitadoFeminino() {
        return numeroVagasHabilitadoFeminino != null ? numeroVagasHabilitadoFeminino : 0;
    }

    public void setNumeroVagasHabilitadoFeminino(Integer numeroVagasHabilitadoFeminino) {
        this.numeroVagasHabilitadoFeminino = numeroVagasHabilitadoFeminino != null ? numeroVagasHabilitadoFeminino : 0;
    }

    public Integer getNumeroVagasReservado() {
        return numeroVagasReservado != null ? numeroVagasReservado : 0;
    }

    public void setNumeroVagasReservado(Integer numeroVagasReservado) {
        this.numeroVagasReservado = numeroVagasReservado != null ? numeroVagasReservado : 0;
    }

    // Getters para totais
    public Integer getNumeroVagasClassificado() {
        return getNumeroVagasClassificadoMasculino() + getNumeroVagasClassificadoFeminino();
    }

    public Integer getNumeroVagasHabilitado() {
        return getNumeroVagasHabilitadoMasculino() + getNumeroVagasHabilitadoFeminino();
    }

    // Ocupadas
    public Integer getVagasClassificadoMasculinoOcupadas() {
        return vagasClassificadoMasculinoOcupadas != null ? vagasClassificadoMasculinoOcupadas : 0;
    }

    public void setVagasClassificadoMasculinoOcupadas(Integer vagasClassificadoMasculinoOcupadas) {
        this.vagasClassificadoMasculinoOcupadas = vagasClassificadoMasculinoOcupadas != null ? vagasClassificadoMasculinoOcupadas : 0;
    }

    public Integer getVagasClassificadoFemininoOcupadas() {
        return vagasClassificadoFemininoOcupadas != null ? vagasClassificadoFemininoOcupadas : 0;
    }

    public void setVagasClassificadoFemininoOcupadas(Integer vagasClassificadoFemininoOcupadas) {
        this.vagasClassificadoFemininoOcupadas = vagasClassificadoFemininoOcupadas != null ? vagasClassificadoFemininoOcupadas : 0;
    }

    public Integer getVagasHabilitadoMasculinoOcupadas() {
        return vagasHabilitadoMasculinoOcupadas != null ? vagasHabilitadoMasculinoOcupadas : 0;
    }

    public void setVagasHabilitadoMasculinoOcupadas(Integer vagasHabilitadoMasculinoOcupadas) {
        this.vagasHabilitadoMasculinoOcupadas = vagasHabilitadoMasculinoOcupadas != null ? vagasHabilitadoMasculinoOcupadas : 0;
    }

    public Integer getVagasHabilitadoFemininoOcupadas() {
        return vagasHabilitadoFemininoOcupadas != null ? vagasHabilitadoFemininoOcupadas : 0;
    }

    public void setVagasHabilitadoFemininoOcupadas(Integer vagasHabilitadoFemininoOcupadas) {
        this.vagasHabilitadoFemininoOcupadas = vagasHabilitadoFemininoOcupadas != null ? vagasHabilitadoFemininoOcupadas : 0;
    }

    public Integer getVagasReservadoOcupadas() {
        return vagasReservadoOcupadas != null ? vagasReservadoOcupadas : 0;
    }

    public void setVagasReservadoOcupadas(Integer vagasReservadoOcupadas) {
        this.vagasReservadoOcupadas = vagasReservadoOcupadas != null ? vagasReservadoOcupadas : 0;
    }

    // Getters para disponíveis
    public int getVagasClassificadoMasculinoDisponiveis() {
        return Math.max(0, getNumeroVagasClassificadoMasculino() - getVagasClassificadoMasculinoOcupadas());
    }

    public int getVagasClassificadoFemininoDisponiveis() {
        return Math.max(0, getNumeroVagasClassificadoFeminino() - getVagasClassificadoFemininoOcupadas());
    }

    public int getVagasHabilitadoMasculinoDisponiveis() {
        return Math.max(0, getNumeroVagasHabilitadoMasculino() - getVagasHabilitadoMasculinoOcupadas());
    }

    public int getVagasHabilitadoFemininoDisponiveis() {
        return Math.max(0, getNumeroVagasHabilitadoFeminino() - getVagasHabilitadoFemininoOcupadas());
    }

    public int getVagasReservadoDisponiveis() {
        return Math.max(0, getNumeroVagasReservado() - getVagasReservadoOcupadas());
    }

    public int getVagasClassificadoOcupadas() {
        return getVagasClassificadoMasculinoOcupadas() + getVagasClassificadoFemininoOcupadas();
    }

    public int getVagasClassificadoDisponiveis() {
        return getVagasClassificadoMasculinoDisponiveis() + getVagasClassificadoFemininoDisponiveis();
    }

    public int getVagasHabilitadoOcupadas() {
        return getVagasHabilitadoMasculinoOcupadas() + getVagasHabilitadoFemininoOcupadas();
    }

    public int getVagasHabilitadoDisponiveis() {
        return getVagasHabilitadoMasculinoDisponiveis() + getVagasHabilitadoFemininoDisponiveis();
    }

    /**
     * Setter de conveniência que atualiza o total de vagas 'classificado'.
     * Internamente o modelo separa por gênero (masculino/feminino), então
     * por compatibilidade com a UI que envia um total, definimos o valor
     * para a parcela masculina e zeramos a feminina.
     */
    public void setNumeroVagasClassificado(Integer total) {
        int val = total != null ? total : 0;
        this.numeroVagasClassificadoMasculino = val;
        this.numeroVagasClassificadoFeminino = 0;
    }

    /**
     * Setter de conveniência que atualiza o total de vagas 'habilitado'.
     * O mesmo comportamento de compatibilidade é usado: aplica ao masculino
     * e zera a parcela feminina.
     */
    public void setNumeroVagasHabilitado(Integer total) {
        int val = total != null ? total : 0;
        this.numeroVagasHabilitadoMasculino = val;
        this.numeroVagasHabilitadoFeminino = 0;
    }
}
