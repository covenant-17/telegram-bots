#!/data/data/com.termux/files/usr/bin/bash
# Script for bulk renaming mp3 files via FileNameSanitizerCli
# Usage: ./sanitize_mp3.sh <folder> [extension] [dryRun]
# Example: ./sanitize_mp3.sh termuxserver/youtube_mp3_downloader_workzone .mp3 true

JAR="$HOME/termuxserver/src/youtube-mp3-downloader-1.0-SNAPSHOT-jar-with-dependencies.jar"
MAIN_CLASS="dev.telegrambots.youtubemp3downloader.FileNameSanitizerCli"

if [ $# -lt 1 ]; then
  echo "Usage: $0 <dir> [ext] [dryRun]"
  exit 1
fi

java -cp "$JAR" $MAIN_CLASS "$@"
