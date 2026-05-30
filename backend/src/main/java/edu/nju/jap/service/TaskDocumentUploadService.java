package edu.nju.jap.service;

import edu.nju.jap.util.DocumentTextExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskDocumentUploadService {

    public Map<String, Object> upload(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择至少一个文件");
        }
        List<Map<String, Object>> previews = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
            try {
                DocumentTextExtractor.ParsedDocument parsed = DocumentTextExtractor.parse(file);
                previews.add(Map.of(
                        "sourceType", "UPLOAD",
                        "fileName", filename,
                        "title", parsed.title(),
                        "type", parsed.type(),
                        "extractedText", parsed.content(),
                        "contentLength", parsed.content().length()
                ));
            } catch (Exception ex) {
                errors.add(filename + ": " + ex.getMessage());
            }
        }
        if (previews.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    errors.isEmpty() ? "未收到有效文件" : String.join("; ", errors));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", previews.size());
        payload.put("list", previews);
        if (!errors.isEmpty()) {
            payload.put("errors", errors);
        }
        return payload;
    }
}
