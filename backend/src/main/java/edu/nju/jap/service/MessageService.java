package edu.nju.jap.service;

import edu.nju.jap.mapper.MessageMapper;
import edu.nju.jap.model.entity.Message;
import edu.nju.jap.service.support.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageService {
    private final MessageMapper messageMapper;
    private final CurrentUserService currentUserService;

    public MessageService(MessageMapper messageMapper, CurrentUserService currentUserService) {
        this.messageMapper = messageMapper;
        this.currentUserService = currentUserService;
    }

    /** Admin returns empty list */
    public Map<String, Object> list(HttpServletRequest request) {
        var user = currentUserService.requireCurrent(request);
        if ("admin".equals(user.role)) return Map.of("total", 0, "list", List.of());
        List<Message> list = messageMapper.selectByUserId(user.id);
        return Map.of("total", list.size(), "list", list);
    }

    public Map<String, Object> unreadCount(HttpServletRequest request) {
        var user = currentUserService.requireCurrent(request);
        if ("admin".equals(user.role)) return Map.of("count", 0);
        return Map.of("count", messageMapper.countUnread(user.id));
    }

    public void markRead(long id, HttpServletRequest request) {
        var user = currentUserService.requireCurrent(request);
        messageMapper.markRead(id, user.id);
    }

    public void markAllRead(HttpServletRequest request) {
        var user = currentUserService.requireCurrent(request);
        messageMapper.markAllRead(user.id);
    }

    public void deleteRead(HttpServletRequest request) {
        var user = currentUserService.requireCurrent(request);
        messageMapper.deleteRead(user.id);
    }

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    /** Convenience method: create a single message. Failures are logged but never propagated. */
    public void send(long userId, String type, String title, String content, Integer taskId, Integer taskDocumentId, Integer dataId) {
        try {
            Message msg = new Message(userId, type, title, content, taskId, taskDocumentId, dataId);
            messageMapper.insert(msg);
        } catch (Exception e) {
            log.error("Failed to send message: type={}, userId={}", type, userId, e);
        }
    }
}
