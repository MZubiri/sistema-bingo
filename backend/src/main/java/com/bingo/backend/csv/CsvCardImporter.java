package com.bingo.backend.csv;

import com.bingo.backend.card.BingoCard;
import com.bingo.backend.card.BingoCardRepository;
import com.bingo.backend.card.CardStatus;
import com.bingo.backend.common.HashService;
import com.bingo.backend.common.OrganizationCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CsvCardImporter implements CommandLineRunner {
    private static final List<String> EXPECTED_HEADERS = List.of(
            "serial", "B1", "I1", "N1", "G1", "O1",
            "B2", "I2", "N2", "G2", "O2",
            "B3", "I3", "N3", "G3", "O3",
            "B4", "I4", "N4", "G4", "O4",
            "B5", "I5", "N5", "G5", "O5",
            "firma"
    );

    private final BingoCardRepository bingoCardRepository;
    private final ObjectMapper objectMapper;
    private final HashService hashService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        importCards();
    }

    private void importCards() throws IOException {
        ClassPathResource resource = new ClassPathResource("cartones_generados.csv");
        if (!resource.exists()) {
            throw new IllegalStateException("No se encontro cartones_generados.csv en resources");
        }

        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            validateHeader(header);

            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                String[] columns = line.split(",", -1);
                if (columns.length != EXPECTED_HEADERS.size()) {
                    throw new IllegalStateException("Fila CSV " + row + " tiene " + columns.length + " columnas");
                }

                String serial = columns[0].trim();
                String firma = columns[26].trim();
                if (!serial.matches("\\d{4}")) {
                    throw new IllegalStateException("Serial invalido en fila " + row + ": " + serial);
                }
                if (!"LIBRE".equals(columns[13])) {
                    throw new IllegalStateException("La casilla N3 debe ser LIBRE en fila " + row);
                }
                if (bingoCardRepository.existsBySerial(serial)) {
                    continue;
                }
                if (bingoCardRepository.existsByPositionalSignature(firma)) {
                    throw new IllegalStateException("Firma duplicada en CSV: " + firma);
                }

                BingoCard card = new BingoCard();
                card.setSerial(serial);
                card.setNumbersJson(toNumbersJson(columns));
                card.setPositionalSignature(firma);
                card.setNumbersSignature(hashService.sha256(normalizedNumbers(columns)));
                card.setOrganizationCode(OrganizationCode.fromSerial(serial));
                card.setStatus(CardStatus.AVAILABLE);
                bingoCardRepository.save(card);
                imported++;
            }
        }
        if (imported > 0) {
            System.out.println("Cartones importados desde CSV: " + imported);
        }
    }

    private void validateHeader(String header) {
        if (header == null) {
            throw new IllegalStateException("El CSV esta vacio");
        }
        List<String> headers = Arrays.stream(header.split(",", -1)).map(String::trim).toList();
        if (!headers.equals(EXPECTED_HEADERS)) {
            throw new IllegalStateException("Cabecera CSV inesperada: " + header);
        }
    }

    private String toNumbersJson(String[] columns) throws JsonProcessingException {
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            List<String> values = new ArrayList<>();
            for (int col = 0; col < 5; col++) {
                values.add(columns[1 + row * 5 + col]);
            }
            rows.add(values);
        }
        return objectMapper.writeValueAsString(rows);
    }

    private String normalizedNumbers(String[] columns) {
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            if (!"LIBRE".equals(columns[i])) {
                values.add(columns[i]);
            }
        }
        values.sort(String::compareTo);
        return String.join("|", values);
    }
}
