package edu.nju.jap.service;

import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.AnnotationResult;
import edu.nju.jap.model.AnnotationSubmit;
import edu.nju.jap.model.Proposition;
import edu.nju.jap.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnnotationService {
    private final DemoDataStore store;

    public AnnotationService(DemoDataStore store) {
        this.store = store;
    }

    public void submit(AnnotationSubmit body, HttpServletRequest request) {
        User user = store.current(request);
        List<Proposition> propositions = new ArrayList<>(body.propositions() == null ? List.of() : body.propositions());
        propositions.sort(Comparator.comparingInt(Proposition::startPos));
        for (int i = 0; i < propositions.size(); i++) {
            Proposition p = propositions.get(i);
            propositions.set(i, new Proposition(p.propId(), i + 1, p.startPos(), p.endPos(), p.text(), p.tag()));
        }
        AnnotationResult result = new AnnotationResult(body.taskId(), body.dataId(), user.id, propositions,
                body.relations() == null ? List.of() : body.relations(), body.isDraft(), LocalDateTime.now());
        store.annotations.put(DemoDataStore.annotationKey(body.taskId(), body.dataId(), user.id), result);
    }
}
