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
import com.example.energif.repository.CampusEditalRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.CandidatoRepository;
import com.example.energif.repository.EditalRepository;

@Service
public class CandidatoService {

    private static final Logger logger = LoggerFactory.getLogger(CandidatoService.class); // ADICIONE ESTA LINHA

    private final CandidatoRepository candidatoRepository;
    private final EditalRepository editalRepository;
    private final CampusEditalRepository campusEditalRepository;
    private final CampusRepository campusRepository;

    public CandidatoService(CandidatoRepository candidatoRepository, 
                          EditalRepository editalRepository, 
                          CampusEditalRepository campusEditalRepository,
                          CampusRepository campusRepository) {
        this.candidatoRepository = candidatoRepository;
        this.editalRepository = editalRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.campusRepository = campusRepository;
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
    
    // VERIFICAR SE JÁ ESTÁ HABILITADO
    if (Boolean.TRUE.equals(candidato.getHabilitado())) {
        logger.info("Candidato {} já está habilitado", candidatoId);
        return;
    }
    
    // Verificar disponibilidade de vagas
    boolean vagaDisponivel = false;
    if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
        vagaDisponivel = campus.temVagaReservadaDisponivel();
    } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
        vagaDisponivel = campus.temVagaAmplaDisponivel();
    }
    
    if (!vagaDisponivel) {
        throw new IllegalStateException("Não há vagas disponíveis para o tipo selecionado no campus " + campus.getNome());
    }
    
    // Atualizar contador de vagas ocupadas
    if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
        campus.setVagasReservadasOcupadas(campus.getVagasReservadasOcupadas() + 1);
    } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
        campus.setVagasAmplaOcupadas(campus.getVagasAmplaOcupadas() + 1);
    }
    
    // Habilitar o candidato
    candidato.setHabilitado(true);
    candidato.setMotivoNaoHabilitacao(null);
    
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
        
        // VERIFICAR SE JÁ ESTÁ HABILITADO
        if (Boolean.TRUE.equals(candidato.getHabilitado())) {
            logger.info("Candidato {} já está habilitado", candidatoId);
            resultado.put("sucesso", true);
            resultado.put("mensagem", "Candidato já estava habilitado");
            resultado.put("jaHabilitado", true);
            return resultado;
        }
        
        // Verificar disponibilidade de vagas
        boolean vagaDisponivel = false;
        int vagasDisponiveis = 0;
        int vagasOcupadas = 0;
        int vagasTotais = 0;
        
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            vagaDisponivel = campus.temVagaReservadaDisponivel();
            vagasDisponiveis = campus.getVagasReservadasDisponiveis();
            vagasOcupadas = campus.getVagasReservadasOcupadas();
            vagasTotais = campus.getNumeroVagasReservadas();
            logger.info("Vagas Reservadas - Ocupadas: {}/{}, Disponíveis: {}", 
                       vagasOcupadas, vagasTotais, vagasDisponiveis);
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            vagaDisponivel = campus.temVagaAmplaDisponivel();
            vagasDisponiveis = campus.getVagasAmplaDisponiveis();
            vagasOcupadas = campus.getVagasAmplaOcupadas();
            vagasTotais = campus.getNumeroVagasAmplaConcorrencia();
            logger.info("Vagas Ampla - Ocupadas: {}/{}, Disponíveis: {}", 
                       vagasOcupadas, vagasTotais, vagasDisponiveis);
        }
        
        if (!vagaDisponivel) {
            logger.error("NÃO há vagas disponíveis para o candidato {} no campus {}", candidatoId, campus.getNome());
            resultado.put("sucesso", false);
            String tipo = (candidato.getTipoVaga() == TipoVaga.RESERVADA) ? "reservadas" : "de ampla concorrência";
            String mensagem = "Não há vagas " + tipo + " disponíveis no campus " + campus.getNome() + ".";
            // If total is zero, hint that admin must configure vacancies
            if (vagasTotais == 0) {
                mensagem += " Por favor, configure o número de vagas disponíveis no cadastro do campus antes de habilitar candidatos.";
            }
            resultado.put("mensagem", mensagem);
            resultado.put("vagasDisponiveis", vagasDisponiveis);
            resultado.put("vagasOcupadas", vagasOcupadas);
            resultado.put("vagasTotais", vagasTotais);
            resultado.put("tipoVaga", candidato.getTipoVaga().toString());
            resultado.put("campusNome", campus.getNome());
            return resultado;
        }
        
        // ATUALIZAR CONTADOR DE VAGAS OCUPADAS
        if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
            int novasOcupadas = campus.getVagasReservadasOcupadas() + 1;
            campus.setVagasReservadasOcupadas(novasOcupadas);
            logger.info("Vagas Reservadas: {} → {}", vagasOcupadas, novasOcupadas);
        } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
            int novasOcupadas = campus.getVagasAmplaOcupadas() + 1;
            campus.setVagasAmplaOcupadas(novasOcupadas);
            logger.info("Vagas Ampla: {} → {}", vagasOcupadas, novasOcupadas);
        }
        
        // SALVAR PRIMEIRO O CAMPUS (para garantir as vagas ocupadas)
        Campus campusSalvo = campusRepository.save(campus);
        logger.info("Campus salvo - Vagas Reservadas Ocupadas: {}, Vagas Ampla Ocupadas: {}", 
                   campusSalvo.getVagasReservadasOcupadas(), 
                   campusSalvo.getVagasAmplaOcupadas());
        
        // DEPOIS SALVAR O CANDIDATO
        candidato.setHabilitado(true);
        candidato.setMotivoNaoHabilitacao(null);
        
        Candidato candidatoSalvo = candidatoRepository.save(candidato);
        logger.info("Candidato salvo - Habilitado: {}", candidatoSalvo.getHabilitado());
        
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
    
    // Se estava habilitado, liberar a vaga
    if (Boolean.TRUE.equals(candidato.getHabilitado()) && campus != null) {
        logger.info("Liberando vaga do candidato habilitado");
        
        int vagasAntes = 0;
        int vagasDepois = 0;
        
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
        
        // SALVAR PRIMEIRO O CAMPUS
        Campus campusSalvo = campusRepository.save(campus);
        logger.info("Campus salvo - Vagas Reservadas Ocupadas: {}, Vagas Ampla Ocupadas: {}", 
                   campusSalvo.getVagasReservadasOcupadas(), 
                   campusSalvo.getVagasAmplaOcupadas());
    }
    
    // Desabilitar o candidato
    candidato.setHabilitado(false);
    candidato.setMotivoNaoHabilitacao(motivo);
    
    Candidato candidatoSalvo = candidatoRepository.save(candidato);
    logger.info("Candidato salvo - Habilitado: {}", candidatoSalvo.getHabilitado());
    
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

    // Método para validar se pode habilitar candidato
    public boolean podeHabilitarCandidato(Long candidatoId) {
        try {
            Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(() -> new IllegalArgumentException("Candidato não encontrado"));
            
            Campus campus = candidato.getCampus();
            if (campus == null) return false;
            
            if (candidato.getTipoVaga() == TipoVaga.RESERVADA) {
                return campus.temVagaReservadaDisponivel();
            } else if (candidato.getTipoVaga() == TipoVaga.AMPLA_CONCORRENCIA) {
                return campus.temVagaAmplaDisponivel();
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Erro ao verificar se pode habilitar candidato {}: {}", candidatoId, e.getMessage());
            return false;
        }
    }
}