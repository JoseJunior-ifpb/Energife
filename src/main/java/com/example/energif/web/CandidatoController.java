package com.example.energif.web;

import com.example.energif.model.Candidato;
import com.example.energif.repository.CandidatoRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.model.Campus;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.example.energif.util.Filtro;

@Controller
@RequestMapping("/candidatos")
public class CandidatoController {

    private static final Logger logger = LoggerFactory.getLogger(CandidatoController.class);

    private final CandidatoRepository candidatoRepository;
    private final CampusRepository campusRepository;
    private final Filtro filtro;
    private final com.example.energif.service.CandidatoService candidatoService;

    public CandidatoController(CandidatoRepository candidatoRepository, CampusRepository campusRepository, Filtro filtro, com.example.energif.service.CandidatoService candidatoService) {
        this.candidatoRepository = candidatoRepository;
        this.campusRepository = campusRepository;
        this.filtro = filtro;
        this.candidatoService = candidatoService;
    }

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("candidato", new Candidato());
        // provide list of campuses for selection in the form
        model.addAttribute("campuses", campusRepository.findAll());
        return "cadastro-candidato";
    }

    @GetMapping
    public String index() {
        // redirect bare /candidatos GET requests to the form to avoid NoResourceFoundException
        return "redirect:/candidatos/novo";
    }

    @GetMapping("/list")
    public String listAll(
            @RequestParam(name = "order", required = false, defaultValue = "newest") String order,
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "campusId", required = false) Long campusId,
    @RequestParam(name = "genero", required = false) String genero,
    @RequestParam(name = "idade", required = false) String idade,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Model model) {

    // normalize incoming filter params: treat empty strings as null
    String generoNorm = (genero != null && !genero.isBlank()) ? genero.trim().toUpperCase() : null;
    Character generoChar = (generoNorm != null && !generoNorm.isEmpty()) ? generoNorm.charAt(0) : null;
    String idadeNorm = (idade != null && !idade.isBlank()) ? idade.trim() : null;

    boolean usingFilters = (q != null && !q.isBlank()) || campusId != null || generoChar != null || (idadeNorm != null && !idadeNorm.isBlank());

        org.springframework.data.domain.Sort sort;
        if (usingFilters) {
            // native query expects DB column names for ORDER BY
            if ("oldest".equalsIgnoreCase(order)) {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "data_inscricao", "hora_inscricao");
            } else {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "data_inscricao", "hora_inscricao");
            }
        } else {
            // JPA property names for non-native repository methods
            if ("oldest".equalsIgnoreCase(order)) {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "dataInscricao", "horaInscricao");
            } else {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dataInscricao", "horaInscricao");
            }
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        org.springframework.data.domain.Page<com.example.energif.model.Candidato> pageResult;
        // Prepare a lowercase trimmed q for queries that compare with lower(...)
        String qParam = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;
        if (usingFilters) {
            pageResult = candidatoRepository.searchCombined(qParam, campusId, generoChar, idadeNorm, pageable);
        } else {
            pageResult = candidatoRepository.findAll(pageable);
        }

    model.addAttribute("selectedOrder", order);
    model.addAttribute("q", q);
    model.addAttribute("selectedCampus", campusId);
    model.addAttribute("selectedGenero", generoNorm);
    model.addAttribute("selectedIdade", idadeNorm);
    model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("candidatosPage", pageResult);
        model.addAttribute("candidatos", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "list";
    }

    @PostMapping
    public String criar(@ModelAttribute Candidato candidato,
                        @RequestParam(required = false) String campusId,
                        @RequestParam(required = false) Integer numeroVagasReservadas,
                        @RequestParam(required = false) Integer numeroVagasAmplaConcorrencia,
                        @RequestParam(required = false) Long editalId,
                        Model model) {
        logger.info("Recebendo candidato para salvar: {}", candidato);
        try {
            // resolve campus if provided as id or nome
            if (campusId != null && !campusId.isBlank()) {
                try {
                    Long cid = Long.parseLong(campusId);
                    campusRepository.findById(cid).ifPresent(c -> {
                        // set optional campus numeric params if provided
                        if (numeroVagasReservadas != null) c.setNumeroVagasReservadas(numeroVagasReservadas);
                        if (numeroVagasAmplaConcorrencia != null) c.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia);
                        candidato.setCampus(c);
                    });
                } catch (NumberFormatException nfe) {
                    Campus byName = campusRepository.findByNome(campusId);
                    if (byName != null) {
                        if (numeroVagasReservadas != null) byName.setNumeroVagasReservadas(numeroVagasReservadas);
                        if (numeroVagasAmplaConcorrencia != null) byName.setNumeroVagasAmplaConcorrencia(numeroVagasAmplaConcorrencia);
                        candidato.setCampus(byName);
                    }
                }
            }
            Candidato saved = candidatoService.salvarComAtualizacao(candidato, numeroVagasReservadas, numeroVagasAmplaConcorrencia, editalId);
            logger.info("Candidato salvo: id={}", saved.getId());
            return "redirect:/candidatos/novo?success";
        } catch (Exception e) {
            logger.error("Erro ao salvar candidato", e);
            return "redirect:/candidatos/novo?error";
        }
    }

    @PostMapping("/import")
    public String importXlsx(@RequestPart("file") MultipartFile file,
                             @RequestParam(name = "editalDescricao", required = false) String editalDescricao,
                             Model model) {
        if (file == null || file.isEmpty()) {
            return "redirect:/candidatos/list?error=empty";
        }
        try {
            // save to temp file for processing by the importer
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("upload-", ".xlsx");
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Uploaded file saved to {}", tmp);
            // invoke Filtro importer to parse and import rows
            try {
                int imported = filtro.importarXlsx(tmp.toFile(), editalDescricao);
                return "redirect:/candidatos/list?import=ok&count=" + imported;
            } catch (Exception ex) {
                logger.error("Erro ao importar xlsx", ex);
                return "redirect:/candidatos/list?import=error";
            }
        } catch (Exception e) {
            logger.error("Erro ao receber arquivo de importacao", e);
            return "redirect:/candidatos/list?import=error";
        }
    }

    @PostMapping("/delete-all")
    public String deleteAll() {
        candidatoRepository.deleteAll();
        return "redirect:/candidatos/list?deleted=all";
    }

    @PostMapping("/{id}/habilitar")
    public Object habilitarCandidato(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                                     @RequestParam(name = "situacao", required = false) String situacao,
                                     @RequestParam(name = "motivo", required = false) String motivo,
                                     jakarta.servlet.http.HttpServletRequest request) {
        try {
            var opt = candidatoRepository.findById(id);
            if (opt.isPresent()) {
                var c = opt.get();
                boolean habil = "sim".equalsIgnoreCase(situacao);
                c.setHabilitado(habil);
                if (!habil) {
                    c.setMotivoNaoHabilitacao(motivo);
                } else {
                    c.setMotivoNaoHabilitacao(null);
                }
                candidatoRepository.save(c);
            }
        } catch (Exception ex) {
            logger.error("Erro ao atualizar situação do candidato {}", id, ex);
        }
        // If the request is AJAX, return 200 OK without redirect so client JS can handle UI updates
        String xrw = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xrw)) {
            return org.springframework.http.ResponseEntity.ok().build();
        }
        return "redirect:/candidatos/list";
    }
}
