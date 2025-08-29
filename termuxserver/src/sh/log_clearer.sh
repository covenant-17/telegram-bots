#!/data/data/com.termux/files/usr/bin/bash
# Очищает все логи в директории LOG_DIR
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"

if [ -d "$LOG_DIR" ]; then
  find "$LOG_DIR" -type f -name "*.log" -exec truncate -s 0 {} \;
  echo "Логи очищены в $LOG_DIR"
else
  echo "Директория логов не найдена: $LOG_DIR"
fi
