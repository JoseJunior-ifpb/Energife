package com.example.energif.web;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.energif.model.Campus;
import com.example.energif.model.Candidato;
import com.example.energif.model.TipoVaga;
import com.example.energif.model.SituacaoCandidato;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.CandidatoRepository;
import com.example.energif.util.Filtro;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/candidatos")
public class CandidatoController {

    private static final Logger logger = LoggerFactory.getLogger(CandidatoController.class);

    private final CandidatoRepository candidatoRepository;
    private final CampusRepository campusRepository;
    private final com.example.energif.repository.CampusEditalRepository campusEditalRepository;
    private final Filtro filtro;
    private final com.example.energif.service.CandidatoService candidatoService;
    private final com.example.energif.repository.MotivoRepository motivoRepository;
    private final com.example.energif.service.RelatorioService relatorioService;

    public CandidatoController(CandidatoRepository candidatoRepository, CampusRepository campusRepository,
            com.example.energif.repository.CampusEditalRepository campusEditalRepository, Filtro filtro,
            com.example.energif.service.CandidatoService candidatoService,
            com.example.energif.repository.MotivoRepository motivoRepository,
            com.example.energif.service.RelatorioService relatorioService) {
        this.candidatoRepository = candidatoRepository;
        this.campusRepository = campusRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.filtro = filtro;
        this.candidatoService = candidatoService;
        this.motivoRepository = motivoRepository;
        this.relatorioService = relatorioService;
    }

    // Return list of duplicate CPFs and their counts as JSON
    @GetMapping("/repetidos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> candidatosRepetidos() {
        try {
            List<Object[]> rows = candidatoRepository.findDuplicateCpfs();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object[] r : rows) {
                String cpf = r[0] != null ? r[0].toString() : null;
                long total = 0;
                if (r[1] instanceof Number)
                    total = ((Number) r[1]).longValue();
                else if (r[1] != null)
                    total = Long.parseLong(r[1].toString());
                out.add(Map.of("cpf", cpf, "total", total));
            }
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            logger.error("Erro ao listar candidatos repetidos", ex);
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Erro ao listar")));
        }
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
        // redirect bare /candidatos GET requests to the form to avoid
        // NoResourceFoundException
        return "redirect:/candidatos/novo";
    }

    @GetMapping("/list")
    public String listAll(
            @RequestParam(name = "order", required = false, defaultValue = "newest") String order,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "campusId", required = false) Long campusId,
            @RequestParam(name = "genero", required = false) String genero,
            @RequestParam(name = "idade", required = false) String idade,
            @RequestParam(name = "situacao", required = false) String situacao,
            @RequestParam(name = "turno", required = false) String turno, // Parâmetro recebido
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Model model) {

        // normalize incoming filter params: treat empty strings as null
        String generoNorm = (genero != null && !genero.isBlank()) ? genero.trim().toUpperCase() : null;
        Character generoChar = (generoNorm != null && !generoNorm.isEmpty()) ? generoNorm.charAt(0) : null;
        String idadeNorm = (idade != null && !idade.isBlank()) ? idade.trim() : null;

        // normalize situacao param: expected values são os nomes do enum SituacaoCandidato
        String situacaoFilter = (situacao != null && !situacao.isBlank()) ? situacao.trim() : null;

        // NOVO: Normalize o parâmetro turno (String)
        String turnoNorm = (turno != null && !turno.isBlank()) ? turno.trim() : null;

        // MUDANÇA: Inclua 'turnoNorm' na verificação 'usingFilters'
        boolean usingFilters = (q != null && !q.isBlank()) || campusId != null || generoChar != null
                || (idadeNorm != null && !idadeNorm.isBlank()) || situacaoFilter != null || turnoNorm != null;

        // Debug logging: show incoming params and whether filters will be applied
        logger.info(
                "Candidatos list request - order={}, q={}, campusId={}, genero={}, idade={}, situacao={}, turno={}, page={}, size={} → usingFilters={}",
                order, q, campusId, generoChar, idadeNorm, situacaoFilter, turnoNorm, page, size, usingFilters);

        org.springframework.data.domain.Sort sort;
        if (usingFilters) {
            // native query expects DB column names for ORDER BY
            if ("oldest".equalsIgnoreCase(order)) {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,
                        "data_inscricao", "hora_inscricao");
            } else {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "data_inscricao", "hora_inscricao");
            }
        } else {
            // JPA property names for non-native repository methods
            if ("oldest".equalsIgnoreCase(order)) {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,
                        "dataInscricao", "horaInscricao");
            } else {
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "dataInscricao", "horaInscricao");
            }
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                sort);
        // defensive: ensure page is not negative (prevents IllegalArgumentException
        // when PageRequest.of is called)
        if (page < 0) {
            logger.warn("Received negative page index ({}). Clamping to 0.", page);
            page = 0;
            pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        }
        org.springframework.data.domain.Page<com.example.energif.model.Candidato> pageResult;
        // Prepare a lowercase trimmed q for queries that compare with lower(...)
        String qParam = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;

        if (usingFilters) {
            // MUDANÇA: Passando o novo parâmetro 'turnoNorm' e situacaoFilter em vez de
            // habilitadoFilter
            pageResult = candidatoRepository.searchCombined(qParam, campusId, generoChar, idadeNorm, situacaoFilter,
                    turnoNorm, pageable);
        } else {
            pageResult = candidatoRepository.findAll(pageable);
        }

        model.addAttribute("selectedOrder", order);
        model.addAttribute("q", q);
        model.addAttribute("selectedCampus", campusId);
        model.addAttribute("selectedGenero", generoNorm);
        model.addAttribute("selectedIdade", idadeNorm);
        model.addAttribute("selectedSituacao", situacaoFilter);
        model.addAttribute("selectedTurno", turnoNorm); // MUDANÇA: Adicionando o valor normalizado ao modelo
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("motivos", motivoRepository.findAll());
        // provide list of distinct turnos for report/filter
        try {
            model.addAttribute("turnos", candidatoRepository.findDistinctTurno());
        } catch (Exception ex) {
            model.addAttribute("turnos", java.util.List.of());
        }
        model.addAttribute("candidatosPage", pageResult);
        model.addAttribute("candidatos", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "list";
    }

    @PostMapping
    public String criar(@ModelAttribute Candidato candidato,
            @RequestParam(required = false) String campusId,
            Model model) {
        logger.info("Recebendo candidato para salvar: {}", candidato);
        try {
            // Resolver campus baseado no ID ou nome
            if (campusId != null && !campusId.isBlank()) {
                Campus campus = resolveCampus(campusId);
                if (campus != null) {
                    candidato.setCampus(campus);
                    logger.info("Campus associado: {}", campus.getNome());
                }
            }

            Candidato saved = candidatoService.salvarComAtualizacao(candidato);
            logger.info("Candidato salvo: id={}, Campus: {}, Tipo Vaga: {}",
                    saved.getId(),
                    saved.getCampus() != null ? saved.getCampus().getNome() : "Nenhum",
                    saved.getTipoVaga());

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
        logger.info("Exclusão em massa iniciada: candidatos, campus_edital, campus");
        try {
            // Remove campus-edital links first to avoid FK constraint
            campusEditalRepository.deleteAll();
        } catch (Exception ex) {
            logger.warn("Falha ao apagar campus_edital antes de apagar campus: {}", ex.getMessage());
        }
        // delete candidates and campuses
        candidatoRepository.deleteAll();
        try {
            campusRepository.deleteAll();
        } catch (Exception ex) {
            logger.error("Erro ao apagar campi: {}", ex.getMessage());
        }
        return "redirect:/candidatos/list?deleted=all";
    }

    // No CandidatoController - método habilitarCandidato com logs detalhados:
    // No CandidatoController - atualize o método habilitarCandidato:
    @PostMapping("/{id}/habilitar")
    @ResponseBody
    public ResponseEntity<?> habilitarCandidato(@PathVariable("id") Long id,
            @RequestParam(name = "situacao", required = false) String situacao,
            @RequestParam(name = "motivo", required = false) String motivo,
            HttpServletRequest request) {

        logger.info("=== SOLICITAÇÃO DE HABILITAÇÃO ===");
        logger.info("Candidato ID: {}, Situação: {}, Motivo: {}", id, situacao, motivo);

        try {
            Map<String, Object> resultado;

            // normalize incoming situacao - esperamos valores em UPPERCASE (CLASSIFICADO, HABILITADO, etc)
            String situacaoNormalized = situacao != null ? situacao.trim().toUpperCase() : null;

            if ("CLASSIFICADO".equals(situacaoNormalized)) {
                logger.info("Tentando classificar candidato {}", id);
                resultado = candidatoService.habilitarCandidatoComFeedback(id, motivo);
                logger.info("Resultado da classificação: {}", resultado);
                return ResponseEntity.ok(resultado);

            } else if ("ELIMINADO".equals(situacaoNormalized)) {
                logger.info("Tentando marcar candidato {} como ELIMINADO", id);
                Candidato cand = candidatoRepository.findById(id).orElse(null);
                if (cand == null)
                    return ResponseEntity.badRequest()
                            .body(Map.of("sucesso", false, "mensagem", "Candidato não encontrado"));
                cand.setSituacao(SituacaoCandidato.ELIMINADO);
                cand.setMotivoNaoClassificacao(motivo);
                candidatoRepository.save(cand);
                return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Candidato marcado como eliminado"));

            } else if ("CADASTRO_RESERVA".equals(situacaoNormalized)) {
                // Marca o candidato como Cadastro de Reserva (suplente) e salva
                logger.info("Marcando candidato {} como CADASTRO_RESERVA", id);
                Candidato candidato = candidatoRepository.findById(id).orElse(null);
                if (candidato == null) {
                    logger.warn("Candidato {} não encontrado para marcar cadastro de reserva", id);
                    return ResponseEntity.badRequest()
                            .body(Map.of("sucesso", false, "mensagem", "Candidato não encontrado"));
                }

                // Validar se existem vagas de cadastro de reserva disponíveis
                Campus campus = candidato.getCampus();
                if (campus == null) {
                    logger.error("Candidato {} não possui campus definido", id);
                    return ResponseEntity.badRequest()
                            .body(Map.of("sucesso", false, "mensagem", "Candidato não possui campus definido"));
                }

                // Verificar se existem vagas de cadastro de reserva configuradas
                Integer vagasCadastroReserva = campus.getNumeroVagasCadastroReserva();
                if (vagasCadastroReserva == null || vagasCadastroReserva <= 0) {
                    logger.warn("Nenhuma vaga de cadastro de reserva disponível no campus {} para candidato {}",
                            campus.getNome(), id);
                    resultado = Map.of(
                            "sucesso", false,
                            "mensagem",
                            "Não existem vagas de cadastro de reserva disponíveis no campus " + campus.getNome()
                                    + ". Por favor, configure o número de vagas de reserva no cadastro do campus.",
                            "campusNome", campus.getNome(),
                            "vagasDisponiveis", 0);
                    return ResponseEntity.ok(resultado);
                }

                candidato.setTipoVaga(TipoVaga.CADASTRO_RESERVA);
                candidato.setSituacao(SituacaoCandidato.CADASTRO_RESERVA);
                candidato.setMotivoNaoClassificacao(null);
                candidatoRepository.save(candidato);
                resultado = Map.of(
                        "sucesso", true,
                        "mensagem", "Candidato marcado como cadastro de reserva com sucesso",
                        "tipoVaga", "CADASTRO_RESERVA");
                return ResponseEntity.ok(resultado);

            } else if ("HABILITADO".equals(situacaoNormalized)) {
                // mark as HABILITADO — com verificação de vagas
                logger.info("Tentando marcar candidato {} como HABILITADO", id);
                resultado = candidatoService.marcarComoHabilitado(id);
                return ResponseEntity.ok(resultado);

            } else if ("PENDENTE".equals(situacaoNormalized)) {
                // mark as PENDENTE
                Candidato cand = candidatoRepository.findById(id).orElse(null);
                if (cand == null)
                    return ResponseEntity.badRequest()
                            .body(Map.of("sucesso", false, "mensagem", "Candidato não encontrado"));
                cand.setSituacao(SituacaoCandidato.PENDENTE);
                cand.setMotivoNaoClassificacao(null);
                candidatoRepository.save(cand);
                return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Candidato marcado como pendente"));

            } else {
                logger.warn("Situação inválida: {}", situacao);
                resultado = Map.of(
                        "sucesso", false,
                        "mensagem", "Situação inválida: " + situacao);
                return ResponseEntity.ok(resultado);
            }

        } catch (Exception ex) {
            logger.error("ERRO ao atualizar situação do candidato {}", id, ex);
            Map<String, Object> erro = Map.of(
                    "sucesso", false,
                    "mensagem", "Erro interno: " + ex.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    // Generate PDF report for a given campus and optional turno
    @GetMapping("/report")
    public void gerarRelatorio(@RequestParam(name = "campusId", required = false) Long campusId,
            @RequestParam(name = "turno", required = false) String turno,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            // prepare filename
            String filename = "relatorio_resultado_final.pdf";
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            // build PDF using OpenPDF
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, response.getOutputStream());
            doc.open();

            // Se um campus específico foi solicitado
            if (campusId != null) {
                com.example.energif.model.Campus campus = campusRepository.findById(campusId).orElse(null);
                java.util.List<com.example.energif.model.Candidato> candidatos;
                
                if (turno != null && !turno.isBlank()) {
                    candidatos = candidatoRepository
                            .findAllByCampusIdAndTurnoOrderByDataInscricaoAscHoraInscricao(campusId, turno);
                } else {
                    candidatos = candidatoRepository.findAllByCampusIdOrderByDataInscricaoAscHoraInscricao(campusId);
                }

                // Agrupar por campus para usar o serviço
                java.util.Map<com.example.energif.model.Campus, java.util.List<com.example.energif.model.Candidato>> grouped = 
                    java.util.Map.of(campus, candidatos);
                java.util.List<com.example.energif.model.Campus> campusList = java.util.List.of(campus);
                
                relatorioService.gerarRelatorioPDF(doc, campusList, grouped, turno);
            } else {
                // Sem campus específico: gerar para todos
                java.util.List<com.example.energif.model.Candidato> all = candidatoRepository.findAll(
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,
                                "dataInscricao", "horaInscricao"));
                
                if (turno != null && !turno.isBlank()) {
                    all = all.stream().filter(c -> c.getTurno() != null && turno.equals(c.getTurno())).toList();
                }

                java.util.Map<com.example.energif.model.Campus, java.util.List<com.example.energif.model.Candidato>> grouped = all
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(com.example.energif.model.Candidato::getCampus,
                                java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

                java.util.List<com.example.energif.model.Campus> campusList = new java.util.ArrayList<>(grouped.keySet());
                relatorioService.gerarRelatorioPDF(doc, campusList, grouped, turno);
            }

            doc.close();

        } catch (Exception e) {
            logger.error("Erro ao gerar relatorio PDF", e);
            try {
                response.sendError(500, "Erro ao gerar relatorio: " + e.getMessage());
            } catch (java.io.IOException ex) {
            }
        }
    }

    private Campus resolveCampus(String campusId) {
        try {
            Long cid = Long.parseLong(campusId);
            return campusRepository.findById(cid).orElse(null);
        } catch (NumberFormatException nfe) {
            return campusRepository.findByNome(campusId);
        }
    }

}
