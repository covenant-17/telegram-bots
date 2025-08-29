#!/data/data/com.termux/files/usr/bin/bash
# Script for running both converter-bot and fileconverter bots in Termux

# Kill previous bot processes using external script
. ~/termuxserver/src/sh/kill_bots.sh
sleep 2

# Обработчик SIGINT (CTRL+C)
trap '. ~/termuxserver/src/sh/kill_bots.sh; exit' INT

# Лог-папка
LOG_DIR="/data/data/com.termux/files/home/termuxserver/src/sh/logs"
mkdir -p "$LOG_DIR"

# converter bot  
CONVERTER_JAR="/data/data/com.termux/files/home/termuxserver/src/converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
CONVERTER_LOG="$LOG_DIR/converter-bot.log"
CONVERTER_ERR="$LOG_DIR/converter-bot-error.log"
# # rt-file-converter-bot потушил, переехал жить в Серпстат.
# FILECONVERTER_JAR="/data/data/com.termux/files/home/termuxserver/src/rt-file-converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"
# FILECONVERTER_LOG="$LOG_DIR/rt-file-converter-bot.log"
# FILECONVERTER_ERR="$LOG_DIR/rt-file-converter-bot-error.log"
# youtube-mp3-downloader bot
YOUTUBE_MP3_DOWNLOADER_JAR="/data/data/com.termux/files/home/termuxserver/src/youtube-mp3-downloader-1.0-SNAPSHOT-jar-with-dependencies.jar"
YOUTUBE_MP3_DOWNLOADER_LOG="$LOG_DIR/youtube-mp3-downloader.log"
YOUTUBE_MP3_DOWNLOADER_ERR="$LOG_DIR/youtube-mp3-downloader-error.log"

# Starting the bots
echo "Starting converter-bot..." >> "$CONVERTER_LOG"
java -jar "$CONVERTER_JAR" >> "$CONVERTER_LOG" 2>> "$CONVERTER_ERR" &
# echo "Starting fileconverter..." >> "$FILECONVERTER_LOG" #потушил, переехал жить в Серпстат.
# java -jar "$FILECONVERTER_JAR" >> "$FILECONVERTER_LOG" 2>> "$FILECONVERTER_ERR" &
echo "Starting youtube-mp3-downloader..." >> "$YOUTUBE_MP3_DOWNLOADER_LOG"
java -jar "$YOUTUBE_MP3_DOWNLOADER_JAR" >> "$YOUTUBE_MP3_DOWNLOADER_LOG" 2>> "$YOUTUBE_MP3_DOWNLOADER_ERR" &

# Wait for all background processes to finish
exit 0

#bash ~/termuxserver/src/sh/run_bot.sh
#bash ~/termuxserver/src/sh/log_clearer.sh
#bash ~/termuxserver/src/sh/kill_bots.sh
#tmux new -s mybot
#tmux attach -t mybot
#bash ~/termuxserver/src/sh/sanitize_mp3.sh termuxserver/youtube_mp3_downloader_workzone .mp3 true