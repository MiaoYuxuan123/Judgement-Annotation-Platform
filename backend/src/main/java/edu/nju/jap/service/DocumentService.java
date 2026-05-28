package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.GlobalDocumentMapper;
import edu.nju.jap.model.entity.DocumentItem;
import edu.nju.jap.model.po.GlobalDocument;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.DomainConverter;
import edu.nju.jap.util.DocumentTextExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class DocumentService {
    private final GlobalDocumentMapper globalDocumentMapper;
    private final CurrentUserService currentUserService;

    public DocumentService(GlobalDocumentMapper globalDocumentMapper, CurrentUserService currentUserService) {
        this.globalDocumentMapper = globalDocumentMapper;
        this.currentUserService = currentUserService;
    }

    public Map<String, Object> list(String keyword) {
        List<DocumentItem> list = globalDocumentMapper.selectAll(keyword).stream()
                .map(DomainConverter::toDocumentItem)
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    public DocumentItem getById(long id) {
        GlobalDocument doc = globalDocumentMapper.selectById(id);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文书不存在");
        }
        return DomainConverter.toDocumentItem(doc);
    }

    public void delete(long id) {
        if (globalDocumentMapper.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文书不存在");
        }
    }

    public long create(Map<String, Object> body, HttpServletRequest request) {
        long userId = currentUserService.requireCurrent(request).id;
        GlobalDocument doc = new GlobalDocument();
        doc.setDocumentId("W" + System.currentTimeMillis());
        doc.setTitle(MapBodyUtils.text(body, "title", "新文书"));
        doc.setFileName(doc.getTitle() + ".txt");
        doc.setFilePath("");
        doc.setFileType(MapBodyUtils.text(body, "type", "民事判决书"));
        doc.setExtractedText(MapBodyUtils.text(body, "content", "本院认为，案涉事实清楚，证据充分。"));
        doc.setUploadedById(userId);
        globalDocumentMapper.insert(doc);
        return doc.getId();
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
