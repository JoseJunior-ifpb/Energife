package com.example.energif.web;

import com.example.energif.model.CampusEdital;
import com.example.energif.repository.CampusEditalRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.EditalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/campus-edital")
public class CampusEditalController {

    private final CampusEditalRepository campusEditalRepository;
    private final CampusRepository campusRepository;
    private final EditalRepository editalRepository;

    public CampusEditalController(CampusEditalRepository campusEditalRepository, CampusRepository campusRepository, EditalRepository editalRepository) {
        this.campusEditalRepository = campusEditalRepository;
        this.campusRepository = campusRepository;
        this.editalRepository = editalRepository;
    }

    @GetMapping("/list")
    public String list(@RequestParam(name = "editalId", required = false) Long editalId,
                       @RequestParam(name = "campusId", required = false) Long campusId,
                       Model model) {

        List<CampusEdital> items;
        if (editalId != null && campusId != null) {
            items = campusEditalRepository.findAllByEditalIdAndCampusId(editalId, campusId);
        } else if (editalId != null) {
            items = campusEditalRepository.findAllByEditalId(editalId);
        } else if (campusId != null) {
            items = campusEditalRepository.findAllByCampusId(campusId);
        } else {
            items = campusEditalRepository.findAll(org.springframework.data.domain.Sort.by("edital.id", "campus.nome"));
        }

        model.addAttribute("items", items);
        model.addAttribute("campuses", campusRepository.findAll(Sort.by("nome")));
        model.addAttribute("editais", editalRepository.findAll(Sort.by("id")));
        model.addAttribute("selectedEdital", editalId);
        model.addAttribute("selectedCampus", campusId);
        return "campus-edital-list";
    }

    @org.springframework.web.bind.annotation.PostMapping("/migrate")
    public String migrateToCampusEdital(@RequestParam(name = "editalId") Long editalId,
                                        Model model) {
        if (editalId == null) {
            model.addAttribute("error", "É necessário informar o id do edital para migração.");
            return "redirect:/campus-edital/list";
        }

        var editalOpt = editalRepository.findById(editalId);
        if (editalOpt.isEmpty()) {
            model.addAttribute("error", "Edital não encontrado: " + editalId);
            return "redirect:/campus-edital/list";
        }
        var edital = editalOpt.get();

        var campuses = campusRepository.findAll();
        int created = 0;
        for (var campus : campuses) {
            var existing = campusEditalRepository.findByCampusAndEdital(campus, edital);
            if (existing == null) {
                var ce = new CampusEdital();
                ce.setCampus(campus);
                ce.setEdital(edital);
                ce.setNumeroVagasReservadas(campus.getNumeroVagasReservadas());
                ce.setNumeroVagasAmplaConcorrencia(campus.getNumeroVagasAmplaConcorrencia());
                campusEditalRepository.save(ce);
                created++;
            } else {
                // do not overwrite existing values to preserve manual edits
            }
        }

        model.addAttribute("message", "Migração concluída: registros criados " + created);
        return "redirect:/campus-edital/list?editalId=" + editalId;
    }
}
