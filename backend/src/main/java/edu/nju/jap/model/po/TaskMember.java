package edu.nju.jap.model.po;

public class TaskMember {
    private Integer id;
    private Integer taskId;
    private Long userId;
    private String roleInTask;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleInTask() { return roleInTask; }
    public void setRoleInTask(String roleInTask) { this.roleInTask = roleInTask; }
}
