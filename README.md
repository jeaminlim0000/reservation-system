# 캠퍼스 예약 시스템

대학 내 강의실 및 장비 대여를 효율적으로 관리하기 위한 웹 기반 예약 시스템

## 프로젝트 소개

학생들이 강의실과 실습 장비를 예약하고, 관리자가 이를 승인 및 관리할 수 있는 통합 예약 시스템입니다.

### 개발 배경
- 기존 수기 예약 방식의 비효율성 개선
- 실시간 예약 현황 확인 필요
- 체계적인 사용자 및 예약 관리 시스템 구축

## 주요 기능

### 사용자 기능
- 회원가입 및 로그인
- 강의실 예약 (캠퍼스/건물/층별 검색)
- 장비 대여 신청
- 예약 내역 조회 및 취소
- 게시판 (공지사항, 문의)

### 관리자 기능
- 회원 가입 승인/거부
- 강의실 관리 (추가/수정/삭제)
- 장비 관리 (추가/수정/삭제)
- 사용자 계정 관리
- 예약 현황 모니터링

## 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java-1.6-blue)
![Spring](https://img.shields.io/badge/Spring_Framework-3.1.1.RELEASE-brightgreen)
![Servlet](https://img.shields.io/badge/Servlet_API-3.0.1-orange)
![JSP](https://img.shields.io/badge/JSP-2.2-yellow)
![MyBatis](https://img.shields.io/badge/MyBatis-3.2.8-red)

### Frontend
![HTML5](https://img.shields.io/badge/HTML5-Markup-orange)
![CSS3](https://img.shields.io/badge/CSS3-Style-blue)
![JavaScript](https://img.shields.io/badge/JavaScript-ES5/ES6-yellow)
![JSTL](https://img.shields.io/badge/JSTL-1.2-lightgrey)

### Database
![MySQL](https://img.shields.io/badge/MySQL-5.x-blue)
![JDBC](https://img.shields.io/badge/Connector/J-5.1.48-blue)

### Server & Build
![Tomcat](https://img.shields.io/badge/Tomcat-9.0.98-important)
![Maven](https://img.shields.io/badge/Maven-Build_Tool-orange)

## 시스템 요구사항

- OS: Windows 10/11, macOS, Linux
- JDK: Java 6 이상 권장
- MySQL: 5.x 이상
- Apache Tomcat: 9.0 이상

## 설치 및 실행

### 1. 데이터베이스 설정

#### (1) MySQL 접속
```bash
mysql -u root -p
```

#### (2) 데이터베이스 생성 및 스키마 적용
```sql
CREATE DATABASE limlimlim;
```

```bash
mysql -u root -p limlimlim < database/schema.sql
```

#### (3) 초기 데이터 입력 (관리자 계정)
```bash
mysql -u root -p limlimlim < database/init_data.sql
```

### 2. 프로젝트 설정

각 Servlet 및 JSP 파일에서 데이터베이스 접속 정보를 본인의 MySQL 계정으로 수정:

```java
private static final String USER = "YOUR_DB_USER";
private static final String PW   = "YOUR_DB_PASSWORD";
```

수정이 필요한 파일 예시:
- `SignupServlet.java`
- `LoginServlet.java`
- `AdminDeleteUserServlet.java`
- `admin_delete.jsp`
- 기타 DB 접속을 사용하는 모든 파일

### 3. 실행 방법

#### IntelliJ IDEA 사용 시
1. File → Open → 프로젝트 폴더 선택
2. Run → Edit Configurations
3. '+' 클릭 → Tomcat Server → Local
4. Deployment 탭에서 Artifact 추가
5. Run 버튼 클릭

#### 수동 빌드 및 배포
```bash
mvn clean package
cp target/reservation_project.war $TOMCAT_HOME/webapps/
```

### 4. 접속
```
http://localhost:8080/프로젝트명/
```

### 기본 관리자 계정
- 아이디: admin
- 비밀번호: init_data.sql에서 설정한 비밀번호

## 데이터베이스 구조

### 주요 테이블

**user_info** - 사용자 정보
- id (PK)
- pwd
- name
- email
- department
- reg_date

**user_signup_request** - 회원가입 승인 대기
- req_id (PK)
- user_id
- status (PENDING/APPROVED/REJECTED)
- requested_at

**room** - 강의실 정보
- id (PK)
- campus
- building
- floor
- room_no

**room_reservationfff** - 강의실 예약
- id (PK)
- user_id (FK)
- room_id (FK)
- timeslot
- reserved_date

**tool** - 장비 정보
- id (PK)
- name
- category
- department
- location

**tool_reservation** - 장비 대여
- id (PK)
- user_id (FK)
- tool_id (FK)
- timeslot
- reserved_date

자세한 스키마는 `database/schema.sql` 참고

## 주요 구현 사항

### 인증 및 권한 관리
- 세션 기반 로그인 시스템
- 관리자/일반 사용자 권한 분리
- AuthFilter를 통한 접근 제어

### 예약 시스템
- 중복 예약 방지 (DB 제약조건)
- 시간대별 예약 관리
- 예약 내역 조회 및 취소

### 관리자 기능
- 회원 가입 승인 워크플로우
- 강의실/장비 동적 관리
- 사용자 계정 관리 (관리자 계정 삭제 방지)

## 프로젝트 구조

```
reservation-project/
├── database/
│   ├── schema.sql          # 데이터베이스 스키마
│   └── init_data.sql       # 초기 데이터
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/lim/
│   │   │       ├── AdminDeleteUserServlet.java
│   │   │       ├── LoginServlet.java
│   │   │       ├── ReservationServlet.java
│   │   │       └── ...
│   │   ├── webapp/
│   │   │   ├── WEB-INF/
│   │   │   │   └── web.xml
│   │   │   ├── index.jsp
│   │   │   ├── main.jsp
│   │   │   ├── admin_delete.jsp
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.properties
│   └── Main.java
├── pom.xml
└── README.md
```


## 개발자 정보

- 개발자: Lim jeamin
- 이메일: woals3346@naver.com
- GitHub: https://github.com/jeaminlim0000/reservation-system

## 참고 사항

본 프로젝트는 개인 포트폴리오 목적으로 제작되었습니다.
실제 운영 환경에서 사용하려면 보안 강화가 필요합니다.
