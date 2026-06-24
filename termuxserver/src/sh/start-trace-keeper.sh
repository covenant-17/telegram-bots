#!/data/data/com.termux/files/usr/bin/bash
# Wrapper to launch trace-keeper inside proot-distro ubuntu (required for libssl.so.3)

ENV_FILE="/data/data/com.termux/files/home/termuxserver/src/.env"
APP_HOME="/data/data/com.termux/files/home"
TRACE_JAR="/data/data/com.termux/files/home/termuxserver/src/trace-keeper-1.0.0-SNAPSHOT.jar"

if [ -f "$ENV_FILE" ]; then
    set -a; source "$ENV_FILE"; set +a
fi

exec proot-distro login ubuntu --kernel 5.4.0 -- \
  env TDLIB_API_ID="$TDLIB_API_ID" \
  TDLIB_API_HASH="$TDLIB_API_HASH" \
  TELEGRAM_BOT_TOKEN="$TELEGRAM_BOT_TOKEN" \
  TELEGRAM_NOTIFY_CHAT_ID="$TELEGRAM_NOTIFY_CHAT_ID" \
  TELEGRAM_ADMIN_USER_ID="$TELEGRAM_ADMIN_USER_ID" \
  java -Dapp.home="$APP_HOME" -jar "$TRACE_JAR"
