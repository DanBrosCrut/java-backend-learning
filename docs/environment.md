# Окружение

## Java

Установлена **Temurin 21.0.10 LTS** (Eclipse Adoptium).
$ java -version
openjdk version "21.0.10" 2026-01-20 LTS
OpenJDK Runtime Environment Temurin-21.0.10+8 (build 21.0.10+8-LTS-217)
OpenJDK 64-Bit Server VM Temurin-21.0.10+8 (build 21.0.10+8-LTS-217, mixed mode, sharing)

**Способ установки:** SDKMAN (Java 21.0.2-tem — базовая версия в `~/.sdkman/candidates/java/current`).
**Системная Java** (через PATH Windows): 21.0.10 LTS.

**Важно:** в Git Bash `java -version` может показывать версию от SDKMAN (21.0.2-tem), а в PowerShell — системную (21.0.10). Обе — LTS, разница в минорной версии не критична.

---

## Maven

Установлен **Apache Maven 3.9.10** через SDKMAN.
$ mvn -version
Apache Maven 3.9.10 (5f519b97e944483d878815739f519b2eade0a91d)
Maven home: C:\Users\Home.sdkman\candidates\maven\current
Java version: 21.0.2, vendor: Eclipse Adoptium, runtime: C:\Users\Home.sdkman\candidates\java\current
Default locale: ru_RU, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"

**Путь к исполняемому файлу:** `C:\Users\Home\.sdkman\candidates\maven\current\bin\mvn.cmd`

---

## SDKMAN

- Установлен в **Git Bash** (MINGW64)
- Инициализация: `source "$HOME/.sdkman/bin/sdkman-init.sh"`
- Через SDKMAN установлены: Java 21.0.2-tem, Maven 3.9.10
- Команды: `sdk list java`, `sdk install <candidate> <version>`, `sdk default <candidate> <version>`
- **Примечание:** сайт `get.sdkman.io` блокируется в РФ — потребовался VPN при установке.

---

## VS Code

- **Версия:** [1.140.0]
- **Расширения:**
  - Extension Pack for Java (Microsoft, `vscjava.vscode-java-pack`)
  - Kilo Code (модель `kilo-auto/free`)

### Настройка Maven в VS Code

В `settings.json` указан путь к `mvn.cmd`:

```json
{
  "maven.executable.path": "C:/Users/Home/.sdkman/candidates/maven/current/bin/mvn.cmd"
}