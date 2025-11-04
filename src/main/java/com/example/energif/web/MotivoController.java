package com.example.energif.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.energif.model.Motivo;
import com.example.energif.repository.MotivoRepository;

@Controller
@RequestMapping("/motivos")
public class MotivoController {

    private static final Logger logger = LoggerFactory.getLogger(MotivoController.class);

    private final MotivoRepository motivoRepository;

    public MotivoController(MotivoRepository motivoRepository) {
        this.motivoRepository = motivoRepository;
    }

    @GetMapping({"/list", ""})
    public String list(Model model) {
        model.addAttribute("motivos", motivoRepository.findAll());
        return "motivos-list";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("motivo", new Motivo());
        return "motivos-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Motivo motivo) {
        logger.info("Salvando motivo: {}", motivo);
        motivoRepository.save(motivo);
        return "redirect:/motivos/list?success";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model) {
        Motivo m = motivoRepository.findById(id).orElse(new Motivo());
        model.addAttribute("motivo", m);
        return "motivos-form";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id) {
        try {
            motivoRepository.deleteById(id);
            return "redirect:/motivos/list?deleted";
        } catch (DataIntegrityViolationException ex) {
            logger.error("Erro ao excluir motivo {}: {}", id, ex.getMessage());
            return "redirect:/motivos/list?error=constraint";
        }
    }
}
