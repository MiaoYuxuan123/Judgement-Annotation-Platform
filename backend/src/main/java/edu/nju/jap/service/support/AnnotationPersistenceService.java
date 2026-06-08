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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnnotationPersistenceService {
    public static final String RECORD_ANNOTATION = AnnotationMapper.RECORD_ANNOTATION;
    public static final String RECORD_ARBITRATION = AnnotationMapper.RECORD_ARBITRATION;

    private final AnnotationMapper annotationMapper;
    private final PropositionMapper propositionMapper;
    private final RelationMapper relationMapper;
    private final RelationMemberMapper relationMemberMapper;
    private final GraphLayoutJsonCodec graphLayoutJsonCodec;

    public AnnotationPersistenceService(AnnotationMapper annotationMapper, PropositionMapper propositionMapper,
                                        RelationMapper relationMapper, RelationMemberMapper relationMemberMapper,
                                        GraphLayoutJsonCodec graphLayoutJsonCodec) {
        this.annotationMapper = annotationMapper;
        this.propositionMapper = propositionMapper;
        this.relationMapper = relationMapper;
        this.relationMemberMapper = relationMemberMapper;
        this.graphLayoutJsonCodec = graphLayoutJsonCodec;
    }

    @Transactional
    public void saveAnnotation(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                               List<Relation> relations, boolean draft) {
        saveAnnotation(taskId, taskDocumentId, userId, propositions, relations, draft, null);
    }

    @Transactional
    public void saveAnnotation(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                               List<Relation> relations, boolean draft, Object graphLayout) {
        saveRecord(taskId, taskDocumentId, userId, propositions, relations, draft, RECORD_ANNOTATION, graphLayout);
    }

    @Transactional
    public void saveArbitration(int taskId, int taskDocumentId, long arbitratorId, List<Proposition> propositions,
                                List<Relation> relations, boolean draft) {
        saveArbitration(taskId, taskDocumentId, arbitratorId, propositions, relations, draft, null);
    }

    @Transactional
    public void saveArbitration(int taskId, int taskDocumentId, long arbitratorId, List<Proposition> propositions,
                                List<Relation> relations, boolean draft, Object graphLayout) {
        saveRecord(taskId, taskDocumentId, arbitratorId, propositions, relations, draft, RECORD_ARBITRATION, graphLayout);
    }

    @Transactional
    public void saveGraphLayout(int taskId, int taskDocumentId, long userId, Object graphLayout, String recordType) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, userId, recordType);
        if (annotation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "标注结果不存在，请先保存标注内容");
        }
        annotationMapper.updateLayout(annotation.getId(), graphLayoutJsonCodec.toJson(graphLayout));
    }

    @Transactional
    public void deleteArbitration(int taskId, int taskDocumentId, long arbitratorId) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, arbitratorId, RECORD_ARBITRATION);
        if (annotation == null) {
            return;
        }
        clearAnnotationContent(annotation.getId());
        annotationMapper.deleteById(annotation.getId());
    }

    @Transactional
    public void rejectAnnotation(int taskId, int taskDocumentId, long annotatorId, String reason) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, annotatorId, RECORD_ANNOTATION);
        if (annotation == null || !"SUBMITTED".equalsIgnoreCase(annotation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "标注结果不存在或未提交");
        }
        annotation.setStatus("DRAFT");
        annotation.setSubmittedAt(null);
        annotation.setRejectReason(reason);
        annotationMapper.updateStatus(annotation);
    }

    @Transactional
    public void markArbitrationSubmitted(int taskId, int taskDocumentId, long arbitratorId) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, arbitratorId, RECORD_ARBITRATION);
        if (annotation == null) {
            return;
        }
        annotation.setStatus("SUBMITTED");
        annotation.setSubmittedAt(LocalDateTime.now());
        annotationMapper.updateStatus(annotation);
    }

    @Transactional
    public void saveRecord(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                           List<Relation> relations, boolean draft, String recordType) {
        saveRecord(taskId, taskDocumentId, userId, propositions, relations, draft, recordType, null);
    }

    @Transactional
    public void saveRecord(int taskId, int taskDocumentId, long userId, List<Proposition> propositions,
                           List<Relation> relations, boolean draft, String recordType, Object graphLayout) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, userId, recordType);
        LocalDateTime now = LocalDateTime.now();
        String layoutJson = graphLayoutJsonCodec.toJson(graphLayout);
        if (annotation != null && layoutJson != null) {
            Object incoming = graphLayoutJsonCodec.fromJson(layoutJson);
            Object existing = graphLayoutJsonCodec.fromJson(annotation.getLayoutJson());
            if (!isRichLayout(incoming) && isRichLayout(existing)) {
                layoutJson = null;
            }
        }
        if (annotation == null) {
            annotation = new AnnotationPo();
            annotation.setTaskId((long) taskId);
            annotation.setDocumentId((long) taskDocumentId);
            annotation.setUserId(userId);
            annotation.setRecordType(recordType);
            annotation.setIsFinal(0);
            annotation.setStatus(draft ? "DRAFT" : "SUBMITTED");
            annotation.setSubmittedAt(draft ? null : now);
            annotation.setRejectReason(null);
            annotation.setLayoutJson(layoutJson);
            annotationMapper.insert(annotation);
            if (layoutJson != null) {
                annotationMapper.updateLayout(annotation.getId(), layoutJson);
            }
        } else {
            clearAnnotationContent(annotation.getId());
            annotation.setIsFinal(0);
            annotation.setStatus(draft ? "DRAFT" : "SUBMITTED");
            annotation.setSubmittedAt(draft ? null : now);
            annotation.setRejectReason(draft ? annotation.getRejectReason() : null);
            if (layoutJson != null) {
                annotation.setLayoutJson(layoutJson);
            }
            annotationMapper.updateStatus(annotation);
            if (layoutJson != null) {
                annotationMapper.updateLayout(annotation.getId(), layoutJson);
            }
        }

        persistContent(annotation, propositions, relations);
    }

    private void clearAnnotationContent(long annotationId) {
        relationMemberMapper.deleteByAnnotationId(annotationId);
        relationMapper.deleteByAnnotationId(annotationId);
        propositionMapper.deleteByAnnotationId(annotationId);
    }

    private void persistContent(AnnotationPo annotation, List<Proposition> propositions, List<Relation> relations) {
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
        return loadRecord(taskId, taskDocumentId, userId, apiDataId, RECORD_ANNOTATION);
    }

    public ArbitrationResult loadArbitration(int taskId, int taskDocumentId, long apiDataId, long arbitratorId,
                                             edu.nju.jap.model.po.ArbitrationSnapshot snapshot) {
        AnnotationResult base = loadRecord(taskId, taskDocumentId, arbitratorId, apiDataId, RECORD_ARBITRATION);
        ArbitrationResult result = new ArbitrationResult(taskId, apiDataId, arbitratorId, base.propositions,
                base.relations, snapshot.getAdoptedFrom(), snapshot.getArbitratedAt());
        result.graphLayout = base.graphLayout;
        result.finalResult = snapshot.getFinalResult() != null && snapshot.getFinalResult() == 1;
        return result;
    }

    private AnnotationResult loadRecord(int taskId, int taskDocumentId, long userId, long apiDataId,
                                        String recordType) {
        AnnotationPo annotation = annotationMapper.selectByScope(taskId, taskDocumentId, userId, recordType);
        if (annotation == null) {
            return new AnnotationResult(taskId, apiDataId, userId, List.of(), List.of(), true, null, null, null);
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
        return new AnnotationResult(taskId, apiDataId, userId, propositions, relations, draft, submitted,
                annotation.getRejectReason(), graphLayoutJsonCodec.fromJson(annotation.getLayoutJson()));
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

    @SuppressWarnings("unchecked")
    private static boolean isRichLayout(Object layout) {
        if (!(layout instanceof Map<?, ?> map)) {
            return false;
        }
        Object version = map.get("version");
        if (version instanceof Number number && number.intValue() == 2) {
            Object nodes = map.get("nodes");
            return nodes instanceof List<?> list && !list.isEmpty();
        }
        Object nodePositions = map.get("nodePositions");
        Object edgeStyles = map.get("edgeStyles");
        boolean hasNodes = nodePositions instanceof Map<?, ?> positions && !positions.isEmpty();
        boolean hasEdges = edgeStyles instanceof Map<?, ?> styles && !styles.isEmpty();
        return hasNodes || hasEdges;
    }
}
