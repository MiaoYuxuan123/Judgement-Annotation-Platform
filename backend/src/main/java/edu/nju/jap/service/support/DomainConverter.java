package edu.nju.jap.service.support;

import edu.nju.jap.model.entity.DocumentItem;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.model.entity.LabelDef;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.*;
import edu.nju.jap.model.dto.response.UserVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DomainConverter {
    private DomainConverter() {
    }

    public static User toUser(SysUser po) {
        if (po == null) {
            return null;
        }
        User user = new User(po.getId(), po.getUsername(), po.getPasswordHash(), po.getRealName(), po.getRole(),
                po.getCanCreateTask() != null && po.getCanCreateTask() == 1,
                po.getStatus() != null && po.getStatus() == 1 ? "正常" : "禁用");
        user.lastSeen = po.getLastSeen();
        return user;
    }

    public static SysUser toSysUser(User user) {
        SysUser po = new SysUser();
        po.setId(user.id);
        po.setUsername(user.username);
        po.setPasswordHash(user.password);
        po.setRealName(user.realName);
        po.setRole(user.role);
        po.setCanCreateTask(user.canCreateTask ? 1 : 0);
        po.setStatus("正常".equals(user.status) ? 1 : 0);
        po.setLastSeen(user.lastSeen);
        return po;
    }

    public static DocumentItem toDocumentItem(GlobalDocument po) {
        if (po == null) {
            return null;
        }
        String date = po.getUploadedAt() == null ? "" : po.getUploadedAt().toLocalDate().toString();
        return new DocumentItem(po.getId(), po.getDocumentId(), po.getTitle(), po.getFileType(), date,
                po.getExtractedText(), po.getUploadedById() == null ? 0 : po.getUploadedById());
    }

    public static DocumentItem toDocumentItem(TaskDocument td, GlobalDocument global) {
        if (td == null) {
            return null;
        }
        long dataId = td.getGlobalDocId() != null ? td.getGlobalDocId() : td.getId();
        String docCode = global != null ? global.getDocumentId() : ("D" + td.getId());
        String title = global != null ? global.getTitle() : td.getFileName();
        String type = global != null ? global.getFileType() : "txt";
        String date = td.getUploadedAt() == null ? "" : td.getUploadedAt().toLocalDate().toString();
        return new DocumentItem(dataId, docCode, title, type, date, td.getExtractedText(), 0);
    }

    public static GuideConfig toGuideConfig(GuideVersion version, List<LabelL1> l1s, List<LabelL2> l2s,
                                            List<RelationType> types) {
        Map<Integer, String> l1Abbr = l1s.stream().collect(Collectors.toMap(LabelL1::getId, LabelL1::getAbbr));
        List<LabelDef> primary = l1s.stream()
                .map(l -> new LabelDef(l.getAbbr(), l.getName(), l.getDescription(), ""))
                .toList();
        List<LabelDef> secondary = l2s.stream()
                .map(l -> new LabelDef(l.getAbbr(), l.getName(), l.getDescription(),
                        l1Abbr.getOrDefault(l.getParentL1Id(), "")))
                .toList();
        List<LabelDef> relations = types.stream()
                .map(t -> new LabelDef(t.getAbbr(), t.getName(), t.getDescription(), ""))
                .toList();
        String created = version.getCreatedAt() == null ? "" : version.getCreatedAt().toLocalDate().toString();
        return new GuideConfig(version.getId(), version.getVersionName(), version.getDescription(),
                version.getIsActive() != null && version.getIsActive() == 1, created, primary, secondary, relations);
    }

    public static Proposition toProposition(PropositionPo po, int sequence) {
        String tag = po.getLabelPath() != null && !po.getLabelPath().isBlank()
                ? po.getLabelPath()
                : (po.getLabelL2() != null && !po.getLabelL2().isBlank() ? po.getLabelL2() : po.getLabelL1());
        return new Proposition(po.getDisplayId(), sequence, po.getStartPos(), po.getEndPos(), po.getSelectedText(), tag);
    }

    public static Relation toRelation(RelationPo po, List<RelationMember> members) {
        return new Relation(po.getDisplayId(), po.getRelationType(), "", "", List.of());
    }

    public static void splitTag(String tag, PropositionPo po) {
        if (tag == null || tag.isBlank()) {
            po.setLabelL1("");
            po.setLabelL2(null);
            po.setLabelPath("");
            return;
        }
        po.setLabelPath(tag);
        int dash = tag.indexOf('-');
        if (dash > 0 && tag.length() > dash + 1) {
            po.setLabelL1(tag.substring(0, dash));
            po.setLabelL2(tag);
        } else {
            po.setLabelL1(tag);
            po.setLabelL2(null);
        }
    }
}
