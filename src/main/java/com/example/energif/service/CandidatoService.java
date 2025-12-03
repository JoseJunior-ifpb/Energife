package com.example.energif.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.energif.model.Campus;
import com.example.energif.model.CampusEdital;
import com.example.energif.model.Candidato;
import com.example.energif.model.Edital;
import com.example.energif.model.TipoVaga;
import com.example.energif.model.SituacaoCandidato;
import com.example.energif.model.CampusEditalTurno;
import com.example.energif.repository.CampusEditalRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.CandidatoRepository;
import com.example.energif.repository.EditalRepository;
import com.example.energif.repository.CampusEditalTurnoRepository;

@Service
public class CandidatoService {

    private static final Logger logger = LoggerFactory.getLogger(CandidatoService.class); // ADICIONE ESTA LINHA

    private final CandidatoRepository candidatoRepository;
    private final EditalRepository editalRepository;
    private final CampusEditalRepository campusEditalRepository;
    private final CampusRepository campusRepository;
    private final CampusEditalTurnoRepository campusEditalTurnoRepository;

    public CandidatoService(CandidatoRepository candidatoRepository, 
                          EditalRepository editalRepository, 
                          CampusEditalRepository campusEditalRepository,
                          CampusRepository campusRepository,
                          CampusEditalTurnoRepository campusEditalTurnoRepository) {
        this.candidatoRepository = candidatoRepository;
        this.editalRepository = editalRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.campusRepository = campusRepository;
        this.campusEditalTurnoRepository = campusEditalTurnoRepository;
    }

    @Transactional
    public Candidato salvarComAtualizacao(Candidato candidato, Integer numeroVagasReservadas, Integer numeroVagasAmplaConcorrencia, Long editalId) {
        // 1. Determinar tipo de vaga automaticamente pelo gênero
        determinarTipoVaga(candidato);
        
        // 2. Processar edital se fornecido
        if (editalId != null) {
            Edital edital = editalRepository.findById(editalId).orElse(null);
            if (edital != null) {
                candidato.setEdital(edital);
            }
        }

        Candidato saved = candidatoRepository.save(candidato);

        // 3. Atualizar CampusEdital
        if (saved.getEdital() != null && saved.getCampus() != null) {
            CampusEdital ce = campusEditalRepository.findByCampusAndEdital(saved.getCampus(), saved.getEdital());
            if (ce == null) {
                ce = new CampusEdital();
                ce.setCampus(saved.getCampus());
                ce.setEdital(saved.getEdital());
            }
            if (numeroVagasReservadas != null) ce.setNumeroVagasReservadas(numeroVagasReservadas);
            if (numeroVagasAmplaConcorrencia != null) ce.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia);
            campusEditalRepository.save(ce);
            // garantir um registro de turno 'UNICO' para compatibilidade com a nova modelagem
            try {
                CampusEditalTurno existing = campusEditalTurnoRepository.findByCampusEditalAndTurno(ce, "UNICO");
                if (existing == null) {
                    CampusEditalTurno cet = new CampusEditalTurno();
                    cet.setCampusEdital(ce);
                    cet.setTurno("UNICO");
                    cet.setNumeroVagasReservadas(ce.getNumeroVagasReservadas());
                    cet.setNumeroVagasAmplaConcorrencia(ce.getNumeroVagasAmplaConcorrencia());
                    campusEditalTurnoRepository.save(cet);
                }
            } catch (Exception ex) {
                logger.warn("Não foi possível criar registro CampusEditalTurno 'UNICO' automaticamente: {}", ex.getMessage());
            }
        }

        // 4. Atualizar contador de inscritos no edital
        if (editalId != null) {
            editalRepository.findById(editalId).ifPresent(edital -> {
                Integer current = edital.getNumeroInscritos();
                edital.setNumeroInscritos((current == null ? 0 : current) + 1);
                editalRepository.save(edital);
            });
        }

        return saved;
    }
    // No CandidatoService - mantenha o método original se você o substituiu:
@Transactional
public void habilitarCandidato(Long candidatoId, String motivo) {
    logger.info("Iniciando habilitação do candidato ID: {}", candidatoId);
    
    Candidato candidato = candidatoRepository.findById(candidatoId)
        .orElseThrow(() -> new IllegalArgumentException("Candidato não encontrado"));
    
    Campus campus = candidato.getCampus();
    if (campus == null) {
        throw new IllegalStateException("Candidato não possui campus definido");
    }
    
    // VERIFICAR SE JÁ ESTÁ CLASSIFICADO
    if (candidato.getSituacao() == SituacaoCandidato.CLASSIFICADO) {
        logger.info("Candidato {} já está classificado", candidatoId);
        return;
    }
    
    // Verificar disponibilidade de vagas - primeiro tentar por turno (campus+edital+turno)
    CampusEditalTurno turnoRecord = null;
    boolean vagaDisponivel = false;
    if (candidato.getCampus() != null && candidato.getEdital() != null && candidato.getTurno() != null) {
        turnoRecord = campusEditalTurnoRepository.findByCampusAndEditalAndTurno(candidato.getCampus(), candidato.getEdital(), candidato.getTurno());
    }

    if (turnoRecord != null) {
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            vagaDisponivel = turnoRecord.getVagasReservadasDisponiveis() > 0;
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            vagaDisponivel = turnoRecord.getVagasAmplaDisponiveis() > 0;
        } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
            vagaDisponivel = turnoRecord.getVagasCadastroReservaDisponiveis() > 0;
        }
    } else {
        // fallback para comportamento legado baseado no campus
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            vagaDisponivel = campus.temVagaReservadaDisponivel();
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            vagaDisponivel = campus.temVagaAmplaDisponivel();
        } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
            Integer vagas = campus.getNumeroVagasCadastroReserva();
            vagaDisponivel = vagas != null && vagas > 0;
        }
    }

    if (!vagaDisponivel) {
        throw new IllegalStateException("Não há vagas disponíveis para o tipo selecionado no campus " + campus.getNome());
    }

    // Atualizar contador de vagas ocupadas (priorizar registro por turno)
    if (turnoRecord != null) {
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            turnoRecord.setVagasReservadasOcupadas(turnoRecord.getVagasReservadasOcupadas() + 1);
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            turnoRecord.setVagasAmplaOcupadas(turnoRecord.getVagasAmplaOcupadas() + 1);
        } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
            turnoRecord.setVagasCadastroReservaOcupadas(turnoRecord.getVagasCadastroReservaOcupadas() + 1);
        }
        campusEditalTurnoRepository.save(turnoRecord);
    } else {
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            campus.setVagasReservadasOcupadas(campus.getVagasReservadasOcupadas() + 1);
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            campus.setVagasAmplaOcupadas(campus.getVagasAmplaOcupadas() + 1);
        }
        // CADASTRO_RESERVA não é rastreado no nível de Campus (apenas por turno); nenhuma alteração local necessária
    }
    
    // Habilitar o candidato
    candidato.setSituacao(SituacaoCandidato.CLASSIFICADO);
    candidato.setMotivoNaoClassificacao(null);
    
    campusRepository.save(campus);
    candidatoRepository.save(candidato);
    
    logger.info("Candidato {} habilitado com sucesso.", candidatoId);
}

  
@Transactional
public Map<String, Object> habilitarCandidatoComFeedback(Long candidatoId, String motivo) {
    Map<String, Object> resultado = new HashMap<>();
    
    try {
        logger.info("=== INICIANDO HABILITAÇÃO DO CANDIDATO {} ===", candidatoId);
        
        Candidato candidato = candidatoRepository.findById(candidatoId)
            .orElseThrow(() -> {
                logger.error("Candidato {} não encontrado", candidatoId);
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Candidato não encontrado");
                return new IllegalArgumentException("Candidato não encontrado");
            });
        
        Campus campus = candidato.getCampus();
        if (campus == null) {
            logger.error("Candidato {} não possui campus definido", candidatoId);
            resultado.put("sucesso", false);
            resultado.put("mensagem", "Candidato não possui campus definido");
            return resultado;
        }
        
        logger.info("Candidato: {}, Campus: {}, Tipo Vaga: {}", 
                   candidato.getNome(), campus.getNome(), candidato.getTipoVaga());
        
        // VERIFICAR SE JÁ ESTÁ CLASSIFICADO
        if (candidato.getSituacao() == SituacaoCandidato.CLASSIFICADO) {
            logger.info("Candidato {} já está classificado", candidatoId);
            resultado.put("sucesso", true);
            resultado.put("mensagem", "Candidato já estava classificado");
            resultado.put("jaClassificado", true);
            return resultado;
        }
        
        // Verificar disponibilidade de vagas (priorizar registro por turno)
        CampusEditalTurno turnoRecord = null;
        if (candidato.getCampus() != null && candidato.getEdital() != null && candidato.getTurno() != null) {
            turnoRecord = campusEditalTurnoRepository.findByCampusAndEditalAndTurno(candidato.getCampus(), candidato.getEdital(), candidato.getTurno());
        }

        boolean vagaDisponivel = false;
        int vagasDisponiveis = 0;
        int vagasOcupadas = 0;
        int vagasTotais = 0;

        if (turnoRecord != null) {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                vagaDisponivel = turnoRecord.getVagasReservadasDisponiveis() > 0;
                vagasDisponiveis = turnoRecord.getVagasReservadasDisponiveis();
                vagasOcupadas = turnoRecord.getVagasReservadasOcupadas();
                vagasTotais = turnoRecord.getNumeroVagasReservadas();
                logger.info("[Turno] Vagas Reservadas - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                vagaDisponivel = turnoRecord.getVagasAmplaDisponiveis() > 0;
                vagasDisponiveis = turnoRecord.getVagasAmplaDisponiveis();
                vagasOcupadas = turnoRecord.getVagasAmplaOcupadas();
                vagasTotais = turnoRecord.getNumeroVagasAmplaConcorrencia();
                logger.info("[Turno] Vagas Ampla - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                vagaDisponivel = turnoRecord.getVagasCadastroReservaDisponiveis() > 0;
                vagasDisponiveis = turnoRecord.getVagasCadastroReservaDisponiveis();
                vagasOcupadas = turnoRecord.getVagasCadastroReservaOcupadas();
                vagasTotais = turnoRecord.getNumeroVagasCadastroReserva();
                logger.info("[Turno] Vagas Cadastro Reserva - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            }
        } else {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                vagaDisponivel = campus.temVagaReservadaDisponivel();
                vagasDisponiveis = campus.getVagasReservadasDisponiveis();
                vagasOcupadas = campus.getVagasReservadasOcupadas();
                vagasTotais = campus.getNumeroVagasReservadas();
                logger.info("Vagas Reservadas - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                vagaDisponivel = campus.temVagaAmplaDisponivel();
                vagasDisponiveis = campus.getVagasAmplaDisponiveis();
                vagasOcupadas = campus.getVagasAmplaOcupadas();
                vagasTotais = campus.getNumeroVagasAmplaConcorrencia();
                logger.info("Vagas Ampla - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                vagasDisponiveis = campus.getNumeroVagasCadastroReserva() != null ? campus.getNumeroVagasCadastroReserva() : 0;
                vagasOcupadas = 0; // Campus não tem rastreamento de ocupação para cadastro reserva (apenas turno)
                vagasTotais = vagasDisponiveis;
                vagaDisponivel = vagasDisponiveis > 0;
                logger.info("Vagas Cadastro Reserva - Ocupadas: {}/{}, Disponíveis: {}", vagasOcupadas, vagasTotais, vagasDisponiveis);
            }
        }

        if (!vagaDisponivel) {
            logger.error("NÃO há vagas disponíveis para o candidato {} no campus {}", candidatoId, campus.getNome());
            resultado.put("sucesso", false);
            String tipo = (candidato.getTipoVaga() == TipoVaga.RESERVADA) ? "reservadas" : "de ampla concorrência";
            String mensagem = "Não há vagas " + tipo + " disponíveis no campus " + campus.getNome() + ".";
            if (vagasTotais == 0) {
                mensagem += " Por favor, configure o número de vagas disponíveis no cadastro do campus/turno antes de habilitar candidatos.";
            }
            resultado.put("mensagem", mensagem);
            resultado.put("vagasDisponiveis", vagasDisponiveis);
            resultado.put("vagasOcupadas", vagasOcupadas);
            resultado.put("vagasTotais", vagasTotais);
            resultado.put("tipoVaga", candidato.getTipoVaga().toString());
            resultado.put("campusNome", campus.getNome());
            return resultado;
        }

        // ATUALIZAR CONTADOR DE VAGAS OCUPADAS (priorizar turnoRecord)
        if (turnoRecord != null) {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                int novasOcupadas = turnoRecord.getVagasReservadasOcupadas() + 1;
                turnoRecord.setVagasReservadasOcupadas(novasOcupadas);
                logger.info("[Turno] Vagas Reservadas: {} → {}", vagasOcupadas, novasOcupadas);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                int novasOcupadas = turnoRecord.getVagasAmplaOcupadas() + 1;
                turnoRecord.setVagasAmplaOcupadas(novasOcupadas);
                logger.info("[Turno] Vagas Ampla: {} → {}", vagasOcupadas, novasOcupadas);
            } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                int novasOcupadas = turnoRecord.getVagasCadastroReservaOcupadas() + 1;
                turnoRecord.setVagasCadastroReservaOcupadas(novasOcupadas);
                logger.info("[Turno] Vagas Cadastro Reserva: {} → {}", vagasOcupadas, novasOcupadas);
            }
            campusEditalTurnoRepository.save(turnoRecord);
        } else {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                int novasOcupadas = campus.getVagasReservadasOcupadas() + 1;
                campus.setVagasReservadasOcupadas(novasOcupadas);
                logger.info("Vagas Reservadas: {} → {}", vagasOcupadas, novasOcupadas);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                int novasOcupadas = campus.getVagasAmplaOcupadas() + 1;
                campus.setVagasAmplaOcupadas(novasOcupadas);
                logger.info("Vagas Ampla: {} → {}", vagasOcupadas, novasOcupadas);
            }
            // CADASTRO_RESERVA não é rastreado no nível de Campus
            Campus campusSalvo = campusRepository.save(campus);
            logger.info("Campus salvo - Vagas Reservadas Ocupadas: {}, Vagas Ampla Ocupadas: {}", 
                       campusSalvo.getVagasReservadasOcupadas(), 
                       campusSalvo.getVagasAmplaOcupadas());
        }
        
        // DEPOIS SALVAR O CANDIDATO
        candidato.setSituacao(SituacaoCandidato.CLASSIFICADO);
        candidato.setMotivoNaoClassificacao(null);
        
        Candidato candidatoSalvo = candidatoRepository.save(candidato);
        logger.info("Candidato salvo - Situação: {}", candidatoSalvo.getSituacao());
        
        resultado.put("sucesso", true);
        resultado.put("mensagem", "Candidato habilitado com sucesso!");
        resultado.put("vagasDisponiveis", vagasDisponiveis - 1); // Uma vaga foi ocupada
        resultado.put("vagasOcupadas", vagasOcupadas + 1);
        resultado.put("vagasTotais", vagasTotais);
        resultado.put("tipoVaga", candidato.getTipoVaga().toString());
        resultado.put("campusNome", campus.getNome());
        
        logger.info("=== HABILITAÇÃO CONCLUÍDA COM SUCESSO ===");
        
    } catch (Exception e) {
        logger.error("Erro ao habilitar candidato {}: {}", candidatoId, e.getMessage(), e);
        resultado.put("sucesso", false);
        resultado.put("mensagem", "Erro interno: " + e.getMessage());
    }
    
    return resultado;
}
    
    @Transactional
public void desabilitarCandidato(Long candidatoId, String motivo) {
    logger.info("=== INICIANDO DESABILITAÇÃO DO CANDIDATO {} ===", candidatoId);
    
    Candidato candidato = candidatoRepository.findById(candidatoId)
        .orElseThrow(() -> new IllegalArgumentException("Candidato não encontrado"));
    
    Campus campus = candidato.getCampus();
    
    // Se estava classificado, liberar a vaga (priorizar turnoRecord)
    if (candidato.getSituacao() == SituacaoCandidato.CLASSIFICADO && campus != null) {
        logger.info("Liberando vaga do candidato habilitado");
        
        CampusEditalTurno turnoRecord = null;
        if (candidato.getCampus() != null && candidato.getEdital() != null && candidato.getTurno() != null) {
            turnoRecord = campusEditalTurnoRepository.findByCampusAndEditalAndTurno(candidato.getCampus(), candidato.getEdital(), candidato.getTurno());
        }

        int vagasAntes = 0;
        int vagasDepois = 0;
        
        if (turnoRecord != null) {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                vagasAntes = turnoRecord.getVagasReservadasOcupadas();
                vagasDepois = Math.max(0, vagasAntes - 1);
                turnoRecord.setVagasReservadasOcupadas(vagasDepois);
                logger.info("[Turno] Vagas Reservadas: {} → {}", vagasAntes, vagasDepois);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                vagasAntes = turnoRecord.getVagasAmplaOcupadas();
                vagasDepois = Math.max(0, vagasAntes - 1);
                turnoRecord.setVagasAmplaOcupadas(vagasDepois);
                logger.info("[Turno] Vagas Ampla: {} → {}", vagasAntes, vagasDepois);
            } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                vagasAntes = turnoRecord.getVagasCadastroReservaOcupadas();
                vagasDepois = Math.max(0, vagasAntes - 1);
                turnoRecord.setVagasCadastroReservaOcupadas(vagasDepois);
                logger.info("[Turno] Vagas Cadastro Reserva: {} → {}", vagasAntes, vagasDepois);
            }
            campusEditalTurnoRepository.save(turnoRecord);
            logger.info("Turno salvo - vagas atualizadas para turno {}", turnoRecord.getTurno());
        } else {
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                vagasAntes = campus.getVagasReservadasOcupadas();
                vagasDepois = Math.max(0, vagasAntes - 1);
                campus.setVagasReservadasOcupadas(vagasDepois);
                logger.info("Vagas Reservadas: {} → {}", vagasAntes, vagasDepois);
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                vagasAntes = campus.getVagasAmplaOcupadas();
                vagasDepois = Math.max(0, vagasAntes - 1);
                campus.setVagasAmplaOcupadas(vagasDepois);
                logger.info("Vagas Ampla: {} → {}", vagasAntes, vagasDepois);
            }
            // CADASTRO_RESERVA não é rastreado no nível de Campus
            Campus campusSalvo = campusRepository.save(campus);
            logger.info("Campus salvo - Vagas Reservadas Ocupadas: {}, Vagas Ampla Ocupadas: {}", 
                       campusSalvo.getVagasReservadasOcupadas(), 
                       campusSalvo.getVagasAmplaOcupadas());
        }
    }
    
    // Desabilitar o candidato
    candidato.setSituacao(SituacaoCandidato.NAO_CLASSIFICADO);
    candidato.setMotivoNaoClassificacao(motivo);
    
    Candidato candidatoSalvo = candidatoRepository.save(candidato);
    logger.info("Candidato salvo - Situação: {}", candidatoSalvo.getSituacao());
    
    logger.info("=== DESABILITAÇÃO CONCLUÍDA ===");
}

    // Método sobrecarregado para salvar sem parâmetros extras
    @Transactional
    public Candidato salvarComAtualizacao(Candidato candidato) {
        determinarTipoVaga(candidato);
        return candidatoRepository.save(candidato);
    }

    // Método auxiliar para determinar tipo de vaga automaticamente
    private void determinarTipoVaga(Candidato candidato) {
        if (candidato.getGenero() != null) {
            if (candidato.getGenero() == 'F') {
                candidato.setTipoVaga(TipoVaga.RESERVADA);
            } else {
                candidato.setTipoVaga(TipoVaga.AMPLA_CONCORRENCIA);
            }
        } else {
            // Se gênero não informado, define como ampla concorrência (padrão)
            candidato.setTipoVaga(TipoVaga.AMPLA_CONCORRENCIA);
        }
    }

    // Método para validar se pode classificar candidato
    public boolean podeClassificarCandidato(Long candidatoId) {
        try {
            Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(() -> new IllegalArgumentException("Candidato não encontrado"));
            
            Campus campus = candidato.getCampus();
            if (campus == null) return false;
            // Priorizar verificação por turno (campus+edital+turno)
            CampusEditalTurno turnoRecord = null;
            if (candidato.getCampus() != null && candidato.getEdital() != null && candidato.getTurno() != null) {
                turnoRecord = campusEditalTurnoRepository.findByCampusAndEditalAndTurno(candidato.getCampus(), candidato.getEdital(), candidato.getTurno());
            }

            if (turnoRecord != null) {
                if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                    return turnoRecord.getVagasReservadasDisponiveis() > 0;
                } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                    return turnoRecord.getVagasAmplaDisponiveis() > 0;
                } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                    return turnoRecord.getVagasCadastroReservaDisponiveis() > 0;
                }
            } else {
                if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                    return campus.temVagaReservadaDisponivel();
                } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                    return campus.temVagaAmplaDisponivel();
                } else if (candidato.getTipoVaga() == TipoVaga.CADASTRO_RESERVA) {
                    Integer vagas = campus.getNumeroVagasCadastroReserva();
                    return vagas != null && vagas > 0;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Erro ao verificar se pode habilitar candidato {}: {}", candidatoId, e.getMessage());
            return false;
        }
    }
}