DROP TABLE IF EXISTS arbitration_snapshot;
DROP TABLE IF EXISTS relation_member;
DROP TABLE IF EXISTS argument_relation;
DROP TABLE IF EXISTS proposition;
DROP TABLE IF EXISTS annotation;
DROP TABLE IF EXISTS task_document;
DROP TABLE IF EXISTS task_member;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS global_document;
DROP TABLE IF EXISTS relation_type;
DROP TABLE IF EXISTS label_l2;
DROP TABLE IF EXISTS label_l1;
DROP TABLE IF EXISTS guide_version;
DROP TABLE IF EXISTS auth_token;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    can_create_task BOOLEAN NOT NULL DEFAULT FALSE,
    status INT NOT NULL DEFAULT 0,
    last_seen TIMESTAMP NULL
);

CREATE TABLE auth_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guide_version (
    id INT AUTO_INCREMENT PRIMARY KEY,
    version_name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE label_l1 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guide_version_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    abbr VARCHAR(16) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE label_l2 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guide_version_id INT NOT NULL,
    parent_l1_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    abbr VARCHAR(32) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE relation_type (
    id INT AUTO_INCREMENT PRIMARY KEY,
    guide_version_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    abbr VARCHAR(16) NOT NULL,
    description VARCHAR(255),
    is_binary INT NOT NULL DEFAULT 1
);

CREATE TABLE global_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(32),
    extracted_text TEXT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    creator_id BIGINT NOT NULL,
    guide_version_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stage_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_member (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    role_in_task VARCHAR(32) NOT NULL
);

CREATE TABLE task_document (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    global_doc_id BIGINT,
    file_name VARCHAR(255),
    file_path VARCHAR(512),
    extracted_text TEXT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT '标注中'
);

CREATE TABLE annotation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    document_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    guide_version_id INT,
    guide_snapshot TEXT,
    submitted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proposition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation_id BIGINT NOT NULL,
    display_id VARCHAR(32) NOT NULL,
    sequence_no INT NOT NULL,
    start_pos INT NOT NULL,
    end_pos INT NOT NULL,
    selected_text TEXT NOT NULL,
    label_l1 VARCHAR(32),
    label_l2 VARCHAR(32),
    label_path VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE argument_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation_id BIGINT NOT NULL,
    display_id VARCHAR(32) NOT NULL,
    sequence_no INT NOT NULL,
    relation_type VARCHAR(16) NOT NULL,
    expression VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE relation_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    relation_id BIGINT NOT NULL,
    member_type VARCHAR(32) NOT NULL,
    proposition_id BIGINT,
    child_relation_id BIGINT,
    member_role VARCHAR(32),
    member_order INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE arbitration_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    task_document_id INT NOT NULL,
    arbitrator_id BIGINT NOT NULL,
    annotation_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
