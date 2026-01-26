# Horizon Backend

Spring Boot 기반의 Horizon 프로젝트 백엔드 서버입니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Security** - JWT 기반 인증
- **Spring Data JPA** - 데이터 접근 계층
- **PostgreSQL** - 데이터베이스
- **Lombok** - 보일러플레이트 코드 감소
- **MapStruct** - Entity-DTO 매핑
- **JJWT** - JWT 토큰 생성 및 검증

## 프로젝트 구조

```
src/main/java/com/horizon/backend/
├── HorizonBackendApplication.java    # 메인 애플리케이션
├── common/                           # 공통 응답 클래스
│   ├── ApiResponse.java
│   └── PageResponse.java
├── config/                           # 설정 클래스
│   ├── JpaConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/                       # REST 컨트롤러
│   ├── AuthController.java
│   └── HealthController.java
├── dto/                              # 데이터 전송 객체
│   ├── auth/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RefreshTokenRequest.java
│   │   └── RegisterRequest.java
│   └── user/
│       ├── UserCreateRequest.java
│       ├── UserDto.java
│       └── UserUpdateRequest.java
├── entity/                           # JPA 엔티티
│   ├── BaseEntity.java
│   ├── Role.java
│   └── User.java
├── exception/                        # 예외 처리
│   ├── BadRequestException.java
│   ├── CustomException.java
│   ├── DuplicateResourceException.java
│   ├── ErrorCode.java
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── mapper/                           # MapStruct 매퍼
│   └── UserMapper.java
├── repository/                       # JPA 리포지토리
│   └── UserRepository.java
├── security/                         # 보안 관련
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
└── service/                          # 비즈니스 로직
    ├── AuthService.java
    └── impl/
        └── AuthServiceImpl.java
```

## 시작하기

### 사전 요구사항

- JDK 17 이상
- Gradle 8.x

### 개발 환경 실행

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행 (개발 모드)
./gradlew bootRun

# 또는 JAR 파일로 실행
./gradlew bootJar
java -jar build/libs/horizon-backend-0.0.1.jar
```

### 프로덕션 환경 실행

```bash
# 프로덕션 프로파일로 실행
java -jar build/libs/horizon-backend-0.0.1.jar --spring.profiles.active=prod
```

## API 엔드포인트

### 인증 API

| Method | Endpoint         | 설명           | 인증 필요 |
|--------|------------------|----------------|-----------|
| POST   | /api/auth/register | 회원가입       | X         |
| POST   | /api/auth/login    | 로그인         | X         |
| POST   | /api/auth/refresh  | 토큰 갱신      | X         |
| POST   | /api/auth/logout   | 로그아웃       | O         |
| GET    | /api/auth/validate | 토큰 검증      | O         |

### 헬스 체크 API

| Method | Endpoint          | 설명           | 인증 필요 |
|--------|-------------------|----------------|-----------|
| GET    | /api/health       | 서비스 상태    | X         |
| GET    | /api/health/ping  | Ping 테스트    | X         |

### 환경 설정

### 데이터베이스 설정

PostgreSQL 데이터베이스를 사용합니다.

| 설정 항목 | 값 |
|-----------|-----|
| Host | 10.0.0.2 |
| Port | 5432 |
| Database | mydb |
| Schema | public |
| Username | admin |
| Password | hxnx |

### 개발 환경 (application-dev.yml)

- PostgreSQL 데이터베이스 사용
- 상세 SQL 로깅 활성화
- `ddl-auto: update` 설정

### 프로덕션 환경 (application-prod.yml)

- PostgreSQL 데이터베이스 사용
- Connection Pool 최적화 (최대 20개)
- SQL 로깅 비활성화

## JWT 설정

| 설정 항목 | 기본값 | 설명 |
|-----------|--------|------|
| jwt.secret | - | JWT 서명 키 (최소 256비트) |
| jwt.expiration | 86400000 | Access Token 만료 시간 (24시간) |
| jwt.refresh-expiration | 604800000 | Refresh Token 만료 시간 (7일) |

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "HorizonBackendApplicationTests"
```

## Docker

### Docker 빌드 및 실행

```bash
# Docker 이미지 빌드
docker build -t horizon-backend .

# Docker 컨테이너 실행
docker run -d \
  --name horizon-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  horizon-backend
```

### Docker Compose

```bash
# 서비스 시작
docker-compose up -d

# 서비스 중지
docker-compose down

# 로그 확인
docker-compose logs -f backend

# 이미지 재빌드 후 시작
docker-compose up -d --build
```

### Docker 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `prod` |
| `SPRING_DATASOURCE_URL` | DB 연결 URL | `jdbc:postgresql://10.0.0.2:5432/mydb?currentSchema=public` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `hxnx` |
| `JWT_SECRET` | JWT 서명 키 | - |
| `JWT_EXPIRATION` | Access Token 만료시간 (ms) | `86400000` |
| `JWT_REFRESH_EXPIRATION` | Refresh Token 만료시간 (ms) | `604800000` |

### Dockerfile 특징

- **Multi-stage 빌드**: 빌드와 런타임 분리로 이미지 크기 최적화
- **Non-root 사용자**: 보안을 위해 spring 사용자로 실행
- **Health Check**: `/api/health/ping` 엔드포인트로 상태 확인
- **JVM 최적화**: 컨테이너 환경에 맞는 메모리 설정

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.