package edu.nju.jap.service.support;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class TaskDocumentStorage {
    private final Path baseDir = Path.of("data", "task-documents");

    public String saveText(long creatorId, String fileName, String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文书正文不能为空");
        }
        String safe = sanitizeFileName(fileName);
        try {
            Path dir = baseDir.resolve(String.valueOf(creatorId));
            Files.createDirectories(dir);
            String unique = UUID.randomUUID() + "_" + safe;
            Path file = dir.resolve(unique);
            Files.writeString(file, text, StandardCharsets.UTF_8);
            return "task-documents/" + creatorId + "/" + unique;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文书文件保存失败");
        }
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of("data").resolve(relativePath));
        } catch (IOException ignored) {
            // Best-effort cleanup; DB rows are removed regardless.
        }
    }

    private static String sanitizeFileName(String fileName) {
        String name = fileName == null || fileName.isBlank() ? "document.txt" : fileName.trim();
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
