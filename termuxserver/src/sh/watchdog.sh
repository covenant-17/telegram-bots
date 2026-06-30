#!/data/data/com.termux/files/usr/bin/bash
# Watchdog for manager-bot and trace-keeper: restarts them if the process dies.
# Usage: run once in background via run_bot.sh or tmux.
#   nohup bash ~/termuxserver/src/sh/watchdog.sh >> ~/termuxserver/src/sh/logs/watchdog.log 2>&1 &

MANAGER_JAR="/data/data/com.termux/files/home/termuxserver/src/manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"
TRACE_START="/data/data/com.termux/files/home/termuxserver/src/sh/start-trace-keeper.sh"
MANAGER_LOG="$LOG_DIR/manager-bot.log"
MANAGER_ERR="$LOG_DIR/manager-bot-error.log"
TRACE_LOG="$LOG_DIR/trace-keeper.log"
TRACE_ERR="$LOG_DIR/trace-keeper-error.log"
CHECK_INTERVAL=30  # seconds between checks

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [watchdog] $1"
}

log "Watchdog started (PID $$)."

while true; do
    # --- manager-bot ---
    PIDS=$(ps aux | grep 'java -jar' | grep "manager-bot" | grep -v grep | awk '{print $2}')
    if [ -z "$PIDS" ]; then
        log "manager-bot is DOWN. Restarting..."
        nohup java -jar "$MANAGER_JAR" >> "$MANAGER_LOG" 2>> "$MANAGER_ERR" &
        log "Started manager-bot with PID $!."
        sleep 10
    fi

    # --- trace-keeper ---
    TK_PIDS=$(ps aux | grep 'java' | grep "trace-keeper" | grep -v grep | awk '{print $2}')
    if [ -z "$TK_PIDS" ]; then
        log "trace-keeper is DOWN. Restarting..."
        nohup bash "$TRACE_START" >> "$TRACE_LOG" 2>> "$TRACE_ERR" &
        log "Started trace-keeper with PID $!."
        sleep 10
    fi

    sleep "$CHECK_INTERVAL"
done
