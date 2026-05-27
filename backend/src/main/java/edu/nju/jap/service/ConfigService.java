package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.GuideConfig;
import edu.nju.jap.model.LabelDef;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {
    private final DemoDataStore store;

    public ConfigService(DemoDataStore store) {
        this.store = store;
    }

    public List<GuideConfig> list() {
        return store.configs.values().stream().sorted(Comparator.comparing(c -> c.id)).toList();
    }

    public GuideConfig active() {
        return store.configs.values().stream().filter(c -> c.active).findFirst().orElseThrow();
    }

    public long create(Map<String, Object> body) {
        long id = store.configSeq.incrementAndGet();
        GuideConfig base = store.configs.get(1L);
        GuideConfig config = new GuideConfig(id, MapBodyUtils.text(body, "versionName", "V" + id),
                MapBodyUtils.text(body, "description", "自定义指南版本"), false,
                java.time.LocalDate.now().toString(),
                labelDefs(body.get("primaryTags"), base.primaryTags),
                labelDefs(body.get("secondaryTags"), base.secondaryTags),
                labelDefs(body.get("relationTypes"), base.relationTypes));
        store.configs.put(id, config);
        return id;
    }

    public GuideConfig update(long id, Map<String, Object> body) {
        GuideConfig old = requireConfig(id);
        GuideConfig updated = new GuideConfig(id,
                MapBodyUtils.text(body, "versionName", old.versionName),
                MapBodyUtils.text(body, "description", old.description),
                old.active, old.createdAt,
                labelDefs(body.get("primaryTags"), old.primaryTags),
                labelDefs(body.get("secondaryTags"), old.secondaryTags),
                labelDefs(body.get("relationTypes"), old.relationTypes));
        store.configs.put(id, updated);
        return updated;
    }

    public void delete(long id) {
        if (id == 1L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "默认指南版本不可删除");
        }
        if (store.configs.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "指南版本不存在");
        }
    }

    public GuideConfig requireConfig(long id) {
        GuideConfig cfg = store.configs.get(id);
        if (cfg == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "指南版本不存在");
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    public static List<LabelDef> labelDefs(Object value, List<LabelDef> fallback) {
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> raw)) {
            return fallback;
        }
        if (raw.isEmpty()) {
            return List.of();
        }
        List<LabelDef> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map<?, ?> m) {
                String sn = m.get("shortName") == null ? "" : m.get("shortName").toString();
                String nm = m.get("name") == null ? "" : m.get("name").toString();
                String desc = m.get("description") == null ? "" : m.get("description").toString();
                String parent = m.get("parentTag") == null ? "" : m.get("parentTag").toString();
                result.add(new LabelDef(sn, nm, desc, parent));
            }
        }
        return result.isEmpty() ? fallback : result;
    }
}
