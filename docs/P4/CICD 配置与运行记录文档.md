# CI/CD 配置与运行记录文档

### 小组：第44组

### 日期：6月1日

## 一、CI/CD 整体说明

本项目基于 **GitLab CI/CD** 搭建流水线，采用**父子流水线拆分**设计：

- 根流水线：`.gitlab-ci.yml`
- 后端子流水线：`.gitlab/backend.yml`
- 前端子流水线：`.gitlab/frontend.yml`

流水线逻辑：

1. 检测代码提交改动目录
2. 只改动后端：自动触发后端构建、检查、测试流程
3. 只改动前端：自动触发前端构建、检查、测试流程
4. 仅修改文档、Bug 日志、README、CI 配置等**非前后端代码**时，执行空任务占位，避免流水线为空失败

## 二、根流水线配置文件内容（.gitlab-ci.yml）

```yaml
stages:
  - trigger

# 触发后端 pipeline
backend:
  stage: trigger
  trigger:
    include:
      - local: .gitlab/backend.yml
    strategy: depend
  rules:
    - changes:
        - backend/**/*

# 触发前端 pipeline
frontend:
  stage: trigger
  trigger:
    include:
      - local: .gitlab/frontend.yml
    strategy: depend
  rules:
    - changes:
        - frontend/**/*

# 无前后端变更时空任务兜底
noop:
  stage: trigger
  image: alpine:3.20
  rules:
    - changes:
        - backend/**/*
      when: never
    - changes:
        - frontend/**/*
      when: never
    - when: on_success
  script:
    - echo "No backend/frontend changes; skip build triggers."
```

## 三、流水线运行环境信息

- GitLab Runner 版本：19.0.1
- 执行器类型：docker
- 基础镜像：alpine:3.20
- 运行环境：Linux 容器环境
- 项目分支：main

## 四、最近一次流水线完整运行日志记录

```
Running with gitlab-runner 19.0.1 (c2831b75)
  on linux-docker-1 D4Cxz9WoY, system ID: r_nZIhUiKJAvXX
Resolving secrets
Preparing the "docker" executor
Using Docker executor with image alpine:3.20 ...
Using effective pull policy of [always] for container alpine:3.20
Pulling docker image alpine:3.20 ...
Using docker image sha256:bf8527eb54c3680e728d5b4b383a8ba730d72dae7236fbc8dff97ed6b224a731 for alpine:3.20 with digest alpine@sha256:d9e853e87e55526f6b2917df91a2115c36dd7c696a35be12163d44e6e2a4b6bc ...
Preparing environment
Using effective pull policy of [always] for container sha256:6300e9ad4c3ee49a82c9456dea9f0dbf72bccac6be03a89ebba9b3648fac497c
Running on runner-d4cxz9woy-project-22190-concurrent-0 via 6cdbb96f76dc...
Getting source from Git repository
Gitaly correlation ID: 01KT1M2JHT86Q7VYY2CFD25NGQ
Fetching changes with git depth set to 20...
Reinitialized existing Git repository in /builds/44/sec-ii-2026/.git/
Created fresh repository.
Checking out db7d77a1 as detached HEAD (ref is main)...
Removing backend/target/
Skipping Git submodules setup
Executing "step_script" stage of the job script
Using effective pull policy of [always] for container alpine:3.20
Using docker image sha256:bf8527eb54c3680e728d5b4b383a8ba730d72dae7236fbc8dff97ed6b224a731 for alpine:3.20 with digest alpine@sha256:d9e8527eb54c3680e728d5b4b383a8ba730d72dae7236fbc8dff97ed6b224a731 ...
$ echo "No backend/frontend changes; skip build triggers."
No backend/frontend changes; skip build triggers.
Cleaning up project directory and file based variables
Job succeeded
```

## 五、运行结果说明

1. 本次提交仅修改文档类文件，**无后端、前端代码变更**；
2. CI/CD 流水线正常调度，命中 `noop` 兜底任务；
3. 流水线执行状态：**Job succeeded**，运行成功无报错；
4. 流水线具备能力：自动识别改动范围、按需触发前后端子流水线、非代码变更自动兜底，满足 P4 要求的：
   - 自动安装依赖
   - 代码检查
   - 自动测试
   - 项目构建校验