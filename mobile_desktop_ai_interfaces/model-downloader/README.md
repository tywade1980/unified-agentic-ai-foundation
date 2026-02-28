# Model Downloader

An automated background LLM model downloader that operates autonomously with minimal human involvement while keeping users in the loop.

## Features

- **Automated Downloads**: Schedule automatic model downloads with configurable intervals
- **Multiple Sources**: Support for Hugging Face Hub and direct URL downloads
- **Background Operation**: Runs as a service with minimal system impact
- **User Notifications**: Keep users informed via logs and notifications
- **Flexible Configuration**: YAML-based configuration with sensible defaults
- **Download Management**: Resume interrupted downloads, retry on failures
- **CLI Interface**: Complete command-line interface for management and monitoring
- **🎤 Speech Support**: Text-to-Speech and Speech-to-Text capabilities
- **💬 Interactive Chat**: Chat with downloaded models using text or voice
- **🌐 Web Interface**: User-friendly web UI for model management and interaction
- **📱 Mobile App**: Android app for easy model downloads (no authentication required for public models)

## Quick Start

### Installation

1. Clone the repository:
```bash
git clone https://github.com/tywade1980/model-downloader.git
cd model-downloader
```

2. Install dependencies:
```bash
pip install -r requirements.txt
# or
pip install -e .
```

### Basic Usage

1. **Start the automated service**:
```bash
model-downloader start
```

2. **Download a model immediately**:
```bash
model-downloader download -m "microsoft/DialoGPT-medium"
```

3. **Add a model for automatic downloads**:
```bash
model-downloader add-model -m "microsoft/DialoGPT-large" -p 1
```

4. **Chat with a downloaded model**:
```bash
model-downloader chat -m "microsoft/DialoGPT-medium"
```

5. **Use voice chat**:
```bash
model-downloader chat -m "microsoft/DialoGPT-medium" --mode voice
```

6. **Start web interface**:
```bash
model-downloader web-ui
```

7. **Test speech capabilities**:
```bash
model-downloader speech-test
```

8. **Check status**:
```bash
model-downloader status
```

9. **List all models**:
```bash
model-downloader list-models
```

## Configuration

The system uses a YAML configuration file located at `~/.model-downloader/config.yaml`. If the file doesn't exist, it will be created with default settings on first run.

### Example Configuration

```yaml
download_directory: './models'

schedule:
  enabled: true
  check_interval_hours: 24
  max_downloads_per_day: 5

models:
  - name: 'microsoft/DialoGPT-medium'
    source: 'huggingface'
    auto_download: true
    priority: 1

notifications:
  enabled: true
  log_level: 'INFO'
  notify_on_start: true
  notify_on_completion: true
  notify_on_error: true

download_settings:
  max_concurrent_downloads: 2
  retry_attempts: 3
  timeout_seconds: 3600
  resume_downloads: true
```

### Configuration Options

- **download_directory**: Where to store downloaded models
- **schedule.enabled**: Enable/disable automated downloads
- **schedule.check_interval_hours**: How often to check for new models
- **schedule.max_downloads_per_day**: Daily download limit
- **models**: List of models to manage automatically
- **notifications**: Notification and logging settings
- **download_settings**: Download behavior configuration

## CLI Commands

### Service Management
```bash
model-downloader start              # Start the automated service
model-downloader check-now          # Force immediate check
model-downloader status             # Show service status
```

### Model Management
```bash
model-downloader download -m MODEL  # Download specific model
model-downloader add-model -m MODEL # Add model to config
model-downloader remove-model -m MODEL # Remove from config
model-downloader list-models        # List all models
```

### Interactive Features
```bash
model-downloader chat               # Start interactive chat
model-downloader chat --mode voice  # Voice-based chat
model-downloader web-ui             # Start web interface
```

### Speech Features
```bash
model-downloader speak -t "text"    # Convert text to speech
model-downloader listen             # Convert speech to text
model-downloader speech-test        # Test TTS/STT capabilities
```

### Configuration
```bash
model-downloader config-show        # Show current config
model-downloader config-set -k KEY -v VALUE # Set config value
model-downloader logs -n 100        # Show recent log entries
```

## Advanced Usage

### Custom Configuration File
```bash
model-downloader --config /path/to/config.yaml start
```

### Verbose Logging
```bash
model-downloader --verbose start
```

### Adding Models with Custom Settings
```bash
model-downloader add-model \
  -m "microsoft/DialoGPT-large" \
  -s "huggingface" \
  -p 1 \
  --auto
```

## Model Interaction

Once models are downloaded, you can interact with them in multiple ways:

### Text Chat
```bash
model-downloader chat
# Select a model and start typing to chat
```

### Voice Chat  
```bash
model-downloader chat --mode voice
# Speak to the model and hear responses
```

### Web Interface
```bash
model-downloader web-ui
# Open http://127.0.0.1:5000 in your browser for a full UI
```

The web interface provides:
- 📥 **Model Management**: Download and organize models
- 💬 **Interactive Chat**: Text and voice-based conversations  
- 🎤 **Speech Testing**: Test TTS/STT capabilities
- 📊 **System Status**: Monitor downloads and system health

## Android App

The included Android app provides a mobile interface for downloading models:

- **No Authentication Required**: Download public models without login
- **Simple Interface**: Choose provider, enter model name, download
- **Offline Storage**: Models stored in app's private directory

Build and install the APK from `model_downloader_app/` directory.

## Architecture

The system consists of several key components:

1. **Config Manager**: Handles configuration loading and management
2. **Model Downloader**: Core download logic with support for multiple sources
3. **Scheduler**: Automated background scheduling using APScheduler
4. **Notification Manager**: Logging and user notifications
5. **CLI Interface**: Command-line interface for user interaction
6. **Speech Manager**: Text-to-Speech and Speech-to-Text processing
7. **Chat Interface**: Interactive model conversation system
8. **Web UI**: Flask-based web interface for comprehensive model management

## Supported Model Sources

- **Hugging Face Hub**: Download models from huggingface.co
- **Direct URLs**: Download from any HTTP/HTTPS URL
- **Extensible**: Easy to add new sources

## Requirements

- Python 3.8+
- Required packages (see requirements.txt):
  - requests
  - pyyaml  
  - tqdm
  - huggingface-hub
  - click
  - apscheduler
  - python-dotenv
  - speechrecognition (for STT)
  - pyttsx3 (for TTS)
  - flask (for web UI)
  - transformers (for model interaction)

### Optional System Dependencies

For full speech functionality, install:

**Linux/Ubuntu:**
```bash
sudo apt-get install espeak espeak-data portaudio19-dev python3-pyaudio
```

**macOS:**
```bash
brew install espeak portaudio
```

**Windows:** No additional system dependencies needed.

See [SPEECH_SETUP.md](SPEECH_SETUP.md) for detailed setup instructions.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

MIT License - see LICENSE file for details

## Support

For issues and questions:
- Check the logs: `model-downloader logs`
- Review configuration: `model-downloader config-show`
- Check status: `model-downloader status`

## Automation Features

This tool is designed for minimal human involvement:

- **Self-configuring**: Creates default configuration automatically
- **Auto-retry**: Retries failed downloads with exponential backoff
- **Resume capability**: Resumes interrupted downloads
- **Health monitoring**: Tracks download success/failure rates
- **Resource management**: Limits concurrent downloads and daily totals
- **Graceful handling**: Handles network issues and temporary failures
- **Progress tracking**: Provides detailed progress and status information

The system keeps users informed without requiring constant supervision, making it ideal for automated ML pipeline workflows.