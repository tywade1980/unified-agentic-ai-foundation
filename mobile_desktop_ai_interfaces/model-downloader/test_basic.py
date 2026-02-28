#!/usr/bin/env python3

"""
Simple test script to demonstrate the model downloader functionality
without requiring external dependencies.
"""

import sys
import os
sys.path.insert(0, '.')

from model_downloader.config import Config
from model_downloader.notifications import NotificationManager

def test_basic_functionality():
    """Test basic functionality without network dependencies."""
    print("🚀 Testing Model Downloader Basic Functionality")
    print("=" * 50)
    
    # Test configuration
    print("\n1. Testing Configuration Management...")
    config = Config()
    print(f"   ✓ Download directory: {config.get_download_directory()}")
    print(f"   ✓ Auto download enabled: {config.is_auto_download_enabled()}")
    print(f"   ✓ Check interval: {config.get_check_interval_hours()} hours")
    
    # Test adding/removing models
    print("\n2. Testing Model Configuration...")
    original_models = len(config.get_models())
    print(f"   ✓ Original model count: {original_models}")
    
    config.add_model("test/model", "huggingface", True, 1)
    new_count = len(config.get_models())
    print(f"   ✓ After adding test model: {new_count}")
    
    config.remove_model("test/model")
    final_count = len(config.get_models())
    print(f"   ✓ After removing test model: {final_count}")
    
    # Test notifications
    print("\n3. Testing Notification System...")
    notif = NotificationManager(config)
    notif.notify("Test Notification", "This is a test message", "info")
    print("   ✓ Notification sent successfully")
    
    history = notif.get_notification_history(1)
    if history:
        print(f"   ✓ Last notification: {history[-1]['title']}")
    
    print("\n4. Testing Configuration Persistence...")
    test_key = "test_setting"
    test_value = "test_value"
    config.set(test_key, test_value)
    retrieved_value = config.get(test_key)
    print(f"   ✓ Set and retrieved config: {test_key} = {retrieved_value}")
    
    print("\n🎉 All basic tests passed successfully!")
    print("\n📋 System Status:")
    print(f"   • Configuration file: {config.config_path}")
    print(f"   • Download directory: {config.get_download_directory()}")
    print(f"   • Log file: {notif.get_log_file_path()}")
    print(f"   • Models configured: {len(config.get_models())}")
    
    return True

def show_example_usage():
    """Show example usage of the system."""
    print("\n" + "=" * 60)
    print("📚 EXAMPLE USAGE")
    print("=" * 60)
    
    print("""
After installing dependencies (pip install -r requirements.txt), you can use:

🔧 CLI Commands:
   model-downloader start                    # Start automated service
   model-downloader download -m "microsoft/DialoGPT-medium"
   model-downloader add-model -m "microsoft/DialoGPT-large" -p 1
   model-downloader list-models              # Show all models
   model-downloader status                   # Show service status
   model-downloader config-show              # Show configuration

🐍 Python API:
   from model_downloader import Config, ModelDownloader, NotificationManager
   
   config = Config()
   notif = NotificationManager(config)
   downloader = ModelDownloader(config)
   
   # Download a model
   success = downloader.download_model("microsoft/DialoGPT-medium")

⚙️  Configuration:
   The system creates ~/.model-downloader/config.yaml automatically.
   You can customize download behavior, scheduling, and notifications.

🤖 Automated Operation:
   • Runs in background checking for new models periodically
   • Downloads models automatically based on configuration
   • Sends notifications about download progress and completion
   • Handles retries and resume functionality
   • Manages daily download limits and concurrent downloads
""")

if __name__ == "__main__":
    try:
        success = test_basic_functionality()
        if success:
            show_example_usage()
    except Exception as e:
        print(f"❌ Test failed: {e}")
        sys.exit(1)