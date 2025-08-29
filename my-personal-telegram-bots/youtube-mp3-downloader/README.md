# YouTube MP3 Downloader Bot

Telegram bot for downloading YouTube videos and converting them to MP3 format with advanced features and parallel processing.

## Features

- Download YouTube videos as MP3
- Parallel download support (up to 3 concurrent downloads)
- File size validation (max 50MB)
- Duration validation (max 10 minutes)
- Automatic file cleanup
- Cross-platform support (Windows/Linux)
- Advanced filename sanitization
- Progress tracking and error handling
- Comprehensive test coverage with JaCoCo

## Requirements

- Java 21 or higher
- yt-dlp installed and available in PATH
- FFmpeg installed and available in PATH
- FFprobe installed and available in PATH
- Telegram Bot Token

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd youtube-mp3-downloader
```

2. Install required tools:
   - **Windows**: Download yt-dlp and FFmpeg from official sources
   - **Linux**: `sudo apt install yt-dlp ffmpeg`

3. Configure the bot:
   - Create `src/main/resources/config.properties` file
   - Add your bot credentials:
```properties
bot.token=your_bot_token_here
bot.username=your_bot_username
yt-dlp.path.win=C:/path/to/yt-dlp.exe
ffmpeg.path.win=C:/path/to/ffmpeg.exe
ffprobe.path.win=C:/path/to/ffprobe.exe
yt-dlp.path.unix=yt-dlp
ffmpeg.path.unix=ffmpeg
ffprobe.path.unix=ffprobe
max.filesize=52428800
max.duration=10.0
max.parallel.downloads=3
```

4. Build the project:
```bash
mvn clean package
```

5. Run the bot:
```bash
java -jar target/youtube-mp3-downloader-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `bot.token` | Telegram Bot Token | Required |
| `bot.username` | Bot username | Required |
| `yt-dlp.path.win` | yt-dlp path (Windows) | `yt-dlp` |
| `ffmpeg.path.win` | FFmpeg path (Windows) | `ffmpeg` |
| `ffprobe.path.win` | FFprobe path (Windows) | `ffprobe` |
| `yt-dlp.path.unix` | yt-dlp path (Unix/Linux) | `yt-dlp` |
| `ffmpeg.path.unix` | FFmpeg path (Unix/Linux) | `ffmpeg` |
| `ffprobe.path.unix` | FFprobe path (Unix/Linux) | `ffprobe` |
| `max.filesize` | Maximum file size in bytes | `52428800` (50MB) |
| `max.duration` | Maximum video duration in minutes | `10.0` |
| `max.parallel.downloads` | Maximum parallel downloads | `3` |

## Usage

1. Start a chat with your bot
2. Send a YouTube URL
3. Bot will download and convert it to MP3
4. Receive the MP3 file

### Supported Commands

- `/start` - Show welcome message
- `/help` - Show help information
- `/status` - Show current download status
- Send any YouTube URL to download

## Development

### Running Tests

#### All Tests
```bash
mvn test
```

#### Core Tests Only (without Mockito issues)
```bash
mvn test -Pcore-tests
```

#### Integration Tests
```bash
mvn test -Pintegration-tests
```

### Building with Tests
```bash
mvn clean verify
```

### Code Coverage
```bash
mvn clean test jacoco:report
```

Coverage report will be generated in `target/site/jacoco/index.html`

### Test Profiles

- `core-tests`: Basic functionality tests
- `integration-tests`: Full integration tests with Mockito
- `all-tests`: All tests combined

## Project Structure

```
src/
├── main/
│   ├── java/dev/telegrambots/youtubemp3downloader/
│   │   ├── Main.java                    # Application entry point
│   │   ├── Bot.java                     # Main bot logic
│   │   ├── BotConfig.java               # Configuration management
│   │   ├── CommandHandler.java          # Command processing
│   │   ├── YtDlpService.java            # YouTube downloading service
│   │   ├── TelegramService.java         # Telegram API integration
│   │   ├── FileNameSanitizer.java       # Filename sanitization
│   │   ├── ProcessRunner.java           # Process execution interface
│   │   ├── SystemProcessRunner.java     # System process implementation
│   │   ├── Utils.java                   # Utility functions
│   │   └── FileNameSanitizerCli.java    # CLI tool for testing
│   └── resources/
│       └── config.properties            # Bot configuration
└── test/
    └── java/dev/telegrambots/youtubemp3downloader/
        ├── BotConfigTest.java
        ├── BotTest.java
        ├── CommandHandlerTest.java
        ├── DebugSanitizerTest.java
        ├── FileNameSanitizerAdvancedTest.java
        ├── FileNameSanitizerTest.java
        ├── MainTest.java
        ├── SystemProcessRunnerTest.java
        ├── UtilsExtensiveTest.java
        ├── UtilsTest.java
        └── UtilityAndEdgeCaseTest.java
```

## Dependencies

- Telegram Bots API (6.8.0)
- Commons IO (2.15.1)
- SLF4J Simple (2.0.13)
- JSON Library (20240303)
- JUnit Jupiter (5.10.2) - for testing
- Mockito Inline (4.11.0) - for testing
- Byte Buddy (1.14.18) - for testing
- JaCoCo (0.8.11) - code coverage

## Architecture

### Key Components

1. **Bot.java**: Main Telegram bot implementation
2. **YtDlpService.java**: Handles YouTube video downloading and conversion
3. **CommandHandler.java**: Processes user commands and messages
4. **BotConfig.java**: Cross-platform configuration management
5. **FileNameSanitizer.java**: Advanced filename sanitization for different filesystems

### Design Patterns

- **Strategy Pattern**: ProcessRunner interface for different execution strategies
- **Factory Pattern**: Configuration-based object creation
- **Observer Pattern**: Progress tracking and status updates
- **Command Pattern**: Command processing and handling

## Performance

- Parallel download support (configurable)
- Efficient memory usage with streaming
- Automatic cleanup of temporary files
- Connection pooling for Telegram API

## Error Handling

- Comprehensive error messages
- Graceful degradation on tool failures
- Automatic retry mechanisms
- Detailed logging with SLF4J

## License

This project is licensed under the MIT License.
