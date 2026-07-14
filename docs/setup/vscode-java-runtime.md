# VS Code Java 21 런타임 셋업 가이드

Keeply 백엔드는 Java 21 toolchain 을 요구한다. VS Code Java Language Server 가 시스템 기본 JDK 로 프로젝트를 파싱하면
Spring / JUnit import 인식이 실패하고 상태바에 `Gradle: Build Error` 가 표시된다.
본 문서는 **각자의 User Settings** 에 Java 21 런타임을 지정하는 개인 설정 방식을 정리한다.

> 이 프로젝트는 개인 프로젝트 성격이므로 `.vscode/settings.json` 을 팀 공유하지 않고,
> 각자 로컬 JDK 경로에 맞춰 User Settings 로 관리한다.

---

## 1. 증상 확인

다음 중 하나라도 해당하면 본 가이드를 적용한다.

- VS Code 상태바에 `Gradle: Build Error` 또는 `JavaSE-25 LTS` 등 21 이 아닌 버전이 표시된다.
- `org.springframework`, `SpringBootTest` 등 import 가 빨간 밑줄로 인식 실패한다.
- CLI 로는 `./gradlew compileJava`, `./gradlew compileTestJava` 가 정상 `BUILD SUCCESSFUL` 이다.

CLI 는 되고 VS Code 만 실패하면 **IDE 인식 문제**이며, 원인은 대부분 Language Server 가 잘못된 JDK 로 파싱하는 경우다.

---

## 2. 원인

- `build.gradle.kts` 는 `JavaLanguageVersion.of(21)` toolchain 을 요구한다.
- Gradle CLI 는 로컬에 설치된 Java 21 (Temurin 등) 을 자동으로 찾아 빌드에 성공한다.
- VS Code Java Language Server 는 별도 지정이 없으면 `JAVA_HOME` 또는 시스템 기본 JDK 로 파싱한다.
- 시스템 기본 JDK 가 21 이 아니면 (예: Java 25), 21 문법·API 인식은 되더라도 프로젝트 클래스패스 구성이 어긋나 import 오류가 대량 발생한다.

---

## 3. 로컬 Java 21 경로 확인

터미널에서 설치된 JDK 목록을 확인한다.

```bash
/usr/libexec/java_home -V
```

출력 예시.

```text
Matching Java Virtual Machines (5):
    25.0.2 (arm64) ...
    24.0.2 (arm64) ...
    22.0.1 (x86_64) ...
    21.0.11 (arm64) "Eclipse Adoptium" - "OpenJDK 21.0.11" /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
    17.0.1 (x86_64) ...
```

- Java 21 이 없으면 [Temurin 21](https://adoptium.net/temurin/releases/?version=21) 을 설치한 뒤 다시 확인한다.
- 경로 예시: `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` (본인 경로로 대체)

---

## 4. VS Code User Settings 적용

VS Code 명령 팔레트 (`Cmd + Shift + P`) → `Preferences: Open User Settings (JSON)` → 아래 항목을 추가한다.

```jsonc
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
      "default": true
    }
  ],
  "java.import.gradle.java.home": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
}
```

- `path` 는 3 단계에서 확인한 **본인의 Java 21 절대 경로**로 반드시 대체한다.
- `default: true` 로 지정해야 프로젝트 toolchain 이 21 로 요구될 때 Language Server 가 선택한다.
- `java.import.gradle.java.home` 은 Gradle import 시 사용할 JDK 를 명시한다.

---

## 5. Language Server 재초기화

캐시된 잘못된 클래스패스를 완전히 비우기 위해 아래 절차를 실행한다.

1. VS Code 명령 팔레트 → `Java: Clean Java Language Server Workspace` 실행
2. `Restart and delete` 선택
3. VS Code 재시작 후 프로젝트가 다시 로드될 때까지 대기 (하단 상태바에 진행률 표시)

---

## 6. 검증 체크리스트

재시작 완료 후 아래 항목을 확인한다.

- [ ] 상태바에 `JavaSE-21` 이 표시된다 (또는 `Gradle: Build Error` 가 사라진다).
- [ ] `src/test/java/com/keeply/KeeplyServerApplicationTests.java` 를 열었을 때 `org.springframework.boot.test.context.SpringBootTest` import 에 빨간 밑줄이 없다.
- [ ] 문제 (Problems) 탭 카운트가 대폭 감소했다 (0 에 가까움).
- [ ] `./gradlew compileTestJava` 가 CLI 에서도 정상 `BUILD SUCCESSFUL`.

---

## 7. 트러블슈팅

### 여전히 import 오류가 남는 경우

- User Settings JSON 문법 오류 확인 (쉼표·따옴표 누락).
- `path` 마지막에 `/Contents/Home` 까지 포함했는지 확인 (`.jdk` 로 끝나면 안 됨).
- Java Language Server 확장 (`Extension Pack for Java`) 설치·활성화 여부 확인.
- 5 단계 재초기화를 한 번 더 실행.

### 임시 처방 (파일 수정 없이)

설정을 바꾸지 않고 잠깐만 살리고 싶다면 명령 팔레트에서 `Java: Clean Java Language Server Workspace` 만 실행해도
일시적으로 회복될 수 있다. 다만 시스템 기본 JDK 가 21 이 아닌 이상 재발한다.
