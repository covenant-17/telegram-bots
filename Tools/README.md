# Tools Directory

This directory contains external tools required for the Telegram bots to function properly.

## Required Tools

### FFmpeg
- **Purpose**: Audio/video processing and conversion
- **Download**: https://ffmpeg.org/download.html
- **Installation**:
  1. Download the Windows build (ffmpeg-*-win64-*.zip)
  2. Extract to `Tools/ffmpeg/`
  3. Ensure `bin/ffmpeg.exe` and `bin/ffprobe.exe` are present

### yt-dlp
- **Purpose**: YouTube video/audio downloader
- **Download**: https://github.com/yt-dlp/yt-dlp/releases
- **Installation**:
  1. Download `yt-dlp.exe` from releases
  2. Place in `Tools/yt-dlp/yt-dlp.exe`

## Configuration Options

### Option 1: System PATH (Recommended)
Add the tool directories to your system PATH:
- `C:\!Dev\Tools\ffmpeg\bin`
- `C:\!Dev\Tools\yt-dlp`

Then use default commands in `config.properties`:
```
yt-dlp.path=yt-dlp
ffmpeg.path=ffmpeg
ffprobe.path=ffprobe
```

### Option 2: Absolute Paths
Specify full paths in `config.properties`:
```
yt-dlp.path.win=C:/!Dev/Tools/yt-dlp/yt-dlp.exe
ffmpeg.path.win=C:/!Dev/Tools/ffmpeg/bin/ffmpeg.exe
ffprobe.path.win=C:/!Dev/Tools/ffmpeg/bin/ffprobe.exe
```

### Option 3: Environment Variables
Set environment variables:
- `YT_DLP_PATH=C:/!Dev/Tools/yt-dlp/yt-dlp.exe`
- `FFMPEG_PATH=C:/!Dev/Tools/ffmpeg/bin/ffmpeg.exe`
- `FFPROBE_PATH=C:/!Dev/Tools/ffmpeg/bin/ffprobe.exe`

And keep default values in `config.properties`.

## Notes
- The bot will automatically try multiple path resolution strategies
- Priority: config property → environment variable → default command
- This ensures maximum compatibility across different environments
