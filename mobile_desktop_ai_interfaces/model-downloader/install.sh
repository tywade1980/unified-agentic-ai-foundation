#!/bin/bash

# Installation script for Model Downloader
# This script sets up the automated background LLM model downloader

set -e

echo "🚀 Model Downloader Installation Script"
echo "========================================"

# Check Python version
echo "📋 Checking Python version..."
python_version=$(python3 --version 2>&1 | awk '{print $2}' | cut -d. -f1,2)
required_version="3.8"

if [ "$(printf '%s\n' "$required_version" "$python_version" | sort -V | head -n1)" = "$required_version" ]; then
    echo "   ✓ Python $python_version detected (>= 3.8 required)"
else
    echo "   ❌ Python $python_version detected, but >= 3.8 is required"
    exit 1
fi

# Check for pip
echo "📦 Checking pip availability..."
if command -v pip3 &> /dev/null; then
    echo "   ✓ pip3 found"
    PIP_CMD="pip3"
elif command -v pip &> /dev/null; then
    echo "   ✓ pip found"
    PIP_CMD="pip"
else
    echo "   ❌ pip not found. Please install pip first."
    exit 1
fi

# Install dependencies
echo "📦 Installing dependencies..."
$PIP_CMD install --user -r requirements.txt

if [ $? -eq 0 ]; then
    echo "   ✓ Dependencies installed successfully"
else
    echo "   ⚠️  Some dependencies may have failed to install."
    echo "   You can install them manually if needed."
fi

# Install the package
echo "📦 Installing model-downloader package..."
$PIP_CMD install --user -e .

if [ $? -eq 0 ]; then
    echo "   ✓ Package installed successfully"
else
    echo "   ❌ Package installation failed"
    exit 1
fi

# Check installation
echo "🔍 Verifying installation..."
if python3 -c "import model_downloader; print('Model Downloader imported successfully')" 2>/dev/null; then
    echo "   ✓ Model Downloader package verified"
else
    echo "   ❌ Model Downloader package verification failed"
    exit 1
fi

# Create initial directories
echo "📁 Setting up directories..."
mkdir -p ~/.model-downloader
mkdir -p ./models/logs
echo "   ✓ Directories created"

# Run basic test
echo "🧪 Running basic functionality test..."
python3 test_basic.py > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "   ✓ Basic functionality test passed"
else
    echo "   ⚠️  Basic functionality test had issues (may be due to missing optional dependencies)"
fi

echo ""
echo "🎉 Installation Complete!"
echo "========================"
echo ""
echo "Next steps:"
echo "1. Start the service:     model-downloader start"
echo "2. Add a model:          model-downloader add-model -m 'microsoft/DialoGPT-medium'"
echo "3. Check status:         model-downloader status"
echo "4. View help:            model-downloader --help"
echo ""
echo "Configuration file:      ~/.model-downloader/config.yaml"
echo "Download directory:      ./models/"
echo "Log files:              ./models/logs/"
echo ""
echo "For more information, see README.md"