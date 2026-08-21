# CODEX — Руководство по проекту telegram-bots

Этот файл собирает ключевую информацию и команды, которые я (ассистент) знаю о проекте и окружении, связанном с репозиторием.

## Структура репозитория (важные папки)

- my-personal-telegram-bots/
  - converter-bot/
  - youtube-mp3-downloader/
- manager-bot/
- shared-config/
- termuxserver/
  - src/sh/ (скрипты для запуска на сервере)
- tools/

## Основные команды сборки

Сборка модулей выполняется через Maven из корня соответствующего модуля.

- Сборка `converter-bot`:

```
mvn clean package
```

Запускать из: `my-personal-telegram-bots/converter-bot`

- Сборка `youtube-mp3-downloader`:

```
mvn clean package
```

Запускать из: `my-personal-telegram-bots/youtube-mp3-downloader`

Дополнительно в VS Code есть готовые задачи сборки (Tasks):

- `Maven: Package Uber Jar (converter-bot)` — запускает `mvn clean package` в `my-personal-telegram-bots/converter-bot`
- `Maven: Package Uber Jar (rt-file-converter-bot)` — похожая задача для `rt-file-converter-bot` (если есть)
- `Maven: Package Uber Jar (youtube-mp3-downloader)` — для `my-personal-telegram-botS/youtube-mp3-downloader`

## Запуск и утилиты на сервере (termuxserver)

В каталоге `termuxserver/src/sh/` находятся вспомогательные скрипты для запуска ботов на сервере:

- `run_bot.sh` — запуск бота
- `watchdog.sh` — наблюдение и перезапуск
- `kill_bots.sh` — остановка ботов
- `log_clearer.sh` — очистка логов
- `setup-git-access.sh` — настройка git-доступа
- `sanitize_mp3.sh` — постобработка mp3

Пример запуска (ssh + запуск скрипта):

```
ssh -i "<SSH_KEY_PATH>" <SFTP_USER>@<SFTP_HOST> -p <SFTP_PORT> "bash /path/to/termuxserver/src/sh/run_bot.sh"
```

## Доступ к серверу

Конкретные реквизиты доступа не хранятся в репозитории. Подставляйте их из
локальной конфигурации или переменных окружения:

- Host: `<SFTP_HOST>`
- Port: `<SFTP_PORT>`
- User: `<SFTP_USER>`
- Private key: `<SSH_KEY_PATH>`

Key-based пример (Windows PowerShell):

```
ssh -i "<SSH_KEY_PATH>" -o StrictHostKeyChecking=no <SFTP_USER>@<SFTP_HOST> -p <SFTP_PORT> "command"
```

Key-based пример (Git Bash / bash):

```
ssh -i "<SSH_KEY_PATH>" -o StrictHostKeyChecking=no <SFTP_USER>@<SFTP_HOST> -p <SFTP_PORT> "command"
```

> Пароли и приватные ключи нельзя добавлять в репозиторий, даже в примеры.

## Файлы конфигурации и примеры

- Многие артефакты конфигурации находятся в `target/classes/` при сборке модулей, например:
  - `bot_texts.json`, `config.properties.example`
- В `manager-bot/target/classes/` есть `config.properties` и `config.properties.example`.

## Тесты

- В проектах присутствуют unit-тесты и отчёты `surefire-reports/`.

## Полезные заметки по рабочей среде

- Рабочая директория (workspace): `<WORKSPACE>/telegram-bots`
- В проекте есть несколько модуля/проектов Maven — пользуйтесь `mvn` из корня нужного модуля.
- В VS Code удобнее запускать готовые Tasks, перечисленные выше.

## Контакты/авторство

Этот файл сгенерирован ассистентом по запросу — содержит собранные мной знания о проекте и среде. Обновляйте при изменениях инфраструктуры или учёте новых секретов.

---

Файл создан автоматически и может быть отредактирован вручную для добавления дополнительных инструкций.
