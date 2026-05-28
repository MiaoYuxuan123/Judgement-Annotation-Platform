package edu.nju.jap.service.support;

import edu.nju.jap.mapper.GlobalDocumentMapper;
import edu.nju.jap.mapper.TaskDocumentMapper;
import edu.nju.jap.model.entity.DocumentItem;
import edu.nju.jap.model.po.GlobalDocument;
import edu.nju.jap.model.po.TaskDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TaskDocumentResolver {
    private final TaskDocumentMapper taskDocumentMapper;
    private final GlobalDocumentMapper globalDocumentMapper;

    public TaskDocumentResolver(TaskDocumentMapper taskDocumentMapper, GlobalDocumentMapper globalDocumentMapper) {
        this.taskDocumentMapper = taskDocumentMapper;
        this.globalDocumentMapper = globalDocumentMapper;
    }

    public TaskDocument requireTaskDocument(int taskId, long dataId) {
        TaskDocument td = taskDocumentMapper.selectByTaskAndDataId(taskId, dataId);
        if (td == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务文书不存在");
        }
        return td;
    }

    public DocumentItem toDocumentItem(TaskDocument td) {
        GlobalDocument global = td.getGlobalDocId() == null ? null : globalDocumentMapper.selectById(td.getGlobalDocId());
        return DomainConverter.toDocumentItem(td, global);
    }

    public long apiDataId(TaskDocument td) {
        return td.getGlobalDocId() != null ? td.getGlobalDocId() : td.getId();
    }
}
