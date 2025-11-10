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

    public CandidatoController(CandidatoRepository candidatoRepository, CampusRepository campusRepository, com.example.energif.repository.CampusEditalRepository campusEditalRepository, Filtro filtro, com.example.energif.service.CandidatoService candidatoService, com.example.energif.repository.MotivoRepository motivoRepository) {
        this.candidatoRepository = candidatoRepository;
        this.campusRepository = campusRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.filtro = filtro;
        this.candidatoService = candidatoService;
        this.motivoRepository = motivoRepository;
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
                if (r[1] instanceof Number) total = ((Number) r[1]).longValue();
                else if (r[1] != null) total = Long.parseLong(r[1].toString());
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
    @RequestParam(name = "habilitado", required = false) String habilitado,
    @RequestParam(name = "turno", required = false) String turno, // Parâmetro recebido
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "20") int size,
        Model model) {

    // normalize incoming filter params: treat empty strings as null
    String generoNorm = (genero != null && !genero.isBlank()) ? genero.trim().toUpperCase() : null;
    Character generoChar = (generoNorm != null && !generoNorm.isEmpty()) ? generoNorm.charAt(0) : null;
    String idadeNorm = (idade != null && !idade.isBlank()) ? idade.trim() : null;

    // normalize habilitado param: expected values 'sim' / 'nao' or empty
    String habilitadoNorm = (habilitado != null && !habilitado.isBlank()) ? habilitado.trim().toLowerCase() : null;
    Boolean habilitadoFilter = null;
    if ("sim".equals(habilitadoNorm)) habilitadoFilter = Boolean.TRUE;
    else if ("nao".equals(habilitadoNorm)) habilitadoFilter = Boolean.FALSE;
    
    // NOVO: Normalize o parâmetro turno (String)
    String turnoNorm = (turno != null && !turno.isBlank()) ? turno.trim() : null;

    // MUDANÇA: Inclua 'turnoNorm' na verificação 'usingFilters'
    boolean usingFilters = (q != null && !q.isBlank()) || campusId != null || generoChar != null || (idadeNorm != null && !idadeNorm.isBlank()) || habilitadoFilter != null || turnoNorm != null;

    // Debug logging: show incoming params and whether filters will be applied
    logger.info("Candidatos list request - order={}, q={}, campusId={}, genero={}, idade={}, habilitado={}, turno={}, page={}, size={} → usingFilters={}",
                 order, q, campusId, generoChar, idadeNorm, habilitadoNorm, turnoNorm, page, size, usingFilters); // Use turnoNorm no log

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
        // defensive: ensure page is not negative (prevents IllegalArgumentException when PageRequest.of is called)
        if (page < 0) {
            logger.warn("Received negative page index ({}). Clamping to 0.", page);
            page = 0;
            pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        }
        org.springframework.data.domain.Page<com.example.energif.model.Candidato> pageResult;
        // Prepare a lowercase trimmed q for queries that compare with lower(...)
        String qParam = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;
        
        if (usingFilters) {
            // MUDANÇA: Passando o novo parâmetro 'turnoNorm'
            pageResult = candidatoRepository.searchCombined(qParam, campusId, generoChar, idadeNorm, habilitadoFilter, turnoNorm, pageable);
        } else {
            pageResult = candidatoRepository.findAll(pageable);
        }

    model.addAttribute("selectedOrder", order);
    model.addAttribute("q", q);
    model.addAttribute("selectedCampus", campusId);
    model.addAttribute("selectedGenero", generoNorm);
    model.addAttribute("selectedIdade", idadeNorm);
    model.addAttribute("selectedHabilitado", habilitadoNorm);
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
        
        if ("sim".equalsIgnoreCase(situacao)) {
            logger.info("Tentando habilitar candidato {}", id);
            resultado = candidatoService.habilitarCandidatoComFeedback(id, motivo);
            logger.info("Resultado da habilitação: {}", resultado);
        } else if ("nao".equalsIgnoreCase(situacao)) {
            logger.info("Tentando desabilitar candidato {}", id);
            candidatoService.desabilitarCandidato(id, motivo);
            resultado = Map.of(
                "sucesso", true,
                "mensagem", "Candidato desabilitado com sucesso"
            );
        } else {
            logger.warn("Situação inválida: {}", situacao);
            resultado = Map.of(
                "sucesso", false,
                "mensagem", "Situação inválida: " + situacao
            );
        }
        
        return ResponseEntity.ok(resultado);
        
    } catch (Exception ex) {
        logger.error("ERRO ao atualizar situação do candidato {}", id, ex);
        Map<String, Object> erro = Map.of(
            "sucesso", false,
            "mensagem", "Erro interno: " + ex.getMessage()
        );
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

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8);

            // Header
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Resultado Final", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(com.lowagie.text.Chunk.NEWLINE);

            String turnoLine = (turno != null && !turno.isBlank()) ? (turno) : "Todos os Turnos";

            // If a specific campus was requested, keep the old behaviour (single section)
            if (campusId != null) {
                com.example.energif.model.Campus campus = campusRepository.findById(campusId).orElse(null);
                java.util.List<com.example.energif.model.Candidato> candidatos;
                if (turno != null && !turno.isBlank()) {
                    candidatos = candidatoRepository.findAllByCampusIdAndTurnoOrderByDataInscricaoAscHoraInscricao(campusId, turno);
                } else {
                    candidatos = candidatoRepository.findAllByCampusIdOrderByDataInscricaoAscHoraInscricao(campusId);
                }

                // Campus header
                String campusLine = "Campus: " + (campus != null ? campus.getNome() : "-");
                com.lowagie.text.Paragraph info = new com.lowagie.text.Paragraph(campusLine + " - " + turnoLine, normalFont);
                info.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                doc.add(info);
                doc.add(com.lowagie.text.Chunk.NEWLINE);

                java.time.format.DateTimeFormatter dateF = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.format.DateTimeFormatter timeF = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

                if (turno != null && !turno.isBlank()) {
                    // single turno: render one table
                    com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[]{2f, 4f, 2f, 4f});
                    table.setWidthPercentage(100);
                    table.addCell(new com.lowagie.text.Phrase("Data e Hora da Inscrição", headerFont));
                    table.addCell(new com.lowagie.text.Phrase("Nome Completo", headerFont));
                    table.addCell(new com.lowagie.text.Phrase("Classificação", headerFont));
                    table.addCell(new com.lowagie.text.Phrase("Situação", headerFont));

                    int rank = 0;
                    for (com.example.energif.model.Candidato c : candidatos) {
                        String dateTime = "-";
                        if (c.getDataInscricao() != null) {
                            dateTime = c.getDataInscricao().format(dateF);
                            if (c.getHoraInscricao() != null) dateTime += " " + c.getHoraInscricao().format(timeF);
                        }
                        table.addCell(new com.lowagie.text.Phrase(dateTime, normalFont));
                        table.addCell(new com.lowagie.text.Phrase(c.getNome() != null ? c.getNome() : "-", normalFont));
                        if (Boolean.TRUE.equals(c.getHabilitado())) {
                            rank++;
                            table.addCell(new com.lowagie.text.Phrase(rank + "°", normalFont));
                            table.addCell(new com.lowagie.text.Phrase("Habilitado", normalFont));
                        } else {
                            table.addCell(new com.lowagie.text.Phrase("-", normalFont));
                            String motivo = c.getMotivoNaoHabilitacao();
                            String situ = "Eliminado" + (motivo != null && !motivo.isBlank() ? " - " + motivo : "");
                            table.addCell(new com.lowagie.text.Phrase(situ, normalFont));
                        }
                    }
                    doc.add(table);
                } else {
                    // multiple turnos: group by turno and render a table per turno
                    java.util.Map<String, java.util.List<com.example.energif.model.Candidato>> byTurno =
                            candidatos.stream().collect(java.util.stream.Collectors.groupingBy(c -> c.getTurno() != null ? c.getTurno() : "(sem turno)", java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

                    for (java.util.Map.Entry<String, java.util.List<com.example.energif.model.Candidato>> te : byTurno.entrySet()) {
                        String turnoName = te.getKey();
                        com.lowagie.text.Paragraph turnoPara = new com.lowagie.text.Paragraph("Turno: " + turnoName, normalFont);
                        turnoPara.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                        doc.add(turnoPara);
                        doc.add(com.lowagie.text.Chunk.NEWLINE);

                        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[]{2f, 4f, 2f, 4f});
                        table.setWidthPercentage(100);
                        table.addCell(new com.lowagie.text.Phrase("Data e Hora da Inscrição", headerFont));
                        table.addCell(new com.lowagie.text.Phrase("Nome Completo", headerFont));
                        table.addCell(new com.lowagie.text.Phrase("Classificação", headerFont));
                        table.addCell(new com.lowagie.text.Phrase("Situação", headerFont));

                        int rank = 0;
                        for (com.example.energif.model.Candidato c : te.getValue()) {
                            String dateTime = "-";
                            if (c.getDataInscricao() != null) {
                                dateTime = c.getDataInscricao().format(dateF);
                                if (c.getHoraInscricao() != null) dateTime += " " + c.getHoraInscricao().format(timeF);
                            }
                            table.addCell(new com.lowagie.text.Phrase(dateTime, normalFont));
                            table.addCell(new com.lowagie.text.Phrase(c.getNome() != null ? c.getNome() : "-", normalFont));
                            if (Boolean.TRUE.equals(c.getHabilitado())) {
                                rank++;
                                table.addCell(new com.lowagie.text.Phrase(rank + "°", normalFont));
                                table.addCell(new com.lowagie.text.Phrase("Habilitado", normalFont));
                            } else {
                                table.addCell(new com.lowagie.text.Phrase("-", normalFont));
                                String motivo = c.getMotivoNaoHabilitacao();
                                String situ = "Eliminado" + (motivo != null && !motivo.isBlank() ? " - " + motivo : "");
                                table.addCell(new com.lowagie.text.Phrase(situ, normalFont));
                            }
                        }
                        doc.add(table);
                        doc.add(com.lowagie.text.Chunk.NEWLINE);
                    }
                }

            } else {
                // No campus specified: group candidates by campus and produce one section per campus
                java.util.List<com.example.energif.model.Candidato> all = candidatoRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "dataInscricao", "horaInscricao"));
                if (turno != null && !turno.isBlank()) {
                    all = all.stream().filter(c -> c.getTurno() != null && turno.equals(c.getTurno())).toList();
                }

                java.util.Map<com.example.energif.model.Campus, java.util.List<com.example.energif.model.Candidato>> grouped =
                        all.stream().collect(java.util.stream.Collectors.groupingBy(com.example.energif.model.Candidato::getCampus, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

                java.time.format.DateTimeFormatter dateF = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.format.DateTimeFormatter timeF = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

                for (java.util.Map.Entry<com.example.energif.model.Campus, java.util.List<com.example.energif.model.Candidato>> e : grouped.entrySet()) {
                    com.example.energif.model.Campus campusKey = e.getKey();
                    java.util.List<com.example.energif.model.Candidato> list = e.getValue();

                    String campusLine = "Campus: " + (campusKey != null ? campusKey.getNome() : "Sem Campus");
                    com.lowagie.text.Paragraph info = new com.lowagie.text.Paragraph(campusLine + " - " + turnoLine, normalFont);
                    info.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                    doc.add(info);
                    doc.add(com.lowagie.text.Chunk.NEWLINE);

                    if (list.isEmpty()) {
                        com.lowagie.text.Paragraph none = new com.lowagie.text.Paragraph("Nenhum candidato para este campus.", normalFont);
                        doc.add(none);
                        doc.add(com.lowagie.text.Chunk.NEWLINE);
                    } else {
                        // If a specific turno was requested earlier, list contains only that turno.
                        // Otherwise, group by turno inside this campus and produce one table per turno.
                        java.util.Map<String, java.util.List<com.example.energif.model.Candidato>> byTurno =
                                list.stream().collect(java.util.stream.Collectors.groupingBy(c -> c.getTurno() != null ? c.getTurno() : "(sem turno)", java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

                        for (java.util.Map.Entry<String, java.util.List<com.example.energif.model.Candidato>> te : byTurno.entrySet()) {
                            String turnoName = te.getKey();
                            com.lowagie.text.Paragraph turnoPara = new com.lowagie.text.Paragraph("Turno: " + turnoName, normalFont);
                            turnoPara.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                            doc.add(turnoPara);
                            doc.add(com.lowagie.text.Chunk.NEWLINE);

                            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[]{2f, 4f, 2f, 4f});
                            table.setWidthPercentage(100);
                            table.addCell(new com.lowagie.text.Phrase("Data e Hora da Inscrição", headerFont));
                            table.addCell(new com.lowagie.text.Phrase("Nome Completo", headerFont));
                            table.addCell(new com.lowagie.text.Phrase("Classificação", headerFont));
                            table.addCell(new com.lowagie.text.Phrase("Situação", headerFont));

                            int rank = 0;
                            for (com.example.energif.model.Candidato c : te.getValue()) {
                                String dateTime = "-";
                                if (c.getDataInscricao() != null) {
                                    dateTime = c.getDataInscricao().format(dateF);
                                    if (c.getHoraInscricao() != null) dateTime += " " + c.getHoraInscricao().format(timeF);
                                }
                                table.addCell(new com.lowagie.text.Phrase(dateTime, normalFont));
                                table.addCell(new com.lowagie.text.Phrase(c.getNome() != null ? c.getNome() : "-", normalFont));
                                if (Boolean.TRUE.equals(c.getHabilitado())) {
                                    rank++;
                                    table.addCell(new com.lowagie.text.Phrase(rank + "°", normalFont));
                                    table.addCell(new com.lowagie.text.Phrase("Habilitado", normalFont));
                                } else {
                                    table.addCell(new com.lowagie.text.Phrase("-", normalFont));
                                    String motivo = c.getMotivoNaoHabilitacao();
                                    String situ = "Eliminado" + (motivo != null && !motivo.isBlank() ? " - " + motivo : "");
                                    table.addCell(new com.lowagie.text.Phrase(situ, normalFont));
                                }
                            }
                            doc.add(table);
                            doc.add(com.lowagie.text.Chunk.NEWLINE);
                        }
                    }

                    // page break between campuses
                    doc.newPage();
                }
            }
            doc.close();

        } catch (Exception e) {
            logger.error("Erro ao gerar relatorio PDF", e);
            try { response.sendError(500, "Erro ao gerar relatorio: " + e.getMessage()); } catch (java.io.IOException ex) {}
        }
    }
    private Campus resolveCampus(String campusId) {
    try {
        Long cid = Long.parseLong(campusId);
        return campusRepository.findById(cid).orElse(null);
    } catch (NumberFormatException nfe) {
        return campusRepository.findByNome(campusId);
    }}
    
}
