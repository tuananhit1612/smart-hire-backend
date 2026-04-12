package com.smarthire.backend.infrastructure.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

@Service
@Slf4j
public class CvTextExtractor {

    public String extractText(Path filePath, String mimeType) {
        log.info("📄 Extracting text from file: {} (mime: {})", filePath, mimeType);
        
        try {
            File file = filePath.toFile();
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + filePath);
            }

            if (mimeType.contains("pdf")) {
                return extractPdfText(file);
            } else if (mimeType.contains("wordprocessingml") || mimeType.contains("msword") || filePath.toString().endsWith(".docx")) {
                return extractDocxText(file);
            } else {
                log.warn("Unsupported mime type for text extraction: {}, treating as plain text fallback", mimeType);
                return "Unsupported file format.";
            }

        } catch (Exception e) {
            log.error("Failed to extract text from file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Failed to extract text from CV file", e);
        }
    }

    private String extractPdfText(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        }
    }

    private String extractDocxText(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            return text != null ? text.trim() : "";
        }
    }
}
