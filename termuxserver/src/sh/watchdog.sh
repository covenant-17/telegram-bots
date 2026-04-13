#!/data/data/com.termux/files/usr/bin/bash
# Watchdog for manager-bot: restarts it if the process dies.
# Usage: run once in background via run_bot.sh or tmux.
#   nohup bash ~/termuxserver/src/sh/watchdog.sh >> ~/termuxserver/src/sh/logs/watchdog.log 2>&1 &

JAR="/data/data/com.termux/files/home/termuxserver/src/manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"
LOG="$LOG_DIR/manager-bot.log"
ERR="$LOG_DIR/manager-bot-error.log"
PATTERN="manager-bot"
CHECK_INTERVAL=30  # seconds between checks

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [watchdog] $1"
}

log "Watchdog started (PID $$)."

while true; do
    PIDS=$(ps aux | grep 'java -jar' | grep "$PATTERN" | grep -v grep | awk '{print $2}')
    if [ -z "$PIDS" ]; then
        log "manager-bot is DOWN. Restarting..."
        nohup java -jar "$JAR" >> "$LOG" 2>> "$ERR" &
        NEW_PID=$!
        log "Started manager-bot with PID $NEW_PID."
        sleep 10  # give it time to start before next check
    fi
    sleep "$CHECK_INTERVAL"
done
