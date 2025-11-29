-- ============================================
-- 예약 시스템 데이터베이스 스키마
-- ============================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS limlimlim;
USE limlimlim;

-- ============================================
-- 1. 사용자 관련 테이블
-- ============================================

-- 사용자 정보 테이블
CREATE TABLE user_info
(
    id         VARCHAR(50)                           NOT NULL PRIMARY KEY,
    pwd        VARCHAR(100)                          NOT NULL,
    name       VARCHAR(100)                          NOT NULL,
    email      VARCHAR(150)                          NOT NULL,
    department VARCHAR(50) DEFAULT '알수없음'            NOT NULL,
    reg_date   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL
) CHARSET = utf8mb4;

-- 회원가입 요청 테이블
CREATE TABLE user_signup_request
(
    req_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id       VARCHAR(50)                                                                     NOT NULL,
    pwd           VARCHAR(100)                                                                    NOT NULL,
    name          VARCHAR(100)                                                                    NOT NULL,
    email         VARCHAR(150)                                                                    NOT NULL,
    department    VARCHAR(50)                                                                     NOT NULL,
    status        ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING'         NOT NULL,
    requested_at  DATETIME                                              DEFAULT CURRENT_TIMESTAMP NOT NULL,
    decided_at    DATETIME                                                                        NULL,
    decided_by    VARCHAR(50)                                                                     NULL,
    decision_note VARCHAR(200)                                                                    NULL,
    requester_ip  VARCHAR(45)                                                                     NULL,
    user_agent    VARCHAR(200)                                                                    NULL,
    updated_at    DATETIME                                              DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_signup_email UNIQUE (email),
    CONSTRAINT uq_signup_user UNIQUE (user_id),
    CONSTRAINT fk_signup_decider FOREIGN KEY (decided_by) REFERENCES user_info (id) ON DELETE SET NULL
) CHARSET = utf8mb4;

CREATE INDEX idx_signup_decided_at ON user_signup_request (decided_at);
CREATE INDEX idx_signup_status_reqtime ON user_signup_request (status, requested_at);

-- 회원가입 요청 로그 테이블
CREATE TABLE user_signup_request_log
(
    log_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    req_id      INT                                                   NOT NULL,
    prev_status ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NULL,
    new_status  ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL,
    changed_at  DATETIME DEFAULT CURRENT_TIMESTAMP                    NOT NULL,
    changed_by  VARCHAR(50)                                           NULL,
    note        VARCHAR(200)                                          NULL,
    CONSTRAINT fk_signup_log_admin FOREIGN KEY (changed_by) REFERENCES user_info (id) ON DELETE SET NULL,
    CONSTRAINT fk_signup_log_req FOREIGN KEY (req_id) REFERENCES user_signup_request (req_id) ON DELETE CASCADE
) CHARSET = utf8mb4;

CREATE INDEX idx_signup_log_req ON user_signup_request_log (req_id, changed_at);

-- ============================================
-- 2. 강의실 관련 테이블
-- ============================================

-- 강의실 테이블
CREATE TABLE room
(
    id       INT AUTO_INCREMENT PRIMARY KEY,
    campus   VARCHAR(50)                        NOT NULL,
    building VARCHAR(50)                        NOT NULL,
    floor    INT                                NOT NULL,
    room_no  VARCHAR(20)                        NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT uq_room UNIQUE (campus, building, floor, room_no)
) CHARSET = utf8mb4;

CREATE INDEX idx_room_campus_building ON room (campus, building);
CREATE INDEX idx_room_campus_building_floor ON room (campus, building, floor);

-- 강의실 예약 테이블
CREATE TABLE room_reservationfff
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       VARCHAR(50)                        NOT NULL,
    room_id       INT                                NOT NULL,
    timeslot      VARCHAR(10)                        NOT NULL,
    reserved_date DATE                               NOT NULL,
    reserved_at   DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT uq_rr UNIQUE (room_id, reserved_date, timeslot),
    CONSTRAINT fk_rr_room FOREIGN KEY (room_id) REFERENCES room (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_user FOREIGN KEY (user_id) REFERENCES user_info (id) ON DELETE CASCADE
) CHARSET = utf8mb4;

CREATE INDEX idx_rr_date ON room_reservationfff (reserved_date);

-- 강의실 과거 데이터 테이블
CREATE TABLE roompast
(
    id       INT AUTO_INCREMENT PRIMARY KEY,
    campus   VARCHAR(50)                        NOT NULL,
    building VARCHAR(50)                        NOT NULL,
    floor    INT                                NOT NULL,
    room_no  VARCHAR(20)                        NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT uq_room UNIQUE (campus, building, floor, room_no)
) CHARSET = utf8mb4;

CREATE INDEX idx_campus_building ON roompast (campus, building);
CREATE INDEX idx_campus_building_floor ON roompast (campus, building, floor);

-- ============================================
-- 3. 예약 관련 테이블 (레거시)
-- ============================================

-- 예약 테이블
CREATE TABLE reservation
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    campus        VARCHAR(50)                        NOT NULL,
    building      VARCHAR(50)                        NOT NULL,
    floor         INT                                NOT NULL,
    room          VARCHAR(10)                        NOT NULL,
    timeslot      VARCHAR(10)                        NOT NULL,
    reserved_by   VARCHAR(50)                        NOT NULL,
    reserved_at   DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    reserved_date DATE                               NOT NULL,
    CONSTRAINT uq_res UNIQUE (campus, building, floor, room, reserved_date, timeslot),
    CONSTRAINT fk_reservation_user FOREIGN KEY (reserved_by) REFERENCES user_info (id) ON DELETE CASCADE
) CHARSET = utf8mb4;

CREATE INDEX idx_res_date ON reservation (reserved_date);

-- 예약 과거 데이터 테이블
CREATE TABLE reservationpast
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    campus        VARCHAR(50)                        NULL,
    building      VARCHAR(50)                        NULL,
    floor         INT                                NULL,
    room          VARCHAR(10)                        NULL,
    timeslot      VARCHAR(10)                        NULL,
    reserved_by   VARCHAR(50)                        NULL,
    reserved_at   DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    reserved_date DATE     DEFAULT (CURDATE())       NOT NULL
);

-- ============================================
-- 4. 도구 대여 관련 테이블
-- ============================================

-- 도구 테이블
CREATE TABLE tool
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)                        NOT NULL,
    category   VARCHAR(50)                        NOT NULL,
    department VARCHAR(50)                        NULL,
    location   VARCHAR(100)                       NULL,
    added_at   DATETIME DEFAULT CURRENT_TIMESTAMP NULL
) CHARSET = utf8mb4;

-- 도구 예약 테이블
CREATE TABLE tool_reservation
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       VARCHAR(50)                        NOT NULL,
    tool_id       INT                                NOT NULL,
    timeslot      VARCHAR(10)                        NOT NULL,
    reserved_date DATE                               NOT NULL,
    reserved_at   DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT fk_res_tool FOREIGN KEY (tool_id) REFERENCES tool (id),
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES user_info (id) ON DELETE CASCADE
) CHARSET = utf8mb4;
