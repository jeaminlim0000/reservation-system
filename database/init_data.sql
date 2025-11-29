-- ============================================
-- 초기 데이터 (관리자 계정 및 샘플 데이터)
-- ============================================

USE limlimlim;

-- 관리자 계정 생성
-- 비밀번호는 실제 사용하실 비밀번호로 변경하세요
INSERT INTO user_info (id, pwd, name, email, department, reg_date)
VALUES ('admin', '관리자비밀번호', '관리자', 'admin@example.com', '관리부서', NOW());

-- 샘플 사용자 데이터 (선택사항)
-- INSERT INTO user_info (id, pwd, name, email, department, reg_date)
-- VALUES 
--     ('user1', 'password1', '홍길동', 'hong@example.com', '컴퓨터공학과', NOW()),
--     ('user2', 'password2', '김철수', 'kim@example.com', '전자공학과', NOW());

-- 샘플 강의실 데이터 (선택사항)
-- INSERT INTO room (campus, building, floor, room_no, added_at)
-- VALUES 
--     ('본교', '공학관', 1, '101', NOW()),
--     ('본교', '공학관', 1, '102', NOW()),
--     ('본교', '공학관', 2, '201', NOW());

-- 샘플 도구 데이터 (선택사항)
-- INSERT INTO tool (name, category, department, location, added_at)
-- VALUES 
--     ('노트북 1', '전자기기', '전산실', '전산실 1층', NOW()),
--     ('빔프로젝터 1', '영상기기', '전산실', '전산실 2층', NOW()),
--     ('카메라 1', '촬영장비', '미디어실', '미디어실', NOW());
