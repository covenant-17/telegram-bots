# Converter Bot

Telegram bot for converting WebM and GIF files to MP4 format.

## Features

- Convert WebM files to MP4
- Convert GIF files to MP4
- File size validation (max 50MB)
- Duration validation (max 10 minutes)
- Automatic file cleanup
- Cross-platform support

## Requirements

- Java 21 or higher
- FFmpeg installed and available in PATH
- Telegram Bot Token

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd converter-bot
```

2. Configure the bot:
   - Create `src/main/resources/config.properties` file
   - Add your bot credentials:
```properties
bot.token=your_bot_token_here
bot.username=your_bot_username
ffmpeg.path=ffmpeg
max.filesize=52428800
max.duration=10.0
```

3. Build the project:
```bash
mvn clean package
```

4. Run the bot:
```bash
java -jar target/converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `bot.token` | Telegram Bot Token | Required |
| `bot.username` | Bot username | Required |
| `ffmpeg.path` | Path to FFmpeg executable | `ffmpeg` |
| `max.filesize` | Maximum file size in bytes | `52428800` (50MB) |
| `max.duration` | Maximum video duration in minutes | `10.0` |

## Usage

1. Start a chat with your bot
2. Send a WebM or GIF file
3. Bot will convert it to MP4 and send back

## Development

### Running Tests
```bash
mvn test
```

### Building with Tests
```bash
mvn clean verify
```

### Code Coverage
```bash
mvn clean test jacoco:report
```

## Project Structure

```
src/
├── main/
│   ├── java/dev/telegrambots/converterbot/
│   │   ├── Main.java              # Application entry point
│   │   ├── ConverterBot.java      # Main bot logic
│   │   └── BotConfig.java         # Configuration management
│   └── resources/
│       └── config.properties      # Bot configuration
└── test/
    └── java/dev/telegrambots/converterbot/
        └── ConverterBotTest.java  # Unit tests
```

## Dependencies

- Telegram Bots API (6.8.0)
- SLF4J Simple (2.0.13)
- JSON Library (20240303)
- JUnit Jupiter (5.10.2) - for testing

## License

This project is licensed under the MIT License.
