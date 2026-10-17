# Git Workflow

Документ описывает правила работы с Git в проекте и объясняет, когда использовать `merge`, `rebase` и `cherry-pick`.

## Основные правила проекта

- **Не пушить в `main` напрямую.** Ветка `main` защищена: включены Require pull request before merging и Require approvals.
- **Каждая фича — отдельная ветка.** Имя ветки: `feature/<краткое-название>`, `docs/<...>`, `fix/<...>`, `chore/<...>`.
- **Коммиты по Conventional Commits.** Шаблон — `.gitmessage` в корне репозитория.
- **Каждый коммит ссылается на Issue.** В теле коммита: `Refs: #<номер>`.
- **Перед PR — обновить ветку от `main`** через `rebase`.
- **Merge — только через Pull Request** и после approve.

## Когда merge

`git merge` — слияние двух веток с созданием merge-коммита. Сохраняет историю обеих веток.

**Когда использовать:**
- Слияние feature-ветки в `main` через PR (делает GitHub автоматически).
- Слияние `main` в свою ветку, если нужно подтянуть свежие изменения и ты не хочешь переписывать историю.

**Пример:**
```bash
git checkout main
git pull
git merge feature/products-endpoint
```
Плюсы:
История сохраняется полностью.
Безопасно для публичных веток.

Минусы:
История становится «ветвистой» — сложнее читать.
Много merge-коммитов, если делать часто.

Когда rebase
```bash
git rebase — перенос коммитов одной ветки поверх другой. Переписывает историю.
```
Когда использовать:
```bash
Обновить свою feature-ветку от main перед PR: git pull --rebase origin main.
Привести историю к линейному виду перед merge.
```
Пример:

```bash
git checkout feature/products-endpoint
git fetch origin
git rebase origin/main
```
Плюсы:

Линейная, чистая история.

Легче читать git log.

Риски:

Переписывает историю — нельзя делать на ветках, с которыми работают другие.
После rebase нужен git push --force-with-lease (не --force!).
Конфликты приходится решать по одному коммиту.
Правило: rebase — только для своей локальной ветки, до её пуша в общий доступ.

Когда cherry-pick
```bash
git cherry-pick — перенос одного конкретного коммита из одной ветки в другую.
```

Когда использовать:
```bash
Перенести hotfix из main в release.
Забрать один полезный коммит из чужой ветки, не сливая её целиком.
Восстановить случайно потерянный коммит.
```
Пример:

bash
git checkout main
git cherry-pick a1b2c3d
Плюсы:

Точечный перенос — не тянет за собой всю ветку.

Полезно для hotfix.

Риски:
```
Создаёт дубликаты коммитов — один и тот же код в двух коммитах с разными хешами.
При последующем merge может вызвать конфликты.
Легко забыть, что коммит уже был где-то.
```
Практика на учебных ветках
Показано руками (см. скриншоты/лог в отчёте):

Merge
```bash
git checkout main
git checkout -b test-merge
echo "test" > test-merge.txt
git add . && git commit -m "test: merge demo"
git checkout main
git merge test-merge
Rebase
```
```bash
git checkout main
git checkout -b test-rebase
echo "test" > test-rebase.txt
git add . && git commit -m "test: rebase demo"
git checkout main
git checkout -b test-rebase-target
echo "main change" > main.txt
git add . && git commit -m "test: main change"
git checkout test-rebase
git rebase test-rebase-target
Cherry-pick
```
```bash
git checkout main
git checkout -b test-cherry-pick
echo "cherry" > cherry.txt
git add . && git commit -m "test: cherry-pick source"
git log --oneline -1   # запомнить хеш
git checkout main
git cherry-pick <хеш>
```
Соглашение о коммитах
Проект использует Conventional Commits. Шаблон — .gitmessage в корне.

Подключение:

```bash
git config commit.template .gitmessage
```
Формат:
```text
<тип>(<область>): <краткое описание>
<зачем сделано>
Refs: #<issues>
```
Типы: feat, fix, docs, style, refactor, perf, test, build, ci, chore.

Правила:
Первая строка ≤ 72 символов.
Императив: «add», «fix», «update» (не «added», «fixes»).
Ссылки на Issues — в Refs:.
Не коммитить секреты и target/.