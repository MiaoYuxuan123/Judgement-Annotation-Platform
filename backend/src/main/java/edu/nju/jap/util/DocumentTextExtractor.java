package edu.nju.jap.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 从上传文件中提取全部纯文本，不做段落截取或内容改写。
 * 支持格式：TXT、PDF、DOCX。旧版 .doc 格式不支持，请转换为 .docx 后上传。
 */
public final class DocumentTextExtractor {

    private DocumentTextExtractor() {
    }

    public record ParsedDocument(String title, String type, String content) {
    }

    public static ParsedDocument parse(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("文件名为空");
        }
        String ext = extension(filename);
        String content = switch (ext) {
            case "txt" -> readText(file);
            case "pdf" -> readPdf(file);
            case "docx" -> readDocx(file);
            case "doc" -> throw new IllegalArgumentException(
                    "不支持旧版 .doc 格式，请用 Word 将文件另存为 .docx 后上传");
            default -> throw new IllegalArgumentException(
                    "不支持的文件类型 ." + ext + "，请上传 PDF、Word(.docx) 或 TXT");
        };
        if (content.isBlank()) {
            throw new IllegalArgumentException("未能提取到文本，可能是扫描件 PDF 或空文件");
        }
        String title = stripExtension(filename);
        return new ParsedDocument(title, inferType(filename, content), content);
    }

    private static String readText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8.contains("\uFFFD")) {
            return preserveLineEndings(utf8);
        }
        return preserveLineEndings(new String(bytes, Charset.forName("GBK")));
    }

    private static String readPdf(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); PDDocument document = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setShouldSeparateByBeads(true);
            return preserveLineEndings(stripper.getText(document));
        }
    }

    private static String readDocx(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return preserveLineEndings(extractor.getText());
        }
    }

    private static String preserveLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String inferType(String filename, String text) {
        String probe = filename + " " + text.substring(0, Math.min(800, text.length()));
        if (probe.contains("裁定书")) {
            return "民事裁定书";
        }
        if (probe.contains("判决书")) {
            return "民事判决书";
        }
        return "上传文件";
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public static List<ParsedDocument> parseZip(MultipartFile zipFile) throws IOException {
        List<ParsedDocument> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        byte[] zipBytes = zipFile.getBytes();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                String ext = extension(name);
                if (!ext.equals("txt") && !ext.equals("pdf") && !ext.equals("docx")) continue;
                try {
                    byte[] entryBytes = zis.readAllBytes();
                    String content = switch (ext) {
                        case "txt" -> parseTextBytes(entryBytes);
                        case "pdf" -> parsePdfBytes(entryBytes);
                        case "docx" -> parseDocxBytes(entryBytes);
                        default -> throw new IllegalArgumentException("不支持的文件类型");
                    };
                    if (content.isBlank()) {
                        errors.add(name + ": 未能提取文本");
                        continue;
                    }
                    String title = stripExtension(new java.io.File(name).getName());
                    results.add(new ParsedDocument(title, inferType(name, content), content));
                } catch (Exception ex) {
                    errors.add(name + ": " + ex.getMessage());
                }
            }
        }
        return results;
    }

    private static String parseTextBytes(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8.contains("\uFFFD")) return preserveLineEndings(utf8);
        return preserveLineEndings(new String(bytes, Charset.forName("GBK")));
    }

    private static String parsePdfBytes(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return preserveLineEndings(stripper.getText(document));
        }
    }

    private static String parseDocxBytes(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return preserveLineEndings(extractor.getText());
        }
    }
}
