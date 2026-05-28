package edu.nju.jap.service.support;

import edu.nju.jap.mapper.PropositionMapper;
import edu.nju.jap.mapper.RelationMapper;
import edu.nju.jap.mapper.RelationMemberMapper;
import edu.nju.jap.model.entity.AnnotationResult;
import edu.nju.jap.model.entity.ArbitrationResult;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;
import edu.nju.jap.model.po.PropositionPo;
import edu.nju.jap.model.po.RelationMember;
import edu.nju.jap.model.po.RelationPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnnotationPersistenceService {
    private final PropositionMapper propositionMapper;
    private final RelationMapper relationMapper;
    private final RelationMemberMapper relationMemberMapper;

    public AnnotationPersistenceService(PropositionMapper propositionMapper, RelationMapper relationMapper,
                                          RelationMemberMapper relationMemberMapper) {
        this.propositionMapper = propositionMapper;
        this.relationMapper = relationMapper;
        this.relationMemberMapper = relationMemberMapper;
    }

    @Transactional
    public void saveAnnotation(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                               List<Relation> relations, boolean draft) {
        propositionMapper.deleteByScope(taskId, taskDocumentId, userId);
        relationMapper.deleteByScope(taskId, taskDocumentId, userId);

        int status = draft ? 0 : 1;
        if (propositions != null) {
            int seq = 1;
            for (Proposition p : propositions) {
                PropositionPo po = new PropositionPo();
                po.setTaskId(taskId);
                po.setTaskDocumentId(taskDocumentId);
                po.setUserId(userId);
                po.setDisplayId(p.propId());
                po.setContent(p.text());
                po.setStartOffset(p.startPos());
                po.setEndOffset(p.endPos());
                DomainConverter.splitTag(p.tag(), po);
                po.setStatus(status);
                propositionMapper.insert(po);
                seq++;
            }
        }
        if (relations != null) {
            for (Relation r : relations) {
                RelationPo po = new RelationPo();
                po.setTaskId(taskId);
                po.setTaskDocumentId(taskDocumentId);
                po.setUserId(userId);
                po.setDisplayId(r.relId());
                po.setType(r.type());
                po.setTargetType("P");
                po.setTargetId(r.target());
                po.setExpression(r.type() + "(" + r.source() + "," + r.target() + ")");
                po.setStatus(status);
                relationMapper.insert(po);
                RelationMember member = new RelationMember();
                member.setRelationId(po.getId());
                member.setSourceType("P");
                member.setSourceId(r.source());
                relationMemberMapper.insert(member);
            }
        }
    }

    public AnnotationResult loadAnnotation(int taskId, int taskDocumentId, long userId, long apiDataId) {
        List<PropositionPo> props = propositionMapper.selectByScope(taskId, taskDocumentId, userId);
        List<RelationPo> rels = relationMapper.selectByScope(taskId, taskDocumentId, userId);
        List<Proposition> propositions = new ArrayList<>();
        int seq = 1;
        for (PropositionPo po : props) {
            propositions.add(DomainConverter.toProposition(po, seq++));
        }
        List<Relation> relations = new ArrayList<>();
        for (RelationPo po : rels) {
            List<RelationMember> members = relationMemberMapper.selectByRelationId(po.getId());
            relations.add(DomainConverter.toRelation(po, members));
        }
        boolean draft = props.stream().anyMatch(p -> p.getStatus() != null && p.getStatus() == 0);
        LocalDateTime submitted = props.stream()
                .map(PropositionPo::getUpdateTime)
                .filter(t -> t != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (propositions.isEmpty() && relations.isEmpty()) {
            return new AnnotationResult(taskId, apiDataId, userId, List.of(), List.of(), true, null);
        }
        return new AnnotationResult(taskId, apiDataId, userId, propositions, relations, draft, submitted);
    }

    public ArbitrationResult loadArbitration(int taskId, int taskDocumentId, long apiDataId, long arbitratorId,
                                             edu.nju.jap.model.po.ArbitrationSnapshot snapshot) {
        AnnotationResult base = loadAnnotation(taskId, taskDocumentId, arbitratorId, apiDataId);
        ArbitrationResult result = new ArbitrationResult(taskId, apiDataId, arbitratorId, base.propositions,
                base.relations, snapshot.getAdoptedFrom(), snapshot.getArbitratedAt());
        result.finalResult = snapshot.getFinalResult() != null && snapshot.getFinalResult() == 1;
        return result;
    }

}
