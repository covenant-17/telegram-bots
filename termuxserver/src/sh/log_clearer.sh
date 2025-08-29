#!/data/data/com.termux/files/usr/bin/bash
# Clears all logs in LOG_DIR directory
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"

if [ -d "$LOG_DIR" ]; then
  find "$LOG_DIR" -type f -name "*.log" -exec truncate -s 0 {} \;
  echo "Logs cleared in $LOG_DIR"
else
  echo "Log directory not found: $LOG_DIR"
fi
