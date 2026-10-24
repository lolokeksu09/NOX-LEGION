# NOX — журнал изменений

## [context-capsule-design] — 2026-10-24 (design only)

- Сохранён NOX `main` на SHA `8a10093f9e587120d878ed359aa6dd645f0716be` в резервной ветке `archive/nox-main-2026-10-24`; существующие feature-ветки не затронуты.
- Добавлена спецификация новой нативной Android-системы памяти проектов CONTEXT CAPSULE: `docs/superpowers/specs/2026-10-24-context-capsule-design.md`.
- Техническое решение: отдельный Android package `dev.lolokeksu.contextcapsule`, публичный GitHub API read-only, локальное хранение решений, формирование Markdown/JSON без платного OpenAI API, ручная отправка в ChatGPT.
- Ветка: `feature/context-capsule-design`. Состояние: ожидает ревью спецификации. NOX `main`, код и CI не менялись. Компиляция APK и тесты устройства не проводились.

> Правило владельца: **каждый патч = коммит в GitHub + обновление документации**. Инструкции: [AGENTS.md](../AGENTS.md), [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md).

## [docs-policy] — 2026-10-23

- Закреплены постоянные правила для разработчиков и AI-агентов в `AGENTS.md`.
- Описан обязательный цикл: проверка состояния → спецификация/тест → изменение → документация → коммит GitHub → CI → ссылка на артефакт только после проверки.
- Файлы: `AGENTS.md`, `docs/DEVELOPMENT_WORKFLOW.md`, `README.md`, `docs/CHANGELOG.md`.
- Коммиты политики: `71f51e7772891885071085ff3880763bbb04e1b4`, `747e4a6d578d5c1da4d4db24983155c145e2cea5`.
- Статус: документы, без изменений Android-кода; APK и аппаратные тесты не применимы.

## История feature-веток

Основная ветка `main` содержит раннюю основу NOX 1.0. Новые версии развиваются в отдельных ветках; не следует считать их опубликованными в `main` без отдельного подтверждённого merge. Для NOX 1.7 см. `feature/nox-1-7-living-scenes` и отдельный `docs/CHANGELOG.md` этой ветки.
