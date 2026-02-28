# Speech Features Setup Guide

This guide explains how to set up Text-to-Speech (TTS) and Speech-to-Text (STT) capabilities for the Model Downloader.

## System Dependencies

### For Text-to-Speech (TTS)

**Linux/Ubuntu:**
```bash
sudo apt-get update
sudo apt-get install espeak espeak-data libespeak1 libespeak-dev
```

**macOS:**
```bash
brew install espeak
```

**Windows:**
- TTS is built into Windows, no additional setup required

### For Speech-to-Text (STT)

**Linux/Ubuntu:**
```bash
sudo apt-get install portaudio19-dev python3-pyaudio
pip install pyaudio
```

**macOS:**
```bash
brew install portaudio
pip install pyaudio
```

**Windows:**
```bash
pip install pyaudio
```

## Python Dependencies

Install the required Python packages:

```bash
pip install speechrecognition pyttsx3
```

## Testing Speech Features

After installation, test the speech capabilities:

```bash
model-downloader speech-test
```

This will show:
- ✅ TTS Available: Whether text-to-speech is working
- ✅ STT Available: Whether speech-to-text is working  
- ✅ Voices Available: Whether voice options are available

## Usage Examples

### Text-to-Speech
```bash
# Speak text immediately
model-downloader speak -t "Hello, this is a test"

# Save speech to audio file
model-downloader speak -t "Hello world" -s output.wav
```

### Speech-to-Text
```bash
# Listen for 5 seconds and convert to text
model-downloader listen

# Listen for 10 seconds
model-downloader listen --timeout 10

# Process audio file
model-downloader listen --file audio.wav
```

### Voice Chat
```bash
# Start voice chat with a model
model-downloader chat --mode voice

# Start text chat (default)
model-downloader chat --mode text
```

### Web Interface
```bash
# Start web UI with speech support
model-downloader web-ui

# Then open http://127.0.0.1:5000 in your browser
```

## Troubleshooting

### TTS Issues
- **"eSpeak not found"**: Install eSpeak as shown above
- **"No voices available"**: Check eSpeak installation
- **"TTS engine failed"**: Try reinstalling pyttsx3

### STT Issues  
- **"PyAudio not found"**: Install PyAudio and PortAudio
- **"No microphone detected"**: Check microphone permissions
- **"Recognition failed"**: Ensure internet connection (uses Google Speech API)

### Permissions
- **Microphone access**: Grant microphone permissions to your terminal/Python
- **Audio output**: Ensure speakers/headphones are working

## Alternative Configurations

### Offline STT
For offline speech recognition, you can use:
```bash
pip install SpeechRecognition[whisper-api]
```

### Different TTS Voices
The system will use available system voices. On Linux with eSpeak:
```bash
espeak --voices  # List available voices
```

## Integration with Models

Once speech is working, you can:

1. **Download models**: Use voice commands in the web UI
2. **Chat with models**: Use voice input/output in chat mode
3. **Automated interaction**: Set up voice-controlled model management

## Performance Notes

- **TTS**: Usually fast, works offline
- **STT**: Requires internet for Google Speech API (default)
- **Real-time**: Both TTS and STT work in real-time for interactive chat
- **File processing**: Both can work with audio files for batch processing

## Security Considerations

- **STT Privacy**: By default uses Google's cloud API
- **Microphone**: Only active when explicitly requested
- **Audio files**: Processed locally, not uploaded anywhere
- **Voice data**: No voice data is stored permanently