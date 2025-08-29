#!/data/data/com.termux/files/usr/bin/bash
# Kill Java processes for converter-bot, rt-file-converter-bot, and youtube-mp3-downloader
LOGS_DIR="/data/data/com.termux/files/home/termuxserver/logs"
KILL_LOG="$LOGS_DIR/killed_processes.log"

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Add timestamp to log entries
timestamp=$(date '+%Y-%m-%d %H:%M:%S')

RUNNING_PIDS=$(ps aux | grep -E 'converter-bot|youtube-mp3-downloader' | grep 'java -jar' | grep -v grep | awk '{print $2}')
for pid in $RUNNING_PIDS; do 
    if [ "$pid" != "$$" ]; then 
        echo "[$timestamp] Killing process with PID $pid" >> "$KILL_LOG"
        kill -9 $pid
    fi
done