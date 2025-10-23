package com.example.energif.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.energif.model.Candidato;
import com.example.energif.model.Edital;
import com.example.energif.model.CampusEdital;
import com.example.energif.repository.CandidatoRepository;
import com.example.energif.repository.EditalRepository;
import com.example.energif.repository.CampusEditalRepository;

@Service
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final EditalRepository editalRepository;
    private final CampusEditalRepository campusEditalRepository;

    public CandidatoService(CandidatoRepository candidatoRepository, EditalRepository editalRepository, CampusEditalRepository campusEditalRepository) {
        this.candidatoRepository = candidatoRepository;
        this.editalRepository = editalRepository;
        this.campusEditalRepository = campusEditalRepository;
    }

    @Transactional
    public Candidato salvarComAtualizacao(Candidato candidato, Integer numeroVagasReservadas, Integer numeroVagasAmplaConcorrencia, Long editalId) {
        if (editalId != null) {
            Edital edital = editalRepository.findById(editalId).orElse(null);
            if (edital != null) {
                candidato.setEdital(edital);
            }
        }

        Candidato saved = candidatoRepository.save(candidato);

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

        if (editalId != null) {
            editalRepository.findById(editalId).ifPresent(edital -> {
                Integer current = edital.getNumeroInscritos();
                edital.setNumeroInscritos((current == null ? 0 : current) + 1);
                editalRepository.save(edital);
            });
        }

        return saved;
    }
}
