package edu.nju.jap.dao;

import edu.nju.jap.model.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DemoDataStore {
    public final AtomicLong userSeq = new AtomicLong(10);
    public final AtomicLong docSeq = new AtomicLong(100);
    public final AtomicLong configSeq = new AtomicLong(1);
    public final AtomicLong taskSeq = new AtomicLong(1003);
    public final AtomicLong exportSeq = new AtomicLong(1);

    /** 返回当前文档表中未被占用的最小 ID（从 101 起扫描），删除后新增会复用该空位。 */
    public long nextDocId() {
        long id = 101;
        while (documents.containsKey(id)) {
            id++;
        }
        return id;
    }

    public final Map<Long, User> users = new ConcurrentHashMap<>();
    public final Map<String, Long> tokenToUser = new ConcurrentHashMap<>();
    public final Map<Long, DocumentItem> documents = new ConcurrentHashMap<>();
    public final Map<Long, GuideConfig> configs = new ConcurrentHashMap<>();
    public final Map<Long, TaskItem> tasks = new ConcurrentHashMap<>();
    public final Map<String, AnnotationResult> annotations = new ConcurrentHashMap<>();
    public final Map<String, ArbitrationResult> arbitrations = new ConcurrentHashMap<>();

    public DemoDataStore() {
        seedUsers();
        seedDocuments();
        seedConfig();
        seedTasks();
    }

    private void seedUsers() {
        addUser(1, "admin", "123456", "系统管理员", "admin", false);
        addUser(2, "creator", "123456", "任务创建者", "creator", true);
        addUser(3, "annotator1", "123456", "参与者一", "user", false);
        addUser(4, "annotator2", "123456", "参与者二", "user", false);
        addUser(5, "reviewer", "123456", "参与者三", "user", false);
    }

    private void addUser(long id, String username, String password, String realName, String role,
                         boolean canCreateTask) {
        users.put(id, new User(id, username, password, realName, role, canCreateTask, "正常"));
    }

    private void seedDocuments() {
        addDocument(101, "合同纠纷一审判决书", "民事判决书", """
                本院认为，依法成立的合同，对当事人具有法律约束力。当事人应当按照约定全面履行自己的义务。被告未按期支付货款，已经构成违约。原告提交的送货单、对账单能够相互印证，本院予以采信。因此，被告应当向原告支付货款并承担逾期付款责任。
                """);
        addDocument(102, "劳动争议仲裁审查裁定", "民事裁定书", """
                本院认为，劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定。现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动。公司主张双方不存在劳动关系，但未提交充分反证，本院不予采纳。
                """);
        addDocument(103, "侵权责任纠纷判决书", "民事判决书", """
                本院认为，行为人因过错侵害他人民事权益造成损害的，应当承担侵权责任。监控视频显示，被告车辆倒车时未尽到合理注意义务，与原告车辆发生碰撞。事故认定书载明被告承担全部责任，故原告要求赔偿维修费具有事实和法律依据。
                """);
    }

    private void addDocument(long id, String title, String type, String content) {
        documents.put(id, new DocumentItem(id, "W" + id, title, type, "2026-05-" + (id - 95), content.trim(), 1L));
    }

    private void seedConfig() {
        GuideConfig config = new GuideConfig(1, "V1.0 标准指南", "默认裁判文书论证标签体系", true, "2026-05-01",
                List.of(new LabelDef("SF", "个别事实判断", "关于案件中个别对象的事实判断，是规范适用的前提条件", ""),
                        new LabelDef("GF", "一般事实判断", "为规范适用提供经验或背景支撑，例如社会常识、经验法则、行业知识、科学规律等", ""),
                        new LabelDef("SM", "个别规范判断", "体现法院对本案的规范性评价，在法律论证中通常作为结论出现", ""),
                        new LabelDef("GM", "一般规范判断", "构成法律论证的规范基础，是连接个案事实与裁判结论的核心要素", "")),
                List.of(new LabelDef("GM-L", "法律条文", "直接来源于成文法规范的判断，包括对法律条文内容的引用或概括性复述", "GM"),
                        new LabelDef("GM-I", "法律解释", "对法律条文含义、适用范围或适用条件所作的解释性判断", "GM"),
                        new LabelDef("GM-C", "合同及合同解释", "来源于合同条款或对合同条款的规范性解释", "GM"),
                        new LabelDef("GM-U", "习惯与行业惯例", "来源于社会习惯、交易习惯或行业通行规则的规范性判断", "GM"),
                        new LabelDef("GM-M", "道德与价值观念", "基于价值判断、公序良俗或基本原则作出的规范性判断", "GM"),
                        new LabelDef("GM-O", "其他规范判断", "无法稳定归入上述类型的一般规范判断", "GM")),
                List.of(new LabelDef("S", "支持关系", "一命题或命题组为另一命题的成立提供理由", ""),
                        new LabelDef("A", "反对关系", "一命题或命题组为另一命题的不成立提供理由", ""),
                        new LabelDef("J", "组合关系", "多个命题共同成立方能构成完整理由的合取结构", ""),
                        new LabelDef("M", "匹配关系", "规范构成要件与个案事实之间的对应关系，是组合关系的特殊形态", ""),
                        new LabelDef("I", "同一关系", "多个命题在语义上表达同一判断内容，用于处理裁判文书中的重复表达", "")));
        configs.put(1L, config);
    }

    private void seedTasks() {
        TaskItem task = new TaskItem(1001, "合同法标注演示任务", "标注合同纠纷裁判理由中的事实、规范与关系", "标注中", 1,
                List.of(101L, 102L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now().minusDays(1), configs.get(1L));
        tasks.put(task.id, task);

        List<Proposition> p1 = List.of(
                new Proposition("P1", 1, 5, 12, "依法成立的合同", "GM-L"),
                new Proposition("P2", 2, 13, 24, "对当事人具有法律约束力", "GM-I"),
                new Proposition("P3", 3, 44, 53, "被告未按期支付货款", "SF"),
                new Proposition("P4", 4, 56, 60, "构成违约", "SM"),
                new Proposition("P5", 5, 97, 110, "支付货款并承担逾期付款责任", "SF")
        );

        List<Relation> r1 = List.of(
                new Relation("R1", "S", "P1", "P2"),
                new Relation("R2", "S", "P3", "P4"),
                new Relation("R3", "A", "P5", "P4")
        );

        annotations.put(annotationKey(1001, 101, 3),
                new AnnotationResult(1001, 101, 3, p1, r1, false, LocalDateTime.now().minusHours(5)));
        annotations.put(annotationKey(1001, 101, 4),
                new AnnotationResult(1001, 101, 4,
                        List.of(p1.get(0), p1.get(2), p1.get(3)),
                        List.of(new Relation("R1", "S", "P3", "P4")), false, LocalDateTime.now().minusHours(4)));

        TaskItem done = new TaskItem(1002, "侵权责任裁定样例", "展示裁定与导出流程", "可导出", 1,
                List.of(103L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now().minusDays(3), configs.get(1L));
        tasks.put(done.id, done);

        List<Proposition> p2 = List.of(
                new Proposition("P1", 1, 5, 19, "行为人因过错侵害他人民事权益", "SF"),
                new Proposition("P2", 2, 25, 33, "应当承担侵权责任", "GM-L"),
                new Proposition("P3", 3, 41, 57, "被告车辆倒车时未尽到合理注意义务", "SF"),
                new Proposition("P4", 4, 58, 67, "与原告车辆发生碰撞", "SF"),
                new Proposition("P5", 5, 77, 83, "承担全部责任", "GM-L")
        );

        List<Relation> r2 = List.of(
                new Relation("R1", "S", "P1", "P2"),
                new Relation("R2", "A", "P3", "P2"),
                new Relation("R3", "J", "P1", "P4"),
                new Relation("R4", "M", "P2", "P4"),
                new Relation("R5", "I", "P1", "P5")
        );

        arbitrations.put(arbitrationKey(1002, 103),
                new ArbitrationResult(1002, 103, 5, p2, r2, "MANUAL", LocalDateTime.now().minusDays(1)));

        TaskItem pending = new TaskItem(1003, "劳动争议裁定演示", "演示从数据列表进入裁定界面的完整路径", "待裁定", 1,
                List.of(102L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now().minusDays(2), configs.get(1L));
        tasks.put(pending.id, pending);

        List<Proposition> p3a = List.of(
                new Proposition("P1", 1, 5, 33, "劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定", "GM-L"),
                new Proposition("P2", 2, 34, 62, "现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动", "SF"),
                new Proposition("P3", 3, 63, 83, "公司主张双方不存在劳动关系，但未提交充分反证", "SF"),
                new Proposition("P4", 4, 84, 89, "本院不予采纳", "SM")
        );
        List<Relation> r3a = List.of(
                new Relation("R1", "S", "P1", "P4"),
                new Relation("R2", "S", "P2", "P3"),
                new Relation("R3", "S", "P3", "P4")
        );

        List<Proposition> p3b = List.of(
                new Proposition("P1", 1, 5, 33, "劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定", "GF"),
                new Proposition("P2", 2, 34, 62, "现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动", "SF"),
                new Proposition("P3", 3, 63, 83, "公司主张双方不存在劳动关系，但未提交充分反证", "SF"),
                new Proposition("P4", 4, 84, 89, "本院不予采纳", "SM")
        );
        List<Relation> r3b = List.of(
                new Relation("R1", "S", "P2", "P4"),
                new Relation("R2", "A", "P3", "P4")
        );

        annotations.put(annotationKey(1003, 102, 3),
                new AnnotationResult(1003, 102, 3, p3a, r3a, false, LocalDateTime.now().minusHours(3)));
        annotations.put(annotationKey(1003, 102, 4),
                new AnnotationResult(1003, 102, 4, p3b, r3b, false, LocalDateTime.now().minusHours(2)));
    }

    public User userFromHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long userId = tokenToUser.get(authorization.substring(7));
        return userId == null ? null : users.get(userId);
    }

    public User current(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }

    public String issueToken(User user) {
        String token = "demo-token-" + user.id + "-" + UUID.randomUUID();
        tokenToUser.put(token, user.id);
        return token;
    }

    public static String annotationKey(long taskId, long docId, long userId) {
        return taskId + ":" + docId + ":" + userId;
    }

    public static String arbitrationKey(long taskId, long docId) {
        return taskId + ":" + docId;
    }
}
