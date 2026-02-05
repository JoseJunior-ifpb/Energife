package com.example.energif.web;

import com.example.energif.model.CampusEdital;
import com.example.energif.model.CampusEditalTurno;
import com.example.energif.repository.CampusEditalRepository;
import com.example.energif.repository.CampusEditalTurnoRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.EditalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/campus-edital-turno")
public class CampusEditalTurnoController {

    private final CampusEditalTurnoRepository turnoRepository;
    private final CampusEditalRepository campusEditalRepository;
    private final CampusRepository campusRepository;
    private final EditalRepository editalRepository;

    public CampusEditalTurnoController(CampusEditalTurnoRepository turnoRepository,
                                       CampusEditalRepository campusEditalRepository,
                                       CampusRepository campusRepository,
                                       EditalRepository editalRepository) {
        this.turnoRepository = turnoRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.campusRepository = campusRepository;
        this.editalRepository = editalRepository;
    }

    @GetMapping("/list")
    public String list(@RequestParam(name = "editalId", required = false) Long editalId,
                       @RequestParam(name = "campusId", required = false) Long campusId,
                       Model model) {
        
        List<CampusEdital> campusEditais;
        if (editalId != null && campusId != null) {
            campusEditais = campusEditalRepository.findAllByEditalIdAndCampusId(editalId, campusId);
        } else if (editalId != null) {
            campusEditais = campusEditalRepository.findAllByEditalId(editalId);
        } else if (campusId != null) {
            campusEditais = campusEditalRepository.findAllByCampusId(campusId);
        } else {
            campusEditais = campusEditalRepository.findAll(Sort.by("edital.id", "campus.nome"));
        }

        // Enriquecer cada CampusEdital com seus turnos
        campusEditais.forEach(ce -> {
            List<CampusEditalTurno> turnos = turnoRepository.findByCampusEditalId(ce.getId());
            ce.setTurnos(turnos);
        });

        model.addAttribute("campusEditais", campusEditais);
        model.addAttribute("campuses", campusRepository.findAll(Sort.by("nome")));
        model.addAttribute("editais", editalRepository.findAll(Sort.by("id")));
        model.addAttribute("selectedEdital", editalId);
        model.addAttribute("selectedCampus", campusId);
        return "campus-edital-turno-list";
    }

    @PostMapping("/create")
    public String createTurno(@RequestParam Long campusEditalId,
                             @RequestParam String turno,
                             @RequestParam Integer numeroVagasReservadas,
                             @RequestParam Integer numeroVagasAmplaConcorrencia,
                             @RequestParam(required = false) Integer numeroVagasClassificadoMasculino,
                             @RequestParam(required = false) Integer numeroVagasClassificadoFeminino,
                             @RequestParam(required = false) Integer numeroVagasHabilitadoMasculino,
                             @RequestParam(required = false) Integer numeroVagasHabilitadoFeminino,
                             Model model,
                             HttpServletRequest request) {
        
        try {
            CampusEdital ce = campusEditalRepository.findById(campusEditalId)
                    .orElseThrow(() -> new IllegalArgumentException("CampusEdital não encontrado"));

            // Verificar se turno já existe
            CampusEditalTurno existing = turnoRepository.findByCampusEditalAndTurno(ce, turno);
            if (existing != null) {
                model.addAttribute("error", "Turno '" + turno + "' já existe para este campus-edital");
                return redirect(request, campusEditalId);
            }

            CampusEditalTurno cet = new CampusEditalTurno();
            cet.setCampusEdital(ce);
            cet.setTurno(turno);
            cet.setNumeroVagasReservadas(numeroVagasReservadas != null ? numeroVagasReservadas : 0);
            cet.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia != null ? numeroVagasAmplaConcorrencia : 0);
            cet.setNumeroVagasClassificadoMasculino(numeroVagasClassificadoMasculino != null ? numeroVagasClassificadoMasculino : 0);
            cet.setNumeroVagasClassificadoFeminino(numeroVagasClassificadoFeminino != null ? numeroVagasClassificadoFeminino : 0);
            cet.setNumeroVagasHabilitadoMasculino(numeroVagasHabilitadoMasculino != null ? numeroVagasHabilitadoMasculino : 0);
            cet.setNumeroVagasHabilitadoFeminino(numeroVagasHabilitadoFeminino != null ? numeroVagasHabilitadoFeminino : 0);
            cet.setVagasReservadasOcupadas(0);
            cet.setVagasAmplaOcupadas(0);
            turnoRepository.save(cet);

            model.addAttribute("message", "Turno criado com sucesso");
            return redirect(request, campusEditalId);
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao criar turno: " + e.getMessage());
            return redirect(request, campusEditalId);
        }
    }

    @PostMapping("/{turnoId}/update")
    public String updateTurno(@PathVariable Long turnoId,
                             @RequestParam Integer numeroVagasReservadas,
                             @RequestParam Integer numeroVagasAmplaConcorrencia,
                             @RequestParam(required = false) Integer numeroVagasClassificadoMasculino,
                             @RequestParam(required = false) Integer numeroVagasClassificadoFeminino,
                             @RequestParam(required = false) Integer numeroVagasHabilitadoMasculino,
                             @RequestParam(required = false) Integer numeroVagasHabilitadoFeminino,
                             Model model,
                             HttpServletRequest request) {
        
        try {
            CampusEditalTurno turno = turnoRepository.findById(turnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado"));

            turno.setNumeroVagasReservadas(numeroVagasReservadas != null ? numeroVagasReservadas : 0);
            turno.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia != null ? numeroVagasAmplaConcorrencia : 0);
            turno.setNumeroVagasClassificadoMasculino(numeroVagasClassificadoMasculino != null ? numeroVagasClassificadoMasculino : 0);
            turno.setNumeroVagasClassificadoFeminino(numeroVagasClassificadoFeminino != null ? numeroVagasClassificadoFeminino : 0);
            turno.setNumeroVagasHabilitadoMasculino(numeroVagasHabilitadoMasculino != null ? numeroVagasHabilitadoMasculino : 0);
            turno.setNumeroVagasHabilitadoFeminino(numeroVagasHabilitadoFeminino != null ? numeroVagasHabilitadoFeminino : 0);
            turnoRepository.save(turno);

            model.addAttribute("message", "Turno atualizado com sucesso");
            return redirect(request, turno.getCampusEdital().getId());
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao atualizar turno: " + e.getMessage());
            return redirect(request, null);
        }
    }

    @PostMapping("/{turnoId}/delete")
    public String deleteTurno(@PathVariable Long turnoId,
                             @RequestParam(required = false) Long ceId,
                             Model model,
                             HttpServletRequest request) {
        
        try {
            CampusEditalTurno turno = turnoRepository.findById(turnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado"));

            Long ceIdToUse = ceId != null ? ceId : turno.getCampusEdital().getId();
            turnoRepository.deleteById(turnoId);
            
            model.addAttribute("message", "Turno deletado com sucesso");
            return redirect(request, ceIdToUse);
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao deletar turno: " + e.getMessage());
            return redirect(request, ceId);
        }
    }

    private String redirect(HttpServletRequest request, Long ceId) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        if (ceId != null) {
            return "redirect:/campus-edital-turno/list";
        }
        return "redirect:/campus-edital-turno/list";
    }
}
