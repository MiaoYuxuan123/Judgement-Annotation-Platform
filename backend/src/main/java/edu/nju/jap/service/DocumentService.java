package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.entity.DocumentItem;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.util.DocumentTextExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class DocumentService {
    private final DemoDataStore store;

    public DocumentService(DemoDataStore store) {
        this.store = store;
    }

    public Map<String, Object> list(String keyword) {
        List<DocumentItem> list = store.documents.values().stream()
                .filter(d -> keyword == null || d.title.contains(keyword) || d.documentId.contains(keyword))
                .sorted(Comparator.comparing(d -> d.id))
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    public DocumentItem getById(long id) {
        DocumentItem doc = store.documents.get(id);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文书不存在");
        }
        return doc;
    }

    public void delete(long id) {
        if (store.documents.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文书不存在");
        }
    }

    public long create(Map<String, Object> body, HttpServletRequest request) {
        long id = store.docSeq.incrementAndGet();
        User user = store.current(request);
        DocumentItem doc = new DocumentItem(id, "W" + id, MapBodyUtils.text(body, "title", "新文书" + id),
                MapBodyUtils.text(body, "type", "民事判决书"), LocalDate.now().toString(),
                MapBodyUtils.text(body, "content", "本院认为，案涉事实清楚，证据充分。"), user.id);
        store.documents.put(id, doc);
        return id;
    }

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
                previews.add(Map.of("filename", filename, "title", parsed.title(), "type", parsed.type(),
                        "content", parsed.content(), "contentLength", parsed.content().length()));
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
