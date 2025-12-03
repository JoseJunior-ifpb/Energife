package com.example.energif.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.energif.model.Campus;
import com.example.energif.model.CampusEdital;
import com.example.energif.model.CampusEditalTurno;
import com.example.energif.model.Candidato;
import com.example.energif.model.SituacaoCandidato;
import com.example.energif.model.TipoVaga;
import com.example.energif.repository.CampusEditalRepository;
import com.example.energif.repository.CampusEditalTurnoRepository;
import com.example.energif.repository.CampusRepository;
import com.example.energif.repository.CandidatoRepository;

/**
 * Utility that reads an .xlsx file and imports rows into the Candidato table.
 * It looks for columns (by header text approximate match):
 * - Carimbo de data/hora
 * - Campus (cidade) e turno
 * - Nome Completo
 * - Gênero
 * - Data de Nascimento
 * - CPF
 */
@Component
public class Filtro {

    private static final Logger log = LoggerFactory.getLogger(Filtro.class);

    private final CandidatoRepository candidatoRepository;
    private final CampusRepository campusRepository;
    private final com.example.energif.repository.EditalRepository editalRepository;
    private final CampusEditalRepository campusEditalRepository;
    private final CampusEditalTurnoRepository turnoRepository;

    public Filtro(CandidatoRepository candidatoRepository, CampusRepository campusRepository,
            com.example.energif.repository.EditalRepository editalRepository,
            CampusEditalRepository campusEditalRepository, CampusEditalTurnoRepository turnoRepository) {
        this.candidatoRepository = candidatoRepository;
        this.campusRepository = campusRepository;
        this.editalRepository = editalRepository;
        this.campusEditalRepository = campusEditalRepository;
        this.turnoRepository = turnoRepository;
    }

    @Transactional
    public int importarXlsx(File xlsxFile) throws Exception {
        return importarXlsx(xlsxFile, null);
    }

    @Transactional
    public int importarXlsx(File xlsxFile, String editalDescricao) throws Exception {
        com.example.energif.model.Edital targetEdital = null;
        if (editalDescricao != null && !editalDescricao.isBlank()) {
            // find-or-create by descricao
            var list = editalRepository.findAll();
            for (var ed : list) {
                if (editalDescricao.equals(ed.getDescricao())) {
                    targetEdital = ed;
                    break;
                }
            }
            if (targetEdital == null) {
                targetEdital = new com.example.energif.model.Edital();
                targetEdital.setDescricao(editalDescricao);
                targetEdital = editalRepository.save(targetEdital);
            }
        }

        try (InputStream is = new FileInputStream(xlsxFile);
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                log.warn("Planilha vazia: {}", xlsxFile.getAbsolutePath());
                return 0;
            }

            Iterator<Row> rowIt = sheet.iterator();
            if (!rowIt.hasNext())
                return 0;

            // read header
            Row header = rowIt.next();
            Map<String, Integer> colIndex = mapHeaderIndices(header);

            int imported = 0;
            while (rowIt.hasNext()) {
                Row r = rowIt.next();
                try {
                    Candidato c = mapRowToCandidato(r, colIndex);
                    if (c != null) {
                        if (targetEdital != null)
                            c.setEdital(targetEdital);
                        candidatoRepository.save(c);

                        // Se importamos dentro de um edital, garanta os turnos padrão
                        // (Manhã/Tarde/Noite)
                        if (c.getCampus() != null && c.getEdital() != null) {
                            criarTurnosPadraoSeNaoExistir(c.getCampus(), c.getEdital());
                            // também garanta o turno específico da linha (caso seja diferente da lista
                            // padrão)
                            if (c.getTurno() != null && !c.getTurno().isBlank()) {
                                criarTurnoSeNaoExistir(c.getCampus(), c.getEdital(), c.getTurno());
                            }
                        }

                        imported++;
                    }
                } catch (Exception ex) {
                    log.warn("Falha ao importar linha {}: {}", r.getRowNum(), ex.getMessage());
                }
            }

            log.info("Import finished - {} records imported from {}", imported, xlsxFile.getName());
            return imported;
        }
    }

    private Map<String, Integer> mapHeaderIndices(Row header) {
        Map<String, Integer> map = new HashMap<>();

        System.out.println("=== DEBUG HEADER COLUMNS ===");
        for (Cell cell : header) {
            String txt = safeString(cell);
            System.out.println("Coluna " + cell.getColumnIndex() + ": '" + txt + "'");

            // BUSCA EXATA pelas colunas específicas
            if (txt.equals("Carimbo de data/hora")) {
                map.put("timestamp", cell.getColumnIndex());
                System.out.println(">>> CARIMBO DE DATA/HORA EXATO encontrado na coluna: " + cell.getColumnIndex());
            } else if (txt.equals("Campus (cidade) e turno:")) {
                map.put("campus_turno", cell.getColumnIndex());
                System.out.println(">>> CAMPUS E TURNO EXATO encontrado na coluna: " + cell.getColumnIndex());
            } else if (txt.equals("Nome Completo")) {
                map.put("nome", cell.getColumnIndex());
                System.out.println(">>> NOME COMPLETO EXATO encontrado na coluna: " + cell.getColumnIndex());
            } else if (txt.equals("Gênero")) {
                map.put("genero", cell.getColumnIndex());
                System.out.println(">>> GÊNERO EXATO encontrado na coluna: " + cell.getColumnIndex());
            } else if (txt.equals("Data de Nascimento")) {
                map.put("dataNascimento", cell.getColumnIndex());
                System.out.println(">>> DATA DE NASCIMENTO EXATA encontrada na coluna: " + cell.getColumnIndex());
            } else if (txt.equals("CPF")) {
                map.put("cpf", cell.getColumnIndex());
                System.out.println(">>> CPF EXATO encontrado na coluna: " + cell.getColumnIndex());
            }
            // REMOVIDO: busca por "tipo_vaga" pois agora é determinado pelo gênero
        }

        // Verificação se todas as colunas obrigatórias foram encontradas
        System.out.println("=== VERIFICAÇÃO DE COLUNAS OBRIGATÓRIAS ===");
        String[] obrigatorias = { "timestamp", "campus_turno", "nome", "genero", "dataNascimento", "cpf" };
        for (String col : obrigatorias) {
            if (!map.containsKey(col)) {
                System.out.println("!!! ERRO: Coluna obrigatória não encontrada: " + col);
            } else {
                System.out.println("✓ Coluna " + col + " encontrada na posição: " + map.get(col));
            }
        }

        System.out.println("Map final: " + map);
        System.out.println("=============================");

        return map;
    }

    /**
     * Normalize a raw turno string into one of the canonical values: "Manhã",
     * "Tarde", "Noite".
     * If the input doesn't match known patterns, return trimmed original.
     */
    private String normalizeTurno(String raw) {
        if (raw == null)
            return null;
        String s = raw.trim();
        if (s.isEmpty())
            return null;
        try {
            // remove accents to ease matching
            String ascii = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            String lower = ascii.toLowerCase();
            if (lower.contains("manha") || lower.startsWith("m") || lower.startsWith("man"))
                return "Manhã";
            if (lower.contains("tarde") || lower.startsWith("t"))
                return "Tarde";
            if (lower.contains("noite") || lower.startsWith("n"))
                return "Noite";
        } catch (Exception e) {
            // fallthrough
        }
        // fallback: return trimmed original with first-letter capitalized
        return s.substring(0, 1).toUpperCase() + (s.length() > 1 ? s.substring(1) : "");
    }

    private Candidato mapRowToCandidato(Row r, Map<String, Integer> cols) {
        // read fields
        String nome = getCellString(r, cols.get("nome"));
        if (nome == null || nome.isBlank())
            return null; // skip empty rows

        String rawCpf = getCellString(r, cols.get("cpf"));
        String cpf = rawCpf;
        if (cpf != null) {
            cpf = cpf.replaceAll("\\D+", "");
        }

        if (rawCpf != null && !rawCpf.isBlank()) {
            log.debug("Row {}: raw CPF='{}' -> sanitized='{}'", r.getRowNum(), rawCpf, cpf);
        }

        String generoStr = getCellString(r, cols.get("genero"));
        Character genero = null;
        if (generoStr != null && !generoStr.isBlank()) {
            // Normaliza o gênero para M ou F
            if (generoStr.toUpperCase().startsWith("M") || generoStr.equalsIgnoreCase("Masculino")) {
                genero = 'M';
            } else if (generoStr.toUpperCase().startsWith("F") || generoStr.equalsIgnoreCase("Feminino")) {
                genero = 'F';
            }
        }

        LocalDate dataNasc = getCellLocalDate(r, cols.get("dataNascimento"));

        // timestamp -> dataInscricao + horaInscricao
        LocalDate dataInscricao = null;
        LocalTime horaInscricao = null;
        if (cols.containsKey("timestamp")) {
            Cell tsCell = r.getCell(cols.get("timestamp"));
            if (tsCell != null && tsCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(tsCell)) {
                Instant instant = tsCell.getDateCellValue().toInstant();
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                dataInscricao = ldt.toLocalDate();
                horaInscricao = ldt.toLocalTime();
            } else {
                String s = getCellString(r, cols.get("timestamp"));
                if (s != null && !s.isBlank()) {
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(s);
                        dataInscricao = ldt.toLocalDate();
                        horaInscricao = ldt.toLocalTime();
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        // campus and turno may be in the same column
        String campusName = null;
        String turno = null;
        if (cols.containsKey("campus_turno")) {
            String v = getCellString(r, cols.get("campus_turno"));
            if (v != null) {
                String[] parts = v.split("[-|,|/]", 2);
                if (parts.length == 2) {
                    campusName = parts[0].trim();
                    turno = normalizeTurno(parts[1].trim());
                } else {
                    String[] ws = v.split("\\s{2,}|\\r?\\n");
                    if (ws.length >= 2) {
                        campusName = ws[0].trim();
                        turno = normalizeTurno(ws[1].trim());
                    } else {
                        campusName = v.trim();
                    }
                }
            }
        } else {
            campusName = getCellString(r, cols.get("campus"));
            turno = getCellString(r, cols.get("turno"));
        }

        // NOVA LÓGICA: Determinar tipo de vaga automaticamente pelo gênero
        TipoVaga tipoVaga;
        if (genero != null && genero == 'F') {
            tipoVaga = TipoVaga.RESERVADA; // Mulheres concorrem a vagas reservadas
            log.debug("Linha {}: Candidata mulher -> Vaga RESERVADA", r.getRowNum());
        } else {
            tipoVaga = TipoVaga.AMPLA_CONCORRENCIA; // Homens concorrem a ampla concorrência
            log.debug("Linha {}: Candidato homem -> Vaga AMPLA CONCORRÊNCIA", r.getRowNum());
        }

        // resolve or create campus
        Campus campus = null;
        if (campusName != null && !campusName.isBlank()) {
            campus = campusRepository.findByNome(campusName);
            if (campus == null) {
                campus = new Campus();
                campus.setNome(campusName);
                // Define valores padrão para vagas: zero para forçar configuração manual
                campus.setNumeroVagasReservadas(0);
                campus.setNumeroVagasAmplaConcorrencia(0);
                campus.setVagasReservadasOcupadas(0);
                campus.setVagasAmplaOcupadas(0);
                campus = campusRepository.save(campus);
                log.info("Novo campus criado: {} com vagas padrão", campusName);
            }
        }

        Candidato c = new Candidato();
        c.setNome(nome);
        c.setCpf(cpf);
        c.setGenero(genero);
        c.setDataNascimento(dataNasc);
        c.setCampus(campus);
        c.setTurno(turno);
        c.setDataInscricao(dataInscricao);
        c.setHoraInscricao(horaInscricao);
        c.setTipoVaga(tipoVaga); // Definido automaticamente pelo gênero
        c.setSituacao(SituacaoCandidato.NAO_CLASSIFICADO); // Por padrão, candidato não está classificado

        log.debug("Candidato mapeado: {} - Gênero: {} - Campus: {} - Tipo Vaga: {}",
                nome, genero, campusName, tipoVaga);

        return c;
    }

    // helpers
    private String safeString(Cell cell) {
        if (cell == null)
            return "";
        try {
            DataFormatter fmt = new DataFormatter();
            return fmt.formatCellValue(cell);
        } catch (Exception e) {
            return cell.toString();
        }
    }

    private String getCellString(Row r, Integer idx) {
        if (idx == null)
            return null;
        Cell c = r.getCell(idx);
        if (c == null)
            return null;
        String s = safeString(c);
        return s != null ? s.trim() : null;
    }

    private LocalDate getCellLocalDate(Row r, Integer idx) {
        if (idx == null)
            return null;
        Cell c = r.getCell(idx);
        if (c == null)
            return null;

        System.out.println("=== DEBUG DATA NASCIMENTO ===");
        System.out.println("Tipo da célula: " + c.getCellType());

        try {
            // 1. Primeiro tenta como data numérica do Excel
            if (c.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(c)) {
                    java.util.Date date = c.getDateCellValue();
                    Instant instant = date.toInstant();
                    LocalDate result = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
                    System.out.println("Data numérica formatada: " + date + " -> " + result);
                    return result;
                } else {
                    // Pode ser um número serial do Excel (dias desde 1900-01-01)
                    double numericValue = c.getNumericCellValue();
                    java.util.Date date = DateUtil.getJavaDate(numericValue);
                    LocalDate result = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    System.out.println("Data numérica serial: " + numericValue + " -> " + result);
                    return result;
                }
            }

            // 2. Tenta como string
            String s = safeString(c);
            System.out.println("Valor string: '" + s + "'");

            if (s == null || s.isBlank())
                return null;

            // Remove espaços extras
            s = s.trim();

            // 3. Tenta formato ISO (yyyy-MM-dd)
            try {
                LocalDate result = LocalDate.parse(s);
                System.out.println("Parse ISO: " + result);
                return result;
            } catch (Exception e1) {
                // Ignora e continua para outros formatos
            }

            // 4. Tenta formatos brasileiros comuns
            String[] separators = { "\\.", "/", "-" };

            for (String sep : separators) {
                try {
                    String[] parts = s.split(sep);
                    if (parts.length == 3) {
                        int dia, mes, ano;

                        // Detecta formato (dd/MM/yyyy vs MM/dd/yyyy)
                        if (parts[0].length() <= 2 && parts[1].length() <= 2) {
                            dia = Integer.parseInt(parts[0].trim());
                            mes = Integer.parseInt(parts[1].trim());
                            ano = Integer.parseInt(parts[2].trim());

                            // Corrige ano com 2 dígitos
                            if (ano < 100) {
                                ano = ano + 2000; // Assume século 21 para anos com 2 dígitos
                            }

                            LocalDate result = LocalDate.of(ano, mes, dia);
                            System.out.println(
                                    "Parse formato BR " + sep + ": " + dia + "/" + mes + "/" + ano + " -> " + result);
                            return result;
                        }
                    }
                } catch (Exception e2) {
                    // Continua para o próximo separador
                }
            }

            // 5. Tenta formato americano (MM/dd/yyyy)
            try {
                String[] parts = s.split("[/-]");
                if (parts.length == 3) {
                    int mes = Integer.parseInt(parts[0].trim());
                    int dia = Integer.parseInt(parts[1].trim());
                    int ano = Integer.parseInt(parts[2].trim());

                    if (ano < 100) {
                        ano = ano + 2000;
                    }

                    // Valida se é uma data válida (mes entre 1-12, dia entre 1-31)
                    if (mes >= 1 && mes <= 12 && dia >= 1 && dia <= 31) {
                        LocalDate result = LocalDate.of(ano, mes, dia);
                        System.out.println("Parse formato US: " + mes + "/" + dia + "/" + ano + " -> " + result);
                        return result;
                    }
                }
            } catch (Exception e3) {
                // Ignora
            }

            System.out.println("Não foi possível parsear a data: '" + s + "'");

        } catch (Exception ex) {
            System.out.println("Erro ao processar data: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("=============================");
        return null;
    }

    /**
     * Cria automaticamente um registro de turno (CampusEditalTurno) se não existir.
     * Isso garante que quando um candidato é importado, a turno correspondente é
     * registrada.
     */
    /**
     * Guarantee that a CampusEdital exists and create the given turno if missing.
     * Also provide a helper to create the three default turnos (Manhã/Tarde/Noite)
     * when a CampusEdital is present.
     */
    private void criarTurnoSeNaoExistir(Campus campus, com.example.energif.model.Edital edital, String turno) {
        try {
            // Buscar CampusEdital
            CampusEdital ce = campusEditalRepository.findByCampusAndEdital(campus, edital);
            if (ce == null) {
                log.warn("CampusEdital não encontrado para campus={} edital={}", campus.getId(), edital.getId());
                return;
            }

            // Verificar se turno já existe
            CampusEditalTurno existing = turnoRepository.findByCampusEditalAndTurno(ce, turno);
            if (existing != null) {
                log.debug("Turno '{}' já existe para CampusEdital {}", turno, ce.getId());
                return;
            }

            // Criar nova turno com vagas padrão (0) - será configurado manualmente pelo
            // admin
            CampusEditalTurno newTurno = new CampusEditalTurno();
            newTurno.setCampusEdital(ce);
            newTurno.setTurno(turno);
            newTurno.setNumeroVagasReservadas(0);
            newTurno.setNumeroVagasAmplaConcorrencia(0);
            newTurno.setVagasReservadasOcupadas(0);
            newTurno.setVagasAmplaOcupadas(0);
            turnoRepository.save(newTurno);

            log.info("Nova turno criada automaticamente: '{}' para CampusEdital {}", turno, ce.getId());
        } catch (Exception e) {
            log.warn("Erro ao criar turno automaticamente: {}", e.getMessage());
        }
    }

    /**
     * Ensure a CampusEdital exists for campus+edital and create the three default
     * turnos (Manhã, Tarde, Noite) if they don't exist yet. This is used when a
     * campus is created automatically during import and we want to pre-provision
     * the three shifts so imports and later classifications operate per-turno.
     */
    private void criarTurnosPadraoSeNaoExistir(Campus campus, com.example.energif.model.Edital edital) {
        try {
            CampusEdital ce = campusEditalRepository.findByCampusAndEdital(campus, edital);
            if (ce == null) {
                // create the linking record if missing
                ce = new CampusEdital();
                ce.setCampus(campus);
                ce.setEdital(edital);
                // default vagas = 0 – administrative configuration required later
                ce.setNumeroVagasReservadas(0);
                ce.setNumeroVagasAmplaConcorrencia(0);
                campusEditalRepository.save(ce);
                log.info("CampusEdital criado automaticamente para campus={} edital={}", campus.getNome(),
                        edital.getDescricao());
            }

            String[] padrao = new String[] { "Manhã", "Tarde", "Noite" };
            for (String t : padrao) {
                CampusEditalTurno existing = turnoRepository.findByCampusEditalAndTurno(ce, t);
                if (existing == null) {
                    CampusEditalTurno newTurno = new CampusEditalTurno();
                    newTurno.setCampusEdital(ce);
                    newTurno.setTurno(t);
                    newTurno.setNumeroVagasReservadas(0);
                    newTurno.setNumeroVagasAmplaConcorrencia(0);
                    newTurno.setVagasReservadasOcupadas(0);
                    newTurno.setVagasAmplaOcupadas(0);
                    turnoRepository.save(newTurno);
                    log.info("Turno padrão criado: '{}' para CampusEdital {}", t, ce.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao criar turnos padrão para campus {} e edital {}: {}", campus.getNome(),
                    edital.getDescricao(), e.getMessage());
        }
    }

}
