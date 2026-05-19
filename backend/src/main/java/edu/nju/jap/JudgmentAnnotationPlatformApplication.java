package edu.nju.jap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SpringBootApplication
public class JudgmentAnnotationPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(JudgmentAnnotationPlatformApplication.class, args);
    }
}

record ApiResponse<T>(int code, String message, T data) {
    static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ApiResponse<Void> responseStatus(ResponseStatusException ex) {
        return ApiResponse.error(ex.getStatusCode().value(), ex.getReason());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ApiResponse<Void> badRequest(Exception ex) {
        return ApiResponse.error(400, "参数错误");
    }

    @ExceptionHandler(Exception.class)
    ApiResponse<Void> serverError(Exception ex) {
        return ApiResponse.error(500, ex.getMessage() == null ? "服务器异常" : ex.getMessage());
    }
}

@Configuration
class WebConfig implements WebMvcConfigurer {
    private final SimpleAuthInterceptor authInterceptor;

    WebConfig(SimpleAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}

@Component
class SimpleAuthInterceptor implements HandlerInterceptor {
    private final DemoDataStore store;

    SimpleAuthInterceptor(DemoDataStore store) {
        this.store = store;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        User user = store.userFromHeader(request.getHeader("Authorization"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或 Token 无效");
        }
        request.setAttribute("currentUser", user);
        return true;
    }
}

@Component
class DemoDataStore {
    final AtomicLong userSeq = new AtomicLong(10);
    final AtomicLong docSeq = new AtomicLong(100);
    final AtomicLong configSeq = new AtomicLong(1);
    final AtomicLong taskSeq = new AtomicLong(1000);
    final AtomicLong exportSeq = new AtomicLong(1);

    final Map<Long, User> users = new ConcurrentHashMap<>();
    final Map<String, Long> tokenToUser = new ConcurrentHashMap<>();
    final Map<Long, DocumentItem> documents = new ConcurrentHashMap<>();
    final Map<Long, GuideConfig> configs = new ConcurrentHashMap<>();
    final Map<Long, TaskItem> tasks = new ConcurrentHashMap<>();
    final Map<String, AnnotationResult> annotations = new ConcurrentHashMap<>();
    final Map<String, ArbitrationResult> arbitrations = new ConcurrentHashMap<>();

    DemoDataStore() {
        seedUsers();
        seedDocuments();
        seedConfig();
        seedTasks();
    }

    private void seedUsers() {
        addUser(1, "admin", "123456", "系统管理员", "admin", true);
        addUser(2, "creator", "123456", "任务创建者", "creator", true);
        addUser(3, "annotator1", "123456", "标注员一", "annotator", false);
        addUser(4, "annotator2", "123456", "标注员二", "annotator", false);
        addUser(5, "reviewer", "123456", "裁定老师", "reviewer", false);
    }

    private void addUser(long id, String username, String password, String realName, String role, boolean canCreateTask) {
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
        GuideConfig config = new GuideConfig(1, "V1.0 标准指南", "默认裁判文书论证标签体系", true,
                List.of(new LabelDef("SF", "个别事实"), new LabelDef("GF", "一般事实"), new LabelDef("SM", "个别规范"), new LabelDef("GM", "一般规范")),
                List.of(new LabelDef("GM-L", "法律"), new LabelDef("GM-I", "解释"), new LabelDef("GM-C", "合同"), new LabelDef("GM-U", "习惯"), new LabelDef("GM-M", "道德"), new LabelDef("GM-O", "其他规范")),
                List.of(new LabelDef("S", "支持"), new LabelDef("A", "反对"), new LabelDef("J", "组合"), new LabelDef("M", "匹配"), new LabelDef("I", "同一")));
        configs.put(1L, config);
    }

    private void seedTasks() {
        TaskItem task = new TaskItem(1001, "合同法标注演示任务", "标注合同纠纷裁判理由中的事实、规范与关系", "标注中", 1,
                List.of(101L, 102L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now().minusDays(1), configs.get(1L));
        tasks.put(task.id, task);

        List<Proposition> p1 = List.of(
                new Proposition("P1", 1, 4, 11, "依法成立的合同", "GM-L"),
                new Proposition("P2", 2, 23, 39, "对当事人具有法律约束力", "GM-I"),
                new Proposition("P3", 3, 65, 77, "被告未按期支付货款", "SF"),
                new Proposition("P4", 4, 82, 88, "构成违约", "SM")
        );
        List<Relation> r1 = List.of(new Relation("R1", "S", "P1", "P2"), new Relation("R2", "S", "P3", "P4"));
        annotations.put(annotationKey(1001, 101, 3), new AnnotationResult(1001, 101, 3, p1, r1, false, LocalDateTime.now().minusHours(5)));
        annotations.put(annotationKey(1001, 101, 4), new AnnotationResult(1001, 101, 4,
                List.of(p1.get(0), p1.get(2), p1.get(3)), List.of(new Relation("R1", "S", "P3", "P4")), false, LocalDateTime.now().minusHours(4)));

        TaskItem done = new TaskItem(1002, "侵权责任裁定样例", "展示裁定与导出流程", "可导出", 1,
                List.of(103L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now().minusDays(3), configs.get(1L));
        tasks.put(done.id, done);
        arbitrations.put(arbitrationKey(1002, 103), new ArbitrationResult(1002, 103, 5,
                List.of(new Proposition("P1", 1, 4, 16, "行为人因过错侵害他人民事权益", "SF"),
                        new Proposition("P2", 2, 22, 32, "应当承担侵权责任", "GM-L")),
                List.of(new Relation("R1", "S", "P1", "P2")), "MANUAL", LocalDateTime.now().minusDays(1)));
    }

    User userFromHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long userId = tokenToUser.get(authorization.substring(7));
        return userId == null ? null : users.get(userId);
    }

    User current(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }

    String issueToken(User user) {
        String token = "demo-token-" + user.id + "-" + UUID.randomUUID();
        tokenToUser.put(token, user.id);
        return token;
    }

    static String annotationKey(long taskId, long docId, long userId) {
        return taskId + ":" + docId + ":" + userId;
    }

    static String arbitrationKey(long taskId, long docId) {
        return taskId + ":" + docId;
    }
}

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController {
    private final DemoDataStore store;

    AuthController(DemoDataStore store) {
        this.store = store;
    }

    @PostMapping("/login")
    ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = store.users.values().stream()
                .filter(u -> u.username.equals(request.username()) && u.password.equals(request.password()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        return ApiResponse.ok("登录成功", Map.of("token", store.issueToken(user), "user", user.safe()));
    }
}

@RestController
@RequestMapping("/api/users")
class UserController {
    private final DemoDataStore store;

    UserController(DemoDataStore store) {
        this.store = store;
    }

    @GetMapping("/me")
    ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.ok(store.current(request).safe());
    }

    @GetMapping
    ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(store.users.values().stream().map(User::safe).toList());
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        long id = store.userSeq.incrementAndGet();
        User user = new User(id, text(body, "username", "user" + id), text(body, "password", "123456"),
                text(body, "realName", "新用户" + id), text(body, "role", "annotator"),
                bool(body, "canCreateTask", false), "正常");
        store.users.put(id, user);
        return ApiResponse.ok("用户创建成功", Map.of("userId", id));
    }

    @PutMapping("/{id}")
    ApiResponse<Void> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        User old = getUser(store, id);
        store.users.put(id, new User(id, old.username, text(body, "password", old.password),
                text(body, "realName", old.realName), text(body, "role", old.role),
                bool(body, "canCreateTask", old.canCreateTask), text(body, "status", old.status)));
        return ApiResponse.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) {
        store.users.remove(id);
        return ApiResponse.ok("删除成功", null);
    }

    private static User getUser(DemoDataStore store, long id) {
        User user = store.users.get(id);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        return user;
    }

    static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    static boolean bool(Map<String, Object> body, String key, boolean fallback) {
        Object value = body.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }
}

@RestController
@RequestMapping("/api/documents")
class DocumentController {
    private final DemoDataStore store;

    DocumentController(DemoDataStore store) {
        this.store = store;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        List<DocumentItem> list = store.documents.values().stream()
                .filter(d -> keyword == null || d.title.contains(keyword) || d.documentId.contains(keyword))
                .sorted(Comparator.comparing(d -> d.id))
                .toList();
        return ApiResponse.ok(Map.of("total", list.size(), "list", list));
    }

    @GetMapping("/{id}")
    ApiResponse<DocumentItem> detail(@PathVariable long id) {
        return ApiResponse.ok(document(store, id));
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        long id = store.docSeq.incrementAndGet();
        User user = store.current(request);
        DocumentItem doc = new DocumentItem(id, "W" + id, UserController.text(body, "title", "新文书" + id),
                UserController.text(body, "type", "民事判决书"), LocalDate.now().toString(),
                UserController.text(body, "content", "本院认为，案涉事实清楚，证据充分。"), user.id);
        store.documents.put(id, doc);
        return ApiResponse.ok("文书创建成功", Map.of("documentId", id));
    }

    @PostMapping("/upload")
    ApiResponse<Map<String, Object>> upload(@RequestParam("files") MultipartFile[] files, HttpServletRequest request) {
        User user = store.current(request);
        for (MultipartFile file : files) {
            long id = store.docSeq.incrementAndGet();
            store.documents.put(id, new DocumentItem(id, "W" + id, file.getOriginalFilename(), "上传文件",
                    LocalDate.now().toString(), "本院认为，" + file.getOriginalFilename() + " 的模拟解析文本已生成，可用于演示标注流程。", user.id));
        }
        return ApiResponse.ok("上传成功", Map.of("count", files.length));
    }

    static DocumentItem document(DemoDataStore store, long id) {
        DocumentItem doc = store.documents.get(id);
        if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文书不存在");
        return doc;
    }
}

@RestController
@RequestMapping("/api/configs/versions")
class ConfigController {
    private final DemoDataStore store;

    ConfigController(DemoDataStore store) {
        this.store = store;
    }

    @GetMapping
    ApiResponse<List<GuideConfig>> list() {
        return ApiResponse.ok(store.configs.values().stream().sorted(Comparator.comparing(c -> c.id)).toList());
    }

    @GetMapping("/active")
    ApiResponse<GuideConfig> active() {
        return ApiResponse.ok(store.configs.values().stream().filter(c -> c.active).findFirst().orElseThrow());
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        long id = store.configSeq.incrementAndGet();
        GuideConfig base = store.configs.get(1L);
        GuideConfig config = new GuideConfig(id, UserController.text(body, "versionName", "V" + id),
                UserController.text(body, "description", "自定义指南版本"), false,
                base.primaryTags, base.secondaryTags, base.relationTypes);
        store.configs.put(id, config);
        return ApiResponse.ok("配置保存成功", Map.of("configId", id));
    }
}

@RestController
@RequestMapping("/api/tasks")
class TaskController {
    private final DemoDataStore store;

    TaskController(DemoDataStore store) {
        this.store = store;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword) {
        List<TaskSummary> list = store.tasks.values().stream()
                .filter(t -> status == null || status.isBlank() || t.status.equals(status))
                .filter(t -> keyword == null || keyword.isBlank() || t.taskName.contains(keyword))
                .sorted(Comparator.comparing((TaskItem t) -> t.id).reversed())
                .map(t -> TaskSummary.from(t, store))
                .toList();
        return ApiResponse.ok(Map.of("total", list.size(), "list", list));
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = store.current(request);
        List<Long> documentIds = longList(body.get("documentIds"));
        if (documentIds.isEmpty()) documentIds = List.of(101L);
        List<Long> annotators = longList(body.get("annotatorIds"));
        if (annotators.isEmpty()) annotators = List.of(3L, 4L);
        long reviewerId = longValue(body.get("reviewerId"), 5);
        long configId = longValue(body.get("configId"), 1);
        long id = store.taskSeq.incrementAndGet();
        TaskItem task = new TaskItem(id, UserController.text(body, "taskName", "新建标注任务" + id),
                UserController.text(body, "description", "课程演示任务"), "标注中", configId, documentIds,
                annotators, reviewerId, user.id, LocalDateTime.now(), store.configs.get(configId));
        store.tasks.put(id, task);
        return ApiResponse.ok("任务创建成功", Map.of("taskId", id));
    }

    @GetMapping("/{id}")
    ApiResponse<TaskDetail> detail(@PathVariable long id) {
        return ApiResponse.ok(TaskDetail.from(task(store, id), store));
    }

    @PutMapping("/{id}/stage")
    ApiResponse<TaskDetail> advance(@PathVariable long id, @RequestBody Map<String, Object> body) {
        TaskItem task = task(store, id);
        String target = UserController.text(body, "status", nextStatus(task.status));
        List<String> order = List.of("标注中", "待裁定", "可导出");
        if (order.indexOf(target) < order.indexOf(task.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "任务阶段不可回退");
        }
        task.status = target;
        task.stageChangedAt = LocalDateTime.now();
        return ApiResponse.ok("阶段已推进", TaskDetail.from(task, store));
    }

    @GetMapping("/{taskId}/items")
    ApiResponse<List<Map<String, Object>>> items(@PathVariable long taskId) {
        TaskItem task = task(store, taskId);
        return ApiResponse.ok(task.documentIds.stream().map(id -> {
            DocumentItem doc = store.documents.get(id);
            return Map.<String, Object>of("dataId", id, "documentId", doc.documentId, "title", doc.title, "status", task.status);
        }).toList());
    }

    @GetMapping("/{taskId}/items/{dataId}")
    ApiResponse<Map<String, Object>> item(@PathVariable long taskId, @PathVariable long dataId, HttpServletRequest request) {
        TaskItem task = task(store, taskId);
        DocumentItem doc = DocumentController.document(store, dataId);
        User user = store.current(request);
        AnnotationResult annotation = store.annotations.get(DemoDataStore.annotationKey(taskId, dataId, user.id));
        if (annotation == null) {
            annotation = new AnnotationResult(taskId, dataId, user.id, List.of(), List.of(), true, null);
        }
        return ApiResponse.ok(Map.of("task", TaskSummary.from(task, store), "document", doc, "config", task.configSnapshot, "annotation", annotation));
    }

    @GetMapping("/{taskId}/results")
    ApiResponse<List<ArbitrationResult>> results(@PathVariable long taskId) {
        TaskItem task = task(store, taskId);
        return ApiResponse.ok(task.documentIds.stream()
                .map(docId -> store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, docId)))
                .filter(Objects::nonNull)
                .toList());
    }

    @GetMapping("/{taskId}/export")
    ApiResponse<Map<String, Object>> export(@PathVariable long taskId, @RequestParam String format) {
        if (!Set.of("json", "xlsx", "png", "svg", "zip").contains(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出格式非法");
        }
        TaskItem task = task(store, taskId);
        long id = store.exportSeq.incrementAndGet();
        Map<String, Object> payload = Map.of("exportId", id, "taskId", taskId, "format", format,
                "progress", 100, "downloadUrl", "/download/task-" + taskId + "." + format,
                "preview", Map.of("task", task.taskName, "labelVersion", task.configSnapshot.versionName, "results", resultsFor(taskId)));
        return ApiResponse.ok("导出成功", payload);
    }

    private List<ArbitrationResult> resultsFor(long taskId) {
        TaskItem task = task(store, taskId);
        return task.documentIds.stream().map(id -> store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, id))).filter(Objects::nonNull).toList();
    }

    static TaskItem task(DemoDataStore store, long id) {
        TaskItem task = store.tasks.get(id);
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        return task;
    }

    static String nextStatus(String status) {
        if ("标注中".equals(status)) return "待裁定";
        if ("待裁定".equals(status)) return "可导出";
        return "可导出";
    }

    static long longValue(Object value, long fallback) {
        if (value == null) return fallback;
        return Long.parseLong(value.toString());
    }

    static List<Long> longList(Object value) {
        if (!(value instanceof List<?> raw)) return List.of();
        return raw.stream().map(v -> Long.parseLong(v.toString())).toList();
    }
}

@RestController
@RequestMapping("/api/annotations")
class AnnotationController {
    private final DemoDataStore store;

    AnnotationController(DemoDataStore store) {
        this.store = store;
    }

    @PostMapping("/submit")
    ApiResponse<Void> submit(@RequestBody AnnotationSubmit body, HttpServletRequest request) {
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
        return ApiResponse.ok("提交成功", null);
    }
}

@RestController
@RequestMapping("/api/reviews")
class ReviewController {
    private final DemoDataStore store;

    ReviewController(DemoDataStore store) {
        this.store = store;
    }

    @GetMapping("/{taskId}")
    ApiResponse<Map<String, Object>> review(@PathVariable long taskId) {
        TaskItem task = TaskController.task(store, taskId);
        List<Map<String, Object>> documents = task.documentIds.stream().map(docId -> {
            DocumentItem doc = store.documents.get(docId);
            List<AnnotationResult> results = task.annotatorIds.stream()
                    .map(uid -> store.annotations.get(DemoDataStore.annotationKey(taskId, docId, uid)))
                    .filter(Objects::nonNull)
                    .toList();
            ArbitrationResult finalResult = store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, docId));
            return Map.<String, Object>of("document", doc, "annotatorResults", results, "finalResult", finalResult == null ? "" : finalResult);
        }).toList();
        return ApiResponse.ok(Map.of("task", TaskSummary.from(task, store), "documents", documents));
    }

    @PostMapping("/adopt")
    ApiResponse<Void> adopt(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        long taskId = TaskController.longValue(body.get("taskId"), 0);
        long docId = TaskController.longValue(body.get("dataId"), TaskController.longValue(body.get("documentId"), 0));
        long annotatorId = TaskController.longValue(body.get("annotatorId"), 0);
        AnnotationResult source = store.annotations.get(DemoDataStore.annotationKey(taskId, docId, annotatorId));
        if (source == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "标注结果不存在");
        User user = store.current(request);
        store.arbitrations.put(DemoDataStore.arbitrationKey(taskId, docId), new ArbitrationResult(taskId, docId, user.id,
                source.propositions, source.relations, String.valueOf(annotatorId), LocalDateTime.now()));
        TaskController.task(store, taskId).status = "可导出";
        return ApiResponse.ok("裁定完成", null);
    }

    @PostMapping("/manual")
    ApiResponse<Void> manual(@RequestBody ArbitrationSubmit body, HttpServletRequest request) {
        User user = store.current(request);
        store.arbitrations.put(DemoDataStore.arbitrationKey(body.taskId(), body.dataId()), new ArbitrationResult(body.taskId(), body.dataId(), user.id,
                body.propositions() == null ? List.of() : body.propositions(),
                body.relations() == null ? List.of() : body.relations(), "MANUAL", LocalDateTime.now()));
        TaskController.task(store, body.taskId()).status = "可导出";
        return ApiResponse.ok("裁定完成", null);
    }
}

record LoginRequest(@NotBlank String username, @NotBlank String password) {
}

record LabelDef(String shortName, String name) {
}

record Proposition(String propId, int sequenceNo, int startPos, int endPos, String text, String tag) {
}

record Relation(String relId, String type, String source, String target) {
}

record AnnotationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations, boolean isDraft) {
}

record ArbitrationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations) {
}

class User {
    public long id;
    public String username;
    public String password;
    public String realName;
    public String role;
    public boolean canCreateTask;
    public String status;

    User(long id, String username, String password, String realName, String role, boolean canCreateTask, String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
        this.canCreateTask = canCreateTask;
        this.status = status;
    }

    Map<String, Object> safe() {
        return Map.of("id", id, "username", username, "realName", realName, "role", role, "canCreateTask", canCreateTask, "status", status);
    }
}

class DocumentItem {
    public long id;
    public String documentId;
    public String title;
    public String type;
    public String uploadDate;
    public String content;
    public long uploadedBy;

    DocumentItem(long id, String documentId, String title, String type, String uploadDate, String content, long uploadedBy) {
        this.id = id;
        this.documentId = documentId;
        this.title = title;
        this.type = type;
        this.uploadDate = uploadDate;
        this.content = content;
        this.uploadedBy = uploadedBy;
    }
}

class GuideConfig {
    public long id;
    public String versionName;
    public String description;
    public boolean active;
    public List<LabelDef> primaryTags;
    public List<LabelDef> secondaryTags;
    public List<LabelDef> relationTypes;

    GuideConfig(long id, String versionName, String description, boolean active, List<LabelDef> primaryTags, List<LabelDef> secondaryTags, List<LabelDef> relationTypes) {
        this.id = id;
        this.versionName = versionName;
        this.description = description;
        this.active = active;
        this.primaryTags = primaryTags;
        this.secondaryTags = secondaryTags;
        this.relationTypes = relationTypes;
    }
}

class TaskItem {
    public long id;
    public String taskName;
    public String description;
    public String status;
    public long configId;
    public List<Long> documentIds;
    public List<Long> annotatorIds;
    public long reviewerId;
    public long creatorId;
    public LocalDateTime createdAt;
    public LocalDateTime stageChangedAt;
    public GuideConfig configSnapshot;

    TaskItem(long id, String taskName, String description, String status, long configId, List<Long> documentIds,
             List<Long> annotatorIds, long reviewerId, long creatorId, LocalDateTime createdAt, GuideConfig configSnapshot) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.status = status;
        this.configId = configId;
        this.documentIds = documentIds;
        this.annotatorIds = annotatorIds;
        this.reviewerId = reviewerId;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.stageChangedAt = createdAt;
        this.configSnapshot = configSnapshot;
    }
}

record TaskSummary(long taskId, String taskName, String description, String status, int documentCount, int annotatorCount,
                   String reviewerName, String creatorName, LocalDateTime createdAt) {
    static TaskSummary from(TaskItem task, DemoDataStore store) {
        User reviewer = store.users.get(task.reviewerId);
        User creator = store.users.get(task.creatorId);
        return new TaskSummary(task.id, task.taskName, task.description, task.status, task.documentIds.size(),
                task.annotatorIds.size(), reviewer == null ? "-" : reviewer.realName, creator == null ? "-" : creator.realName, task.createdAt);
    }
}

record TaskDetail(TaskSummary summary, List<DocumentItem> documents, List<Map<String, Object>> annotators,
                  Map<String, Object> reviewer, GuideConfig configSnapshot) {
    static TaskDetail from(TaskItem task, DemoDataStore store) {
        return new TaskDetail(TaskSummary.from(task, store),
                task.documentIds.stream().map(store.documents::get).filter(Objects::nonNull).toList(),
                task.annotatorIds.stream().map(id -> store.users.get(id).safe()).toList(),
                store.users.get(task.reviewerId).safe(),
                task.configSnapshot);
    }
}

class AnnotationResult {
    public long taskId;
    public long dataId;
    public long userId;
    public List<Proposition> propositions;
    public List<Relation> relations;
    public boolean draft;
    public LocalDateTime submittedAt;

    AnnotationResult(long taskId, long dataId, long userId, List<Proposition> propositions, List<Relation> relations, boolean draft, LocalDateTime submittedAt) {
        this.taskId = taskId;
        this.dataId = dataId;
        this.userId = userId;
        this.propositions = propositions;
        this.relations = relations;
        this.draft = draft;
        this.submittedAt = submittedAt;
    }
}

class ArbitrationResult {
    public long taskId;
    public long dataId;
    public long arbitratorId;
    public List<Proposition> propositions;
    public List<Relation> relations;
    public String adoptedFrom;
    public LocalDateTime arbitratedAt;
    public boolean finalResult = true;

    ArbitrationResult(long taskId, long dataId, long arbitratorId, List<Proposition> propositions, List<Relation> relations, String adoptedFrom, LocalDateTime arbitratedAt) {
        this.taskId = taskId;
        this.dataId = dataId;
        this.arbitratorId = arbitratorId;
        this.propositions = propositions;
        this.relations = relations;
        this.adoptedFrom = adoptedFrom;
        this.arbitratedAt = arbitratedAt;
    }
}
