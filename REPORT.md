# Отчёт по неделе 1 (02.10 – 07.10)

## Agile и Scrum
**Дата:** 02.10.2026

### Чем Agile отличается от «водопада»

**Waterfall** — последовательный подход: фазы идут одна за другой (анализ → проектирование → разработка → тестирование → внедрение). Следующая фаза не начинается, пока не закончится предыдущая.

**Agile** — итеративный подход: работа разбита на короткие циклы (спринты), каждый из которых производит работающий инкремент. Требования могут меняться между итерациями, а обратная связь от клиента приходит после каждого спринта.

Главное отличие: в Waterfall клиент видит результат в конце, в Agile — каждую итерацию. Agile лучше подходит, когда требования неясны или могут меняться.

### Зачем Daily и Retrospective

**Daily Scrum (15 минут):** событие для Developers, чтобы инспектировать прогресс к Sprint Goal и адаптировать Sprint Backlog. Это не статус-митинг для начальства, а инструмент самоуправления команды. Daily помогает:
- Синхронизировать работу
- Рано выявлять препятствия
- Принимать быстрые решения

**Sprint Retrospective:** рефлексия в конце Sprint — что прошло хорошо, что нет, какие улучшения внедрить. Цель — повысить качество и эффективность команды. Это не разбор ошибок, а поиск конкретных действий по улучшению процесса.

### Definition of Done

**Definition of Done (DoD)** — формальное описание состояния Increment, когда он соответствует требованиям качества. Это общий чек-лист для всех задач, в отличие от Acceptance Criteria, которые относятся к конкретной фиче.

Если элемент Product Backlog не соответствует DoD — он **не может быть выпущен** и даже представлен на Sprint Review. Вместо этого он возвращается в Product Backlog.

DoD обеспечивает прозрачность: все понимают, что значит «готово». Это «социальный контракт» команды.

---

## МойСклад

### 1. Регистрация и учебный центр

**Дата:** 03.10.2026
**Аккаунт:** [danya.pavlinov01@mail.ru]

Пройдены вводные уроки Учебного центра МойСклад:
Просмотрел уроки создания товаров, отгрузки, закупки покупателей

**Ключевые понятия, которые разобрал:**
- Товар и его карточка
- Приёмка и себестоимость
- Остатки на складе
- Заказ покупателя и резервирование
- Отгрузка и списание

---

### 2. Обзор API (dev.moysklad.ru)

**Прочитано:**
- «Первые шаги в API» — что нужно знать для начала работы
- Ограничения JSON API
- Основные сущности: Товары, Контрагенты, Заказы покупателей, Приёмки
- Метаданные объектов
- Вебхуки — механизм для отслеживания изменений в аккаунте — позволяет получать данные об изменениях в реальном времени, вместо периодических запросов.

---

### 3. Онлайн-заказ: что это и зачем

**Где найти:** Решения → Онлайн-заказ

**Что такое Онлайн-заказ:**
Приложение внутри МойСклад, которое создаёт «витрину» товаров — общедоступную ссылку на каталог. Покупатель открывает ссылку, выбирает товары, оформляет заказ — и этот заказ автоматически попадает в МойСклад как «Заказ покупателя» [citation:3].

**Зачем бизнесу (5–10 тезисов):**
1. Не нужен отдельный сайт — достаточно ссылки, которую можно разместить где угодно
2. Заказы автоматически попадают в МойСклад как «Заказ покупателя»
3. Остатки и цены обновляются автоматически
4. Можно показывать только товары в наличии
5. Есть функция «Резервировать заказанные товары» — защита от продажи большего числа, чем есть на складе(удобное ограничение)
6. Можно создать персональную ссылку для конкретного контрагента с его типом цен
7. Покупатель указывает имя и телефон — они попадают в комментарий к заказу
8. 24/7
9. Удобно на любом устройстве
10. На бесплатных тарифах доступен не весь функционал

(Если обобщить простыми словами - удобно, автоматически, оптимизированно)

**Как настроить ссылку (изучил):**
- Перейти в Решения → Онлайн-заказ → Ссылки на каталоги товаров
- Нажать «+Общедоступная ссылка»
- Указать: название ссылки, юрлицо, группы товаров, склады, тип цен
- Отметить «Резервировать заказанные товары» (рекомендуется)
- Нажать «Создать»
(https://danyapavlinov01.moysklad.shop/catalog) - готовая ссылка

**Отличие от «Заказа покупателя» в интерфейсе:**

| Онлайн-заказ | Заказ покупателя (вручную) |
|--------------|---------------------------|
| Создаётся покупателем через ссылку | Создаётся менеджером вручную |
| Автоматически попадает в систему | Вводится вручную |
| Покупатель сам видит остатки и цены | Менеджер подбирает товары |
| Работает без участия менеджера | Зависит от менеджера |
| Контрагент = «Розничный покупатель»| Контрагент выбирается вручную |

(Если обобщить простыми словами - удобно, автоматически, оптимизированно)

---

### 4. Сквозной сценарий: товар → приёмка → заказ → отгрузка

**Шаг 1: Создание товара**
- Раздел: Товары → Товары
- Создан товар: [название, артикул, цена]

**Шаг 2: Приёмка**
- Раздел: Закупки → Приёмки
- Создан документ приёмки: [номер документа]
- Указано: организация, контрагент (поставщик), склад, позиции с ценами
- **Результат:** товар появился на складе, сформирована себестоимость, появился долг перед поставщиком

**Шаг 3: Заказ покупателя**
- Раздел: Продажи → Заказы покупателей
- Создан заказ: [номер документа]
- Указано: контрагент (покупатель), склад, позиции [citation:6]
- **Важно:** заказ не влияет на остатки, только резервирует товар [citation:6]

**Шаг 4: Отгрузка**
- Из Заказа покупателя создан документ Отгрузка
- **Результат:** товар списан со склада, долг покупателя закрыт

---

# Окружение

**Дата:** 04.10.2026

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
```
---

## Репозиторий

- **GitHub:** *(https://github.com/DanBrosCrut/java-backend-learning)*
- **Структура:** `docs/` (конспекты), `health-service/` (Spring Boot)

---

## Ссылки

- Scrum Guide 2020 (русский): https://scrumguides.org/download.html
- МойСклад: https://www.moysklad.ru/

---

## Spring Boot: health-service

**Дата:** 05.10.2026

### Что сделано

Создан минимальный Spring Boot проект `health-service` с эндпоинтом `GET /health`, возвращающим `ok`. Проект размещён внутри учебного репозитория `java-backend-learning` в папке `health-service/`. Репозиторий связан с GitHub, все изменения запушены.

### Технологии и версии

| Компонент | Версия |
|-----------|--------|
| Spring Boot | 4.1.1 |
| Java | 21.0.2 LTS (Temurin) |
| Maven | 3.9.10 |
| Apache Tomcat | 11.0.24 (embedded) |
| Spring Web | Spring MVC |
| Порт приложения | 8080 |

### Структура проекта
```
java-backend-learning/
├── README.md                    ← этот файл
├── REPORT.md                    ← недельный отчёт
├── .gitignore
├── docs/                        ← конспекты и заметки
│   ├── agile-scrum.md           ← Agile, Scrum, роли, артефакты, DoD
│   ├── environment.md           ← версии Java, Maven, настройка SDKMAN
│   └── moysklad.md              ← МойСклад + онлайн-заказ
└── health-service/              ← минимальный Spring Boot сервис
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    └── src/
        └── main/
            ├── java/com/example/health_service/
            │   ├── HealthServiceApplication.java
            │   └── HealthController.java
            └── resources/
                └── application.properties
```

---

Как собрать

```
cd health-service
mvn clean package
В папке target/ появляется health-service-0.0.1-SNAPSHOT.jar.
```

Как запустить
Вариант 1 — через Maven:

```
cd health-service
mvn spring-boot:run
```

Вариант 2 — через собранный JAR:

```
java -jar target/health-service-0.0.1-SNAPSHOT.jar
```

Ожидаемый вывод в логах:

```
Tomcat started on port 8080 (http) with context path '/'
Started HealthServiceApplication in 1.124 seconds
```

Как проверить
Во втором терминале (первый занят приложением):

```
curl http://localhost:8080/health
```

Ответ:

```
ok
```

Через браузер: http://localhost:8080/health → ok.

Что разобрал по ходу
Spring Boot
Spring Boot — это надстройка над Spring Framework. Убирает boilerplate-конфигурацию через автоконфигурацию и стартеры.

Стартеры (starters) — наборы зависимостей «под задачу». spring-boot-starter-web тянет Spring MVC, Jackson (JSON), validation, embedded Tomcat.

Автоконфигурация — Spring Boot анализирует classpath и настраивает бины сам. Видит Tomcat → поднимает встроенный веб-сервер. Видит Spring MVC → настраивает DispatcherServlet.

Embedded Tomcat — сервер встроен в приложение, не нужен внешний контейнер. Собирается в один JAR, запускается командой java -jar.

@SpringBootApplication — комбинация @Configuration + @EnableAutoConfiguration + @ComponentScan. Точка входа в приложение.

Spring Web MVC
@RestController — комбинация @Controller + @ResponseBody. Возвращаемое значение метода идёт в тело HTTP-ответа, а не в имя view.

@GetMapping("/health") — сокращение от @RequestMapping(method = RequestMethod.GET, value = "/health"). Обрабатывает только GET-запросы по пути /health.

DispatcherServlet — центральный сервлет Spring MVC. Принимает все запросы и маршрутизирует их по контроллерам на основе @RequestMapping.

Maven
Maven lifecycle — последовательность фаз: validate → compile → test → package → verify → install → deploy. Каждая фаза запускает предыдущие.

mvn clean package — удаляет target/, компилирует, прогоняет тесты, собирает JAR.

spring-boot-starter-parent — родительский POM, задаёт версии зависимостей, конфигурацию плагинов, дефолтные настройки.

spring-boot-maven-plugin — плагин, который собирает executable JAR (fat JAR со всеми зависимостями) и предоставляет команду mvn spring-boot:run.

Java-конвенции именования
Пакеты пишутся в нижнем регистре, без дефисов. Spring Initializr автоматически преобразовал artifact id health-service → package health_service.

Package declaration должен совпадать с физическим расположением файла. Если файл лежит в com/example/health_service/, то первая строка — package com.example.health_service;.

###Литература

Прочитано ~6 источников технической документации:

Spring Boot Reference Documentation — https://docs.spring.io/spring-boot/index.html

  Getting Started → Installing Spring Boot

  Getting Started → Developing Your First Spring Boot Application

  Reference → Core Principles (автоконфигурация, стартеры, embedded-сервер)

Spring Framework Reference — Web MVC — https://docs.spring.io/spring-framework/reference/web/webmvc.html

  DispatcherServlet — центральный сервлет Spring MVC

  Annotated Controllers (@RestController, @GetMapping)

Apache Maven — Getting Started — https://maven.apache.org/guides/getting-started/

  Introduction to the Build Lifecycle

  POM Reference (структура pom.xml, parent, dependencies, plugins)

Spring Initializr — https://start.spring.io/

  Разбор параметров генерации: Maven, Java 21, Spring Boot 4.1.1, Spring Web

Apache Tomcat 11 Documentation — https://tomcat.apache.org/tomcat-11.0-doc/index.html

  Архитектура: Connector, Engine, Host, Context

  Embedded Tomcat — запуск внутри приложения

SDKMAN Usage — https://sdkman.io/usage

  Управление версиями Java и Maven