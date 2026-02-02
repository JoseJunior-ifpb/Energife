package com.example.energif.service;

import com.example.energif.model.Candidato;
import com.example.energif.model.SituacaoCandidato;
import com.example.energif.model.TipoVaga;
import com.example.energif.model.Vaga;
import com.example.energif.model.Genero;
import com.example.energif.repository.CandidatoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlocacaoVagaService {

    @Autowired
    private CandidatoRepository candidatoRepository;

    /**
     * Aloca vagas para candidatos conforme sua ordem de inscrição, situação e gênero
     * Respeitando quotas de mulheres
     */
    public void processarAlocacaoVagas(Vaga vaga) {
        if (vaga == null || vaga.getQuantidade() == null || vaga.getQuantidade() <= 0) {
            return;
        }

        // Buscar candidatos ordenados por data de inscrição
        List<Candidato> candidatos = candidatoRepository.findByCampusIdAndEditalIdOrderByDataInscricaoAscHoraInscricaoAsc(
                vaga.getCampus().getId(),
                vaga.getEdital().getId()
        );

        if (candidatos.isEmpty()) {
            return;
        }

        // Contar candidatos por situação e gênero
        int totalClassificados = 0;
        int classificadosFeminino = 0;
        int totalHabilitados = 0;

        for (Candidato c : candidatos) {
            if (c.getSituacao() == SituacaoCandidato.CLASSIFICADO) {
                totalClassificados++;
                if (Genero.FEMININO.getCodigo().equals(c.getGenero())) {
                    classificadosFeminino++;
                }
            } else if (c.getSituacao() == SituacaoCandidato.HABILITADO) {
                totalHabilitados++;
            }
        }

        // Alocar vagas dinamicamente
        vaga.alocarVagas(totalClassificados, classificadosFeminino, totalHabilitados);

        // Processar candidatos em ordem de inscrição e atribuir tipo de vaga
        int contadorClassificadosMasc = 0;
        int contadorClassificadosFem = 0;
        int contadorHabilitadosMasc = 0;
        int contadorHabilitadosFem = 0;
        int contadorReservados = 0;

        for (Candidato candidato : candidatos) {
            TipoVaga tipoVagaAtribuido = null;
            
            // Classificados têm prioridade
            if (candidato.getSituacao() == SituacaoCandidato.CLASSIFICADO) {
                if (Genero.FEMININO.getCodigo().equals(candidato.getGenero())) {
                    // Mulher classificada
                    if (contadorClassificadosFem < vaga.getVagasClassificadosFeminino()) {
                        tipoVagaAtribuido = TipoVaga.CLASSIFICADO_FEMININO;
                        contadorClassificadosFem++;
                    }
                } else {
                    // Homem classificado
                    if (contadorClassificadosMasc < vaga.getVagasClassificadosMasculino()) {
                        tipoVagaAtribuido = TipoVaga.CLASSIFICADO_MASCULINO;
                        contadorClassificadosMasc++;
                    }
                }

                // Se não conseguiu vaga classificado, tenta habilitado
                if (tipoVagaAtribuido == null) {
                    if (Genero.FEMININO.getCodigo().equals(candidato.getGenero())) {
                        if (contadorHabilitadosFem < vaga.getVagasHabilitadosFeminino()) {
                            tipoVagaAtribuido = TipoVaga.HABILITADO_FEMININO;
                            contadorHabilitadosFem++;
                            candidato.setSituacao(SituacaoCandidato.HABILITADO);
                        }
                    } else {
                        if (contadorHabilitadosMasc < vaga.getVagasHabilitadosMasculino()) {
                            tipoVagaAtribuido = TipoVaga.HABILITADO_MASCULINO;
                            contadorHabilitadosMasc++;
                            candidato.setSituacao(SituacaoCandidato.HABILITADO);
                        }
                    }
                }

                // Se ainda não teve vaga, fica em reservado
                if (tipoVagaAtribuido == null) {
                    if (contadorReservados < vaga.getVagasReservadas()) {
                        tipoVagaAtribuido = TipoVaga.RESERVADO;
                        contadorReservados++;
                        candidato.setSituacao(SituacaoCandidato.HABILITADO);
                    } else {
                        candidato.setSituacao(SituacaoCandidato.PENDENTE);
                        tipoVagaAtribuido = null;
                    }
                }
            } else if (candidato.getSituacao() == SituacaoCandidato.HABILITADO) {
                // Habilitados ocupam vagas de habilitados se disponível
                if (Genero.FEMININO.getCodigo().equals(candidato.getGenero())) {
                    if (contadorHabilitadosFem < vaga.getVagasHabilitadosFeminino()) {
                        tipoVagaAtribuido = TipoVaga.HABILITADO_FEMININO;
                        contadorHabilitadosFem++;
                    }
                } else {
                    if (contadorHabilitadosMasc < vaga.getVagasHabilitadosMasculino()) {
                        tipoVagaAtribuido = TipoVaga.HABILITADO_MASCULINO;
                        contadorHabilitadosMasc++;
                    }
                }

                // Se não tem vaga de habilitado, tenta reservado
                if (tipoVagaAtribuido == null) {
                    if (contadorReservados < vaga.getVagasReservadas()) {
                        tipoVagaAtribuido = TipoVaga.RESERVADO;
                        contadorReservados++;
                    }
                }
            }

            // Atribuir tipo de vaga ao candidato
            if (tipoVagaAtribuido != null) {
                candidato.setTipoVaga(tipoVagaAtribuido);
                vaga.preencherVaga(tipoVagaAtribuido);
            }
        }

        // Salvar candidatos com as novas alocações
        candidatoRepository.saveAll(candidatos);
    }
}
