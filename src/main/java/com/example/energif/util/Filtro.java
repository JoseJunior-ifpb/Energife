package com.example.energif.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.energif.model.Candidato;
import com.example.energif.repository.CandidatoRepository;

@Component
public class Filtro {

    private static final Logger logger = LoggerFactory.getLogger(Filtro.class);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CandidatoRepository candidatoRepository;

    public Filtro(CandidatoRepository candidatoRepository) {
        this.candidatoRepository = candidatoRepository;
    }

    /**
     * Importa candidatos de um arquivo XLSX
     * 
     * @param arquivo        Arquivo Excel contendo dados dos candidatos
     * @param editalDescricao Descrição do edital associado (opcional)
     * @return Quantidade de candidatos importados com sucesso
     * @throws IOException se houver erro ao ler o arquivo
     */
    public int importarXlsx(File arquivo, String editalDescricao) throws IOException {
        int importados = 0;

        try (FileInputStream fis = new FileInputStream(arquivo);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int inicio = 1; // pula o cabeçalho na linha 0

            for (int i = inicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estaVazia(row)) {
                    continue;
                }

                try {
                    Candidato candidato = parseLinha(row);
                    if (candidato != null) {
                        candidatoRepository.save(candidato);
                        importados++;
                        logger.info("Candidato importado: {}", candidato.getNome());
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao processar linha {}: {}", i + 1, e.getMessage());
                }
            }

            logger.info("Importação concluída. Total de candidatos importados: {}", importados);

        } catch (IOException e) {
            logger.error("Erro ao ler arquivo Excel", e);
            throw e;
        }

        return importados;
    }

    /**
     * Converte uma linha do Excel em um objeto Candidato
     * Esperado: nome, cpf, dataNascimento
     */
    private Candidato parseLinha(Row row) {
        try {
            String nome = getCellValueAsString(row.getCell(0));
            String cpf = getCellValueAsString(row.getCell(1));
            String dataNascimentoStr = getCellValueAsString(row.getCell(2));

            if (nome == null || nome.trim().isEmpty() || cpf == null || cpf.trim().isEmpty()) {
                return null;
            }

            Candidato candidato = new Candidato();
            candidato.setNome(nome.trim());
            candidato.setCpf(cpf.trim());

            // Parse data de nascimento
            if (dataNascimentoStr != null && !dataNascimentoStr.trim().isEmpty()) {
                try {
                    LocalDate dataNascimento = LocalDate.parse(dataNascimentoStr.trim(), dateFormatter);
                    candidato.setDataNascimento(dataNascimento);
                } catch (Exception e) {
                    logger.warn("Data inválida para candidato {}: {}", nome, dataNascimentoStr);
                }
            }

            return candidato;

        } catch (Exception e) {
            logger.error("Erro ao parsear linha: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtém o valor de uma célula como String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType();

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    /**
     * Verifica se uma linha está vazia
     */
    private boolean estaVazia(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
