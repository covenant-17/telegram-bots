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
ssh -i "/path/to/key" user@server -p 8022 "bash /path/to/termuxserver/src/sh/run_bot.sh"
```

## Доступ к серверу (известная информация)

Информация взята из моих сохранённых данных для этой сессии:

- Host: 192.168.50.89
- Port: 8022
- User: username
- Password: ruby

Key-based пример (Windows PowerShell):

```
ssh -i "$env:USERPROFILE\.ssh\termux_deploy" -o StrictHostKeyChecking=no username@192.168.50.89 -p 8022 "command"
```

Key-based пример (Git Bash / bash):

```
ssh -i "/c/Users/LeRuby/.ssh/termux_deploy" -o StrictHostKeyChecking=no username@192.168.50.89 -p 8022 "command"
```

> Внимание: пароли и приватные ключи — чувствительные данные. Убедитесь, что доступ к этому файлу контролируется согласно вашей политике безопасности.

## Файлы конфигурации и примеры

- Многие артефакты конфигурации находятся в `target/classes/` при сборке модулей, например:
  - `bot_texts.json`, `config.properties.example`
- В `manager-bot/target/classes/` есть `config.properties` и `config.properties.example`.

## Тесты

- В проектах присутствуют unit-тесты и отчёты `surefire-reports/`.

## Полезные заметки по рабочей среде

- Рабочая директория (workspace): `C:/!Dev/telegram-bots`
- В проекте есть несколько модуля/проектов Maven — пользуйтесь `mvn` из корня нужного модуля.
- В VS Code удобнее запускать готовые Tasks, перечисленные выше.

## Контакты/авторство

Этот файл сгенерирован ассистентом по запросу — содержит собранные мной знания о проекте и среде. Обновляйте при изменениях инфраструктуры или учёте новых секретов.

---

Файл создан автоматически и может быть отредактирован вручную для добавления дополнительных инструкций.
