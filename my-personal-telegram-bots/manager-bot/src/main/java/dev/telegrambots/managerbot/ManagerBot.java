package dev.telegrambots.managerbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

/**
 * Manager Bot — controls all other bots running on Termux.
 *
 * Commands:
 *   /status              — show all apps + alive/dead state
 *   /rebuild <app>       — git pull + mvn package + restart app
 *   /kill <app>          — kill running process
 *   /start <app>         — start jar (without rebuild)
 *   /restart <app>       — kill + start (without rebuild)
 *   /logs <app> [N]      — last N lines of app log (default 1000)
 *   /clearlogs <app>     — truncate both log files for an app
 *   /apps                — list all apps with ready-to-use commands
 *   /help                — list commands
 */
public class ManagerBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(ManagerBot.class);
    private static final String HOME = "/data/data/com.termux/files/home";

    private final BotConfig config;

    private static String getBotTokenFromConfig() {
        try {
            return java.util.ResourceBundle.getBundle("config").getString("bot.token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load bot token from config", e);
        }
    }

    public ManagerBot() {
        super(getBotTokenFromConfig());
        this.config = new BotConfig();
    }

    public void notifyStartup() {
        for (long userId : config.allowedUserIds) {
            send(userId, "✅ manager-bot is up and running.");
        }
    }

    @Override
    public String getBotUsername() {
        return config.botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        long userId = message.getFrom().getId();
        long chatId = message.getChatId();

        if (!config.isAllowed(userId)) {
            return;
        }

        String text = message.getText().trim();
        String[] parts = text.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "/status" -> handleStatus(chatId);
                case "/rebuild" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /rebuild <app>"); return; }
                    handleRebuild(chatId, parts[1].toLowerCase());
                }
                case "/kill" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /kill <app|PID>"); return; }
                    handleKill(chatId, parts[1]);
                }
                case "/start" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /start <app>"); return; }
                    handleStart(chatId, parts[1].toLowerCase());
                }
                case "/restart" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /restart <app>"); return; }
                    handleRestart(chatId, parts[1].toLowerCase());
                }
                case "/logs" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /logs <app> [N]"); return; }
                    int lines = (parts.length >= 3) ? parseIntSafe(parts[2], 1000) : 1000;
                    handleLogs(chatId, parts[1].toLowerCase(), lines);
                }
                case "/clearlogs" -> {
                    if (parts.length < 2) { send(chatId, "Usage: /clearlogs <app>"); return; }
                    handleClearLogs(chatId, parts[1].toLowerCase());
                }
                case "/apps" -> handleApps(chatId);
                case "/help" -> handleHelp(chatId);
                default -> send(chatId, "Unknown command. Use /help.");
            }
        } catch (Exception e) {
            logger.error("Error handling command '{}': {}", command, e.getMessage(), e);
            send(chatId, "❌ Internal error: " + e.getMessage());
        }
    }

    // ─── Handlers ────────────────────────────────────────────────────────────

    private void handleStatus(long chatId) {
        StringBuilder sb = new StringBuilder("📊 *App Status*\n\n");
        for (Map.Entry<String, AppDefinition> entry : AppRegistry.all().entrySet()) {
            AppDefinition app = entry.getValue();
            List<String> pids = ShellRunner.findPids(app.processPattern);
            String status = pids.isEmpty() ? "💀 dead" : "✅ alive (pids: " + String.join(", ", pids) + ")";
            sb.append("• *").append(app.name).append("*: ").append(status).append("\n");
        }
        send(chatId, sb.toString().trim());
    }

    private void handleRebuild(long chatId, String appName) {
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        send(chatId, "🔄 Rebuilding *" + app.name + "*…");

        // 1. git pull (or clone if repo not present)
        java.io.File repoDir = new java.io.File(app.repoPath);
        if (!repoDir.exists()) {
            if (app.repoUrl == null) {
                send(chatId, "❌ Repo directory not found: `" + app.repoPath + "`\nNo repoUrl configured — clone manually.");
                return;
            }
            send(chatId, "📥 Repo not found — cloning…");
            // Clone into parent dir with the last path segment as target
            String parentPath = repoDir.getParent();
            String cloneCmd = "mkdir -p \"" + parentPath + "\" && git clone \"" + app.repoUrl + "\" \"" + app.repoPath + "\"";
            ShellResult clone = ShellRunner.run(cloneCmd, null);
            if (!clone.isSuccess()) {
                send(chatId, "❌ git clone failed:\n```\n" + truncate(clone.combined(), 800) + "\n```");
                return;
            }
            send(chatId, "✅ git clone OK");
        } else {
            send(chatId, "📥 git pull…");
            ShellResult pull = ShellRunner.run("git pull", app.repoPath);
            if (!pull.isSuccess()) {
                send(chatId, "❌ git pull failed:\n```\n" + truncate(pull.combined(), 800) + "\n```");
                return;
            }
            send(chatId, "✅ git pull OK\n" + truncate(pull.stdout, 200));
        }

        // 2. Pre-build step (e.g. shared-config install)
        if (app.preBuildSubPath != null) {
            String preBuildDir = app.repoPath + "/" + app.preBuildSubPath;
            send(chatId, "⚙️ pre-build: mvn install (shared-config)…");
            ShellResult pre = ShellRunner.run("mvn install -B -DskipTests -q", preBuildDir);
            if (!pre.isSuccess()) {
                send(chatId, "❌ Pre-build failed:\n```\n" + truncate(pre.combined(), 800) + "\n```");
                return;
            }
        }

        // 3. Main build
        String buildDir = app.buildSubPath.isBlank()
                ? app.repoPath
                : app.repoPath + "/" + app.buildSubPath;
        send(chatId, "🔨 mvn package…");
        ShellResult build = ShellRunner.run("mvn clean package -B -DskipTests -q", buildDir);
        if (!build.isSuccess()) {
            send(chatId, "❌ Build failed:\n```\n" + truncate(build.combined(), 1000) + "\n```");
            return;
        }
        send(chatId, "✅ Build OK");

        // 4. Copy jar to deploy location
        ShellResult copyResult = copyJar(app, buildDir);
        if (!copyResult.isSuccess()) {
            send(chatId, "❌ Copy jar failed:\n```\n" + truncate(copyResult.combined(), 800) + "\n```");
            return;
        }

        // 5. Restart (self-rebuild: launch new process first, then exit)
        if (app.name.equals("manager-bot")) {
            doSelfRestart(chatId, app);
        } else {
            doRestart(chatId, app);
        }
    }

    private void handleKill(long chatId, String arg) {
        if (arg.matches("\\d+")) {
            boolean ok = ShellRunner.killByPid(arg);
            if (ok) {
                send(chatId, "🛑 Killed PID " + arg + ".");
            } else {
                send(chatId, "❌ Failed to kill PID " + arg + " (no such process?).");
            }
            return;
        }
        String appName = arg.toLowerCase();
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        int killed = ShellRunner.killByPattern(app.processPattern);
        if (killed == 0) {
            send(chatId, "ℹ️ *" + app.name + "* was not running.");
        } else {
            send(chatId, "🛑 Killed " + killed + " process(es) for *" + app.name + "*.");
        }
    }

    private void handleStart(long chatId, String appName) {
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        doStart(chatId, app);
    }

    private void handleRestart(long chatId, String appName) {
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        doRestart(chatId, app);
    }

    private void handleLogs(long chatId, String appName, int lines) {
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        ShellResult out = ShellRunner.tail(app.logPath, lines);
        ShellResult err = ShellRunner.tail(app.errLogPath, lines);

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(app.name).append(" — last ").append(lines).append(" lines\n\n");
        sb.append("## stdout\n\n");
        sb.append("```\n").append(out.stdout.isBlank() ? "(empty)" : out.stdout).append("\n```\n");
        if (!err.stdout.isBlank()) {
            sb.append("\n## stderr\n\n");
            sb.append("```\n").append(err.stdout).append("\n```\n");
        }

        String filename = app.name + "-logs.md";
        sendDocument(chatId, filename, sb.toString(), "📄 " + app.name + " — last " + lines + " lines");
    }

    private void handleClearLogs(long chatId, String appName) {
        if (!AppRegistry.exists(appName)) {
            send(chatId, unknownApp(appName)); return;
        }
        AppDefinition app = AppRegistry.get(appName);
        ShellResult r = ShellRunner.run(
                "truncate -s 0 \"" + app.logPath + "\" && truncate -s 0 \"" + app.errLogPath + "\"", null);
        if (r.isSuccess()) {
            send(chatId, "🧹 Logs cleared for *" + app.name + "*.");
        } else {
            send(chatId, "❌ Failed to clear logs:\n```\n" + truncate(r.combined(), 400) + "\n```");
        }
    }

    private void handleApps(long chatId) {
        StringBuilder sb = new StringBuilder("📋 *Apps & Ready Commands*\n");
        for (Map.Entry<String, AppDefinition> entry : AppRegistry.all().entrySet()) {
            String name = entry.getValue().name;
            sb.append("\n*").append(name).append("*\n");
            sb.append("/status\n");
            sb.append("/rebuild ").append(name).append("\n");
            sb.append("/kill ").append(name).append("\n");
            sb.append("/start ").append(name).append("\n");
            sb.append("/restart ").append(name).append("\n");
            sb.append("/logs ").append(name).append("\n");
            sb.append("/clearlogs ").append(name).append("\n");
        }
        send(chatId, sb.toString().trim());
    }

    private void handleHelp(long chatId) {
        String help = """
                🤖 *Manager Bot — Commands*

                /status — show all apps (alive/dead)
                /rebuild <app> — git pull + build + restart
                /kill <app|PID> — stop running process
                /start <app> — launch jar (no rebuild)
                /restart <app> — kill + start (no rebuild)
                /logs <app> [N] — last N log lines (default 1000, sent as .md file)
                /clearlogs <app> — truncate log files for an app
                /apps — list all apps with ready commands
                /help — this message

                *Apps:* converter-bot, youtube-mp3-downloader, trace-keeper, manager-bot
                """;
        send(chatId, help);
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private void doStart(long chatId, AppDefinition app) {
        List<String> pids = ShellRunner.findPids(app.processPattern);
        if (!pids.isEmpty()) {
            send(chatId, "ℹ️ *" + app.name + "* is already running (pids: " + String.join(", ", pids) + ").");
            return;
        }
        ShellRunner.runDetached("java -jar " + app.jarPath, app.logPath);
        send(chatId, "🚀 Started *" + app.name + "*.");
    }

    private void doRestart(long chatId, AppDefinition app) {
        int killed = ShellRunner.killByPattern(app.processPattern);
        if (killed > 0) {
            send(chatId, "🛑 Stopped " + killed + " old process(es).");
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }
        ShellRunner.runDetached("java -jar " + app.jarPath, app.logPath);
        send(chatId, "🚀 *" + app.name + "* restarted.");
    }

    /**
     * Self-rebuild restart: schedules a new process that first kills this JVM by PID,
     * then starts the updated jar. More reliable than System.exit(0) since shutdown
     * hooks from the Telegram library can hang and leave the old process alive.
     */
    private void doSelfRestart(long chatId, AppDefinition app) {
        long myPid = ProcessHandle.current().pid();
        // Kill old JVM by PID, wait a moment, then start new one
        String launchCmd = "sleep 2 && kill -9 " + myPid + " 2>/dev/null; sleep 1 && nohup java -jar "
                + app.jarPath + " >> " + app.logPath + " 2>> " + app.errLogPath + " &";
        ShellRunner.runDetached("bash -c '" + launchCmd + "'", "/dev/null");
        send(chatId, "♻️ *manager-bot* rebuilt. Restarting now… I'll be back in a few seconds.");
    }

    /**
     * Copies the built uber-jar from the Maven target directory to the deploy location.
     */
    private ShellResult copyJar(AppDefinition app, String buildDir) {
        // Prioritize the uber-jar produced by maven-assembly-plugin
        String findUber = "find " + buildDir + "/target -maxdepth 1 -name '*-jar-with-dependencies.jar' | head -n 1";
        ShellResult found = ShellRunner.run(findUber, null);
        if (!found.isSuccess() || found.stdout.isBlank()) {
            // Fallback: any jar except sources/javadoc
            String findJar = "find " + buildDir + "/target -maxdepth 1 -name '*.jar' "
                    + "! -name '*-sources.jar' ! -name '*-javadoc.jar' "
                    + "| head -n 1";
            found = ShellRunner.run(findJar, null);
        }
        if (found.stdout.isBlank()) {
            return new ShellResult(-1, "", "No jar found in " + buildDir + "/target");
        }
        String sourceJar = found.stdout.trim();
        return ShellRunner.run("cp \"" + sourceJar + "\" \"" + app.jarPath + "\"", null);
    }

    private void sendDocument(long chatId, String filename, String content, String caption) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            InputFile inputFile = new InputFile(new ByteArrayInputStream(bytes), filename);
            SendDocument doc = new SendDocument();
            doc.setChatId(String.valueOf(chatId));
            doc.setDocument(inputFile);
            doc.setCaption(caption);
            execute(doc);
        } catch (Exception e) {
            logger.error("Failed to send document: {}", e.getMessage());
        }
    }

    private void send(long chatId, String text) {
        try {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(text);
            msg.setParseMode("Markdown");
            execute(msg);
        } catch (Exception e) {
            logger.error("Failed to send message: {}", e.getMessage());
        }
    }

    private String unknownApp(String name) {
        return "❓ Unknown app: *" + name + "*\nKnown apps: " + String.join(", ", AppRegistry.all().keySet());
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(s.length() - maxLen);
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
