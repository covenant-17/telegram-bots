#!/data/data/com.termux/files/usr/bin/bash
# Script for running both converter-bot and fileconverter bots in Termux

# Kill previous bot processes using external script
. ~/termuxserver/src/sh/kill_bots.sh
sleep 2

# SIGINT handler (CTRL+C)
trap '. ~/termuxserver/src/sh/kill_bots.sh; exit' INT

# Log directory
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"
mkdir -p "$LOG_DIR"

# Load .env for trace-keeper
ENV_FILE="/data/data/com.termux/files/home/termuxserver/src/.env"
if [ -f "$ENV_FILE" ]; then
    set -a; source "$ENV_FILE"; set +a
fi

# converter bot
CONVERTER_JAR="/data/data/com.termux/files/home/termuxserver/src/converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
CONVERTER_LOG="$LOG_DIR/converter-bot.log"
CONVERTER_ERR="$LOG_DIR/converter-bot-error.log"
# # rt-file-converter-bot disabled, moved to Serpstat
# FILECONVERTER_JAR="/data/data/com.termux/files/home/termuxserver/src/rt-file-converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
# FILECONVERTER_LOG="$LOG_DIR/rt-file-converter-bot.log"
# FILECONVERTER_ERR="$LOG_DIR/rt-file-converter-bot-error.log"
# youtube-mp3-downloader bot
YOUTUBE_MP3_DOWNLOADER_JAR="/data/data/com.termux/files/home/termuxserver/src/youtube-mp3-downloader-1.0-SNAPSHOT-jar-with-dependencies.jar"
YOUTUBE_MP3_DOWNLOADER_LOG="$LOG_DIR/youtube-mp3-downloader.log"
YOUTUBE_MP3_DOWNLOADER_ERR="$LOG_DIR/youtube-mp3-downloader-error.log"
# manager bot
MANAGER_BOT_JAR="/data/data/com.termux/files/home/termuxserver/src/manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
MANAGER_BOT_LOG="$LOG_DIR/manager-bot.log"
MANAGER_BOT_ERR="$LOG_DIR/manager-bot-error.log"
# trace-keeper (runs inside proot-distro ubuntu — requires glibc + libssl3)
TRACE_KEEPER_JAR="/data/data/com.termux/files/home/termuxserver/src/trace-keeper-1.0.0-SNAPSHOT.jar"
TRACE_KEEPER_LOG="$LOG_DIR/trace-keeper.log"
TRACE_KEEPER_ERR="$LOG_DIR/trace-keeper-error.log"
APP_HOME="/data/data/com.termux/files/home"

# Starting the bots
echo "Starting converter-bot..." >> "$CONVERTER_LOG"
java -jar "$CONVERTER_JAR" >> "$CONVERTER_LOG" 2>> "$CONVERTER_ERR" &
# echo "Starting fileconverter..." >> "$FILECONVERTER_LOG" #disabled, moved to Serpstat
# java -jar "$FILECONVERTER_JAR" >> "$FILECONVERTER_LOG" 2>> "$FILECONVERTER_ERR" &
echo "Starting youtube-mp3-downloader..." >> "$YOUTUBE_MP3_DOWNLOADER_LOG"
java -jar "$YOUTUBE_MP3_DOWNLOADER_JAR" >> "$YOUTUBE_MP3_DOWNLOADER_LOG" 2>> "$YOUTUBE_MP3_DOWNLOADER_ERR" &
WATCHDOG_LOG="$LOG_DIR/watchdog.log"
echo "Starting manager-bot watchdog..." >> "$WATCHDOG_LOG"
nohup bash ~/termuxserver/src/sh/watchdog.sh >> "$WATCHDOG_LOG" 2>&1 &

# trace-keeper (via proot-distro ubuntu for glibc/libssl3 support)
echo "Starting trace-keeper..." >> "$TRACE_KEEPER_LOG"
nohup proot-distro login ubuntu -- \
  env TDLIB_API_ID="$TDLIB_API_ID" TDLIB_API_HASH="$TDLIB_API_HASH" \
  TELEGRAM_BOT_TOKEN="$TELEGRAM_BOT_TOKEN" \
  TELEGRAM_NOTIFY_CHAT_ID="$TELEGRAM_NOTIFY_CHAT_ID" \
  TELEGRAM_ADMIN_USER_ID="$TELEGRAM_ADMIN_USER_ID" \
  java -Dapp.home="$APP_HOME" -jar "$TRACE_KEEPER_JAR" \
  >> "$TRACE_KEEPER_LOG" 2>> "$TRACE_KEEPER_ERR" &

# Wait for all background processes to finish
exit 0

#bash ~/termuxserver/src/sh/run_bot.sh
#bash ~/termuxserver/src/sh/log_clearer.sh
#bash ~/termuxserver/src/sh/kill_bots.sh
#tmux new -s mybot
#tmux attach -t mybot
#bash ~/termuxserver/src/sh/sanitize_mp3.sh termuxserver/youtube_mp3_downloader_workzone .mp3 true
