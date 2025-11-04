package com.example.energif.web;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.energif.model.Campus;
import com.example.energif.repository.CampusRepository;

@Controller
@RequestMapping("/campus")
public class CampusController {

    private static final Logger logger = LoggerFactory.getLogger(CampusController.class);

    private final CampusRepository campusRepository;
    private final com.example.energif.repository.CampusEditalRepository campusEditalRepository;

    public CampusController(CampusRepository campusRepository, com.example.energif.repository.CampusEditalRepository campusEditalRepository) {
        this.campusRepository = campusRepository;
        this.campusEditalRepository = campusEditalRepository;
    }

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("campus", new Campus());
        model.addAttribute("campuses", campusRepository.findAll(Sort.by("nome")));
        return "cadastro-campus";
    }

    // No CampusController - método listarCampus:
@GetMapping("/list")
public String listarCampus(Model model) {
    var campuses = campusRepository.findAll(Sort.by("nome"));
    
    // Inicializar valores nulos
    for (Campus campus : campuses) {
        if (campus.getNumeroVagasReservadas() == null) {
            campus.setNumeroVagasReservadas(0);
        }
        if (campus.getNumeroVagasAmplaConcorrencia() == null) {
            campus.setNumeroVagasAmplaConcorrencia(0);
        }
        if (campus.getVagasReservadasOcupadas() == null) {
            campus.setVagasReservadasOcupadas(0);
        }
        if (campus.getVagasAmplaOcupadas() == null) {
            campus.setVagasAmplaOcupadas(0);
        }
    }
    
    model.addAttribute("campuses", campuses);
    return "lista-campus";
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

    // CORREÇÃO: Usar @PathVariable em vez de @RequestParam
    // No CampusController - adicione estes métodos:
@PostMapping("/{id}/editar-ajax")
@ResponseBody
public ResponseEntity<?> editarCampusAjax(@PathVariable("id") Long id,
                                         @RequestParam Integer numeroVagasReservadas,
                                         @RequestParam Integer numeroVagasAmplaConcorrencia) {
    try {
        Campus campus = campusRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campus não encontrado"));
        
        campus.setNumeroVagasReservadas(numeroVagasReservadas);
        campus.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia);
        Campus campusSalvo = campusRepository.save(campus);
        
        // Retornar os dados atualizados
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("campus", Map.of(
            "id", campusSalvo.getId(),
            "nome", campusSalvo.getNome(),
            "numeroVagasReservadas", campusSalvo.getNumeroVagasReservadas(),
            "numeroVagasAmplaConcorrencia", campusSalvo.getNumeroVagasAmplaConcorrencia(),
            "vagasReservadasOcupadas", campusSalvo.getVagasReservadasOcupadas(),
            "vagasAmplaOcupadas", campusSalvo.getVagasAmplaOcupadas(),
            "vagasReservadasDisponiveis", campusSalvo.getVagasReservadasDisponiveis(),
            "vagasAmplaDisponiveis", campusSalvo.getVagasAmplaDisponiveis()
        ));
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        logger.error("Erro ao editar campus {}", id, e);
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }
}

@PostMapping("/{id}/excluir-ajax")
@ResponseBody
public ResponseEntity<?> excluirCampusAjax(@PathVariable("id") Long id) {
    try {
        Campus campus = campusRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campus não encontrado"));
        
        // Verifica se há candidatos associados ao campus
        // Agora getCandidatos() retorna List<Candidato>, então podemos usar isEmpty()
        if (campus.getCandidatos() != null && !campus.getCandidatos().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Não é possível excluir o campus pois existem " + 
                         campus.getCandidatos().size() + " candidatos associados a ele."
            ));
        }

        // Verifica se há registros em campus_edital que referenciam este campus
        java.util.List<com.example.energif.model.CampusEdital> vinculacoes = campusEditalRepository.findAllByCampusId(id);
        if (vinculacoes != null && !vinculacoes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Não é possível excluir o campus pois existem " + vinculacoes.size() + " vínculos com editais (remova-os primeiro)."
            ));
        }
        
        campusRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    } catch (Exception e) {
        logger.error("Erro ao excluir campus {}", id, e);
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }
}
}