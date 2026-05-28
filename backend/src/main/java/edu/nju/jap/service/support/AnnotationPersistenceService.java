package edu.nju.jap.service.support;

import edu.nju.jap.mapper.AnnotationMapper;
import edu.nju.jap.mapper.PropositionMapper;
import edu.nju.jap.mapper.RelationMapper;
import edu.nju.jap.mapper.RelationMemberMapper;
import edu.nju.jap.model.entity.AnnotationResult;
import edu.nju.jap.model.entity.ArbitrationResult;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;
import edu.nju.jap.model.po.AnnotationPo;
import edu.nju.jap.model.po.PropositionPo;
import edu.nju.jap.model.po.RelationMember;
import edu.nju.jap.model.po.RelationPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnnotationPersistenceService {
    private final AnnotationMapper annotationMapper;
    private final PropositionMapper propositionMapper;
    private final RelationMapper relationMapper;
    private final RelationMemberMapper relationMemberMapper;

    public AnnotationPersistenceService(AnnotationMapper annotationMapper, PropositionMapper propositionMapper,
                                        RelationMapper relationMapper, RelationMemberMapper relationMemberMapper) {
        this.annotationMapper = annotationMapper;
        this.propositionMapper = propositionMapper;
        this.relationMapper = relationMapper;
        this.relationMemberMapper = relationMemberMapper;
    }

    @Transactional
    public void saveAnnotation(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                               List<Relation> relations, boolean draft) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, userId);
        LocalDateTime now = LocalDateTime.now();
        if (annotation == null) {
            annotation = new AnnotationPo();
            annotation.setTaskId((long) taskId);
            annotation.setDocumentId((long) taskDocumentId);
            annotation.setUserId(userId);
            annotation.setIsFinal(0);
            annotation.setStatus(draft ? "DRAFT" : "SUBMITTED");
            annotation.setSubmittedAt(draft ? null : now);
            annotationMapper.insert(annotation);
        } else {
            relationMemberMapper.deleteByAnnotationId(annotation.getId());
            relationMapper.deleteByAnnotationId(annotation.getId());
            propositionMapper.deleteByAnnotationId(annotation.getId());
            annotation.setIsFinal(0);
            annotation.setStatus(draft ? "DRAFT" : "SUBMITTED");
            annotation.setSubmittedAt(draft ? null : now);
            annotationMapper.updateStatus(annotation);
        }

        Map<String, Long> propIds = new LinkedHashMap<>();
        if (propositions != null) {
            int seq = 1;
            for (Proposition p : propositions) {
                PropositionPo po = new PropositionPo();
                po.setAnnotationId(annotation.getId());
                po.setDisplayId(p.propId());
                po.setSequenceNo(seq++);
                po.setStartPos(p.startPos());
                po.setEndPos(p.endPos());
                po.setSelectedText(p.text());
                DomainConverter.splitTag(p.tag(), po);
                propositionMapper.insert(po);
                propIds.put(po.getDisplayId(), po.getId());
            }
        }

        Map<String, Long> relationIds = new LinkedHashMap<>();
        if (relations != null) {
            int seq = 1;
            for (Relation r : relations) {
                RelationPo po = new RelationPo();
                po.setAnnotationId(annotation.getId());
                po.setDisplayId(r.relId());
                po.setSequenceNo(seq++);
                po.setRelationType(r.type());
                po.setExpression(buildExpression(r));
                relationMapper.insert(po);
                relationIds.put(po.getDisplayId(), po.getId());
            }

            for (Relation r : relations) {
                Long relationId = relationIds.get(r.relId());
                if (relationId == null) {
                    continue;
                }
                List<String> members = relationMembers(r);
                for (int i = 0; i < members.size(); i++) {
                    RelationMember member = new RelationMember();
                    member.setRelationId(relationId);
                    member.setMemberRole(memberRole(r.type(), i));
                    member.setMemberOrder(i + 1);
                    fillMemberReference(member, members.get(i), propIds, relationIds);
                    relationMemberMapper.insert(member);
                }
            }
        }
    }

    public AnnotationResult loadAnnotation(int taskId, int taskDocumentId, long userId, long apiDataId) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, userId);
        if (annotation == null) {
            return new AnnotationResult(taskId, apiDataId, userId, List.of(), List.of(), true, null);
        }

        List<PropositionPo> props = propositionMapper.selectByAnnotationId(annotation.getId());
        List<RelationPo> rels = relationMapper.selectByAnnotationId(annotation.getId());
        List<Proposition> propositions = new ArrayList<>();
        Map<Long, String> propDisplayIds = new LinkedHashMap<>();
        int seq = 1;
        for (PropositionPo po : props) {
            propositions.add(DomainConverter.toProposition(po, seq++));
            propDisplayIds.put(po.getId(), po.getDisplayId());
        }

        Map<Long, String> relationDisplayIds = new LinkedHashMap<>();
        for (RelationPo po : rels) {
            relationDisplayIds.put(po.getId(), po.getDisplayId());
        }

        List<Relation> relations = new ArrayList<>();
        for (RelationPo po : rels) {
            List<String> memberIds = relationMemberMapper.selectByRelationId(po.getId()).stream()
                    .map(m -> displayMember(m, propDisplayIds, relationDisplayIds))
                    .filter(s -> s != null && !s.isBlank())
                    .toList();
            String source = memberIds.isEmpty() ? "" : memberIds.get(0);
            String target = memberIds.size() < 2 ? "" : memberIds.get(1);
            relations.add(new Relation(po.getDisplayId(), po.getRelationType(), source, target, memberIds));
        }

        boolean draft = !"SUBMITTED".equalsIgnoreCase(annotation.getStatus());
        LocalDateTime submitted = annotation.getSubmittedAt() != null
                ? annotation.getSubmittedAt()
                : props.stream()
                        .map(PropositionPo::getUpdatedAt)
                        .filter(t -> t != null)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
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

    private static List<String> relationMembers(Relation relation) {
        if (relation.members() != null && !relation.members().isEmpty()) {
            return relation.members().stream().filter(s -> s != null && !s.isBlank()).toList();
        }
        List<String> fallback = new ArrayList<>();
        if (relation.source() != null && !relation.source().isBlank()) {
            fallback.add(relation.source());
        }
        if (relation.target() != null && !relation.target().isBlank()) {
            fallback.add(relation.target());
        }
        return fallback;
    }

    private static String buildExpression(Relation relation) {
        return relation.type() + "(" + String.join(", ", relationMembers(relation)) + ")";
    }

    private static String memberRole(String type, int index) {
        if ("J".equals(type) || "I".equals(type)) {
            return "MEMBER";
        }
        return index == 0 ? "SOURCE" : "TARGET";
    }

    private static void fillMemberReference(RelationMember member, String displayId, Map<String, Long> propIds,
                                            Map<String, Long> relationIds) {
        if (displayId.startsWith("R")) {
            Long childRelationId = relationIds.get(displayId);
            if (childRelationId == null) {
                throw new IllegalArgumentException("关系成员不存在: " + displayId);
            }
            member.setMemberType("R");
            member.setChildRelationId(childRelationId);
            return;
        }
        Long propositionId = propIds.get(displayId);
        if (propositionId == null) {
            throw new IllegalArgumentException("命题成员不存在: " + displayId);
        }
        member.setMemberType("P");
        member.setPropositionId(propositionId);
    }

    private static String displayMember(RelationMember member, Map<Long, String> propDisplayIds,
                                        Map<Long, String> relationDisplayIds) {
        if ("R".equals(member.getMemberType())) {
            return relationDisplayIds.get(member.getChildRelationId());
        }
        return propDisplayIds.get(member.getPropositionId());
    }
}
