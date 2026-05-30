package edu.nju.jap.service.support;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.GlobalDocumentMapper;
import edu.nju.jap.model.po.GlobalDocument;
import edu.nju.jap.model.po.TaskDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class TaskDocumentFactory {
    private final GlobalDocumentMapper globalDocumentMapper;
    private final TaskDocumentStorage taskDocumentStorage;

    public TaskDocumentFactory(GlobalDocumentMapper globalDocumentMapper, TaskDocumentStorage taskDocumentStorage) {
        this.globalDocumentMapper = globalDocumentMapper;
        this.taskDocumentStorage = taskDocumentStorage;
    }

    public TaskDocument buildForCreate(int taskId, Map<String, Object> spec, long creatorId) {
        String sourceType = MapBodyUtils.text(spec, "sourceType", "GLOBAL").toUpperCase();
        TaskDocument td = new TaskDocument();
        td.setTaskId(taskId);
        td.setSourceType(sourceType);
        td.setStatus("待标注");

        return switch (sourceType) {
            case "GLOBAL" -> buildGlobal(td, spec);
            case "UPLOAD" -> buildUpload(td, spec, creatorId);
            case "RECREATE" -> buildRecreate(td, spec, creatorId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的文书来源类型: " + sourceType);
        };
    }

    private TaskDocument buildGlobal(TaskDocument td, Map<String, Object> spec) {
        long globalDocId = MapBodyUtils.longValue(spec.get("globalDocId"), 0);
        if (globalDocId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GLOBAL 文书必须指定 globalDocId");
        }
        GlobalDocument global = requireGlobal(globalDocId);
        td.setGlobalDocId(globalDocId);
        td.setFileName(global.getFileName());
        td.setFilePath(null);
        td.setExtractedText(null);
        return td;
    }

    private TaskDocument buildUpload(TaskDocument td, Map<String, Object> spec, long creatorId) {
        td.setGlobalDocId(null);
        String fileName = MapBodyUtils.text(spec, "fileName", MapBodyUtils.text(spec, "title", "未命名文书.txt"));
        if (!fileName.contains(".")) {
            fileName = fileName + ".txt";
        }
        String extractedText = MapBodyUtils.text(spec, "extractedText", MapBodyUtils.text(spec, "content", ""));
        td.setFileName(fileName);
        td.setFilePath(taskDocumentStorage.saveText(creatorId, fileName, extractedText));
        td.setExtractedText(extractedText);
        return td;
    }

    private TaskDocument buildRecreate(TaskDocument td, Map<String, Object> spec, long creatorId) {
        long globalDocId = MapBodyUtils.longValue(spec.get("globalDocId"), 0);
        if (globalDocId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECREATE 文书必须指定原文书 globalDocId");
        }
        GlobalDocument global = requireGlobal(globalDocId);
        String extractedText = MapBodyUtils.text(spec, "extractedText", MapBodyUtils.text(spec, "content", ""));
        String fileName = MapBodyUtils.text(spec, "fileName", global.getFileName());
        td.setGlobalDocId(globalDocId);
        td.setFileName(fileName);
        td.setFilePath(taskDocumentStorage.saveText(creatorId, fileName, extractedText));
        td.setExtractedText(extractedText);
        return td;
    }

    private GlobalDocument requireGlobal(long id) {
        GlobalDocument global = globalDocumentMapper.selectById(id);
        if (global == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文书总库中不存在 ID=" + id + " 的文书");
        }
        return global;
    }
}
