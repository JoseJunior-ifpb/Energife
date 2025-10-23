package com.example.energif.web;

import com.example.energif.model.Campus;
import com.example.energif.repository.CampusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/campus")
public class CampusController {

    private static final Logger logger = LoggerFactory.getLogger(CampusController.class);

    private final CampusRepository campusRepository;

    public CampusController(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("campus", new Campus());
        // include list of existing campuses so the form can offer a selection for editing
        model.addAttribute("campuses", campusRepository.findAll(org.springframework.data.domain.Sort.by("nome")));
        return "cadastro-campus";
    }

    @PostMapping
    public String criar(@ModelAttribute Campus campus) {
        logger.info("Criando campus: {}", campus.getNome());
        // avoid duplicates: if a campus with same nome exists, update numbers instead
        Campus existing = campusRepository.findByNome(campus.getNome());
        if (existing != null) {
            existing.setNumeroVagasAmplaConcorrencia(campus.getNumeroVagasAmplaConcorrencia());
            existing.setNumeroVagasReservadas(campus.getNumeroVagasReservadas());
            campusRepository.save(existing);
            return "redirect:/campus/novo?updated";
        }
        campusRepository.save(campus);
        return "redirect:/campus/novo?success";
    }
}
