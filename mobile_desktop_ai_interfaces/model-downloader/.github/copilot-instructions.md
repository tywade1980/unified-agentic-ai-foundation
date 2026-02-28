# Copilot Instructions - Model Downloader

## Repository Overview

**Model Downloader** is an automated background LLM model downloader that operates autonomously with minimal human involvement. Python 3.8+ project with Click CLI, APScheduler background tasks, and HuggingFace Hub integration.

**Architecture**: 6 core Python modules (~3,000 lines) in modular design: config management, download engine, scheduler, notifications, and CLI interface.

## Build Process (CRITICAL - Always follow this order)

1. **Install dependencies first** (ALWAYS required):
   ```bash
   pip install -r requirements.txt
   ```

2. **Install package** (ALWAYS required):
   ```bash
   pip install -e .
   ```

3. **Verify installation**:
   ```bash
   model-downloader --help
   ```

**Alternative**: Use `bash install.sh` (handles all steps with verification)

**Build Times**: Dependencies ~30-60s, Package ~10-15s, Tests ~5s

## Testing & Validation

**Quick validation sequence after changes**:
```bash
python3 -c "import model_downloader; print('OK')"  # Import test
model-downloader --help                            # CLI test
model-downloader config-show                       # Config test
python3 test_basic.py                             # Functionality test
model-downloader status                           # Status test
```

**Comprehensive tests**:
- `python3 test_basic.py` - Basic functionality without network
- `python3 demo.py` - Full feature demonstration
- `model-downloader` commands - Live functionality testing

## Project Layout

```
model_downloader/           # Core package
├── cli.py                 # Click-based CLI with all commands
├── config.py              # YAML config (~/.model-downloader/config.yaml)
├── downloader.py          # HuggingFace Hub & direct URL downloads
├── scheduler.py           # APScheduler background service
├── notifications.py       # Logging system
└── __init__.py

Key Files:
- requirements.txt         # 7 Python dependencies
- setup.py                # Package config, console_scripts entry
- install.sh              # Automated setup script
- test_basic.py           # Network-free tests
- demo.py                 # Feature demonstration
- example_config.yaml     # Dev configuration
```

**Generated on first run**:
- `~/.model-downloader/config.yaml` (user config)
- `./models/logs/model_downloader.log` (app logs)

## Essential CLI Commands

**Service**: `model-downloader start|status|check-now`
**Models**: `model-downloader add-model -m "model-name" -p 1|list-models|download -m "model"`
**Config**: `model-downloader config-show|config-set -k KEY -v VALUE|logs -n 100`
**Options**: `--config PATH` (custom config), `--verbose` (debug logging)

## Development Workflow

**Making Changes**:
1. Edit source in `model_downloader/` directory
2. Test immediately (no reinstall needed - uses `-e` flag)
3. Validate with quick sequence above

**Key Behaviors**:
- Configuration auto-created with sensible defaults
- Download directory: `./models/` (relative to current working directory)
- Logs: `./models/logs/model_downloader.log`
- No restart needed for config changes

## Dependencies & Error Patterns

**Required packages**: requests, pyyaml, tqdm, huggingface-hub, click, apscheduler, python-dotenv

**Common errors**:
- **Import errors**: Missing dependencies (run pip install -r requirements.txt)
- **Config errors**: YAML syntax issues
- **Permission errors**: Need write access to `~/.model-downloader/` and `./models/`
- **Network errors**: HuggingFace Hub requires internet
- **Model not found**: System provides detailed error messages in logs

## Critical Notes for Agents

**TRUST THESE INSTRUCTIONS**: Comprehensive and tested with multiple scenarios. Only search if information is incomplete/incorrect.

**NO CI/CD**: No GitHub Actions, linting configs, or automated checks. Changes can be tested immediately.

**SIMPLE TESTING**: Use documented test commands. No pytest/unittest frameworks.

**MODULAR ARCHITECTURE**: Each module has single responsibility. CLI orchestrates other components. Changes to one module rarely affect others.

**ERROR HANDLING**: System gracefully handles failures with comprehensive logging and retry mechanisms. All errors logged to `./models/logs/model_downloader.log`.

**CONFIGURATION-DRIVEN**: Most behavior controlled via YAML. Default config is production-ready. Changes take effect immediately.

Package uses editable installation so code changes are immediately available without reinstallation.
