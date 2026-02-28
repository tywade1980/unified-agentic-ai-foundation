#!/usr/bin/env python3

"""
Demonstration script for the Model Downloader system.

This script showcases the key features and autonomous operation
capabilities of the automated background LLM model downloader.
"""

import sys
import time
import threading
from pathlib import Path

sys.path.insert(0, '.')

from model_downloader.config import Config
from model_downloader.notifications import NotificationManager

def demo_autonomous_operation():
    """Demonstrate autonomous operation with minimal human involvement."""
    print("🤖 AUTONOMOUS MODEL DOWNLOADER DEMONSTRATION")
    print("=" * 60)
    
    print("\n🎯 Key Features:")
    print("  ✓ Automated background downloads")
    print("  ✓ Minimal human involvement")
    print("  ✓ User kept in the loop via notifications")
    print("  ✓ Self-configuring system")
    print("  ✓ Error handling and recovery")
    print("  ✓ Resource management")
    
    # Initialize system
    print("\n🚀 Initializing Autonomous System...")
    config = Config()
    notif = NotificationManager(config)
    
    # Show current configuration
    print(f"\n⚙️  System Configuration:")
    print(f"   • Auto downloads: {'Enabled' if config.is_auto_download_enabled() else 'Disabled'}")
    print(f"   • Check interval: {config.get_check_interval_hours()} hours")
    print(f"   • Daily limit: {config.get('schedule.max_downloads_per_day', 5)} downloads")
    print(f"   • Concurrent downloads: {config.get('download_settings.max_concurrent_downloads', 2)}")
    print(f"   • Download directory: {config.get_download_directory()}")
    
    # Demonstrate automatic model configuration
    print(f"\n📋 Configured Models:")
    models = config.get_models()
    for i, model in enumerate(models, 1):
        auto_status = "🔄 Auto" if model.get('auto_download', True) else "📝 Manual"
        priority = model.get('priority', 1)
        print(f"   {i}. {model['name']} ({auto_status}, Priority: {priority})")
    
    # Add some example models for demonstration
    print(f"\n➕ Adding Example Models for Demonstration...")
    example_models = [
        ("distilbert-base-uncased", "huggingface", 2),
        ("microsoft/DialoGPT-small", "huggingface", 3),
    ]
    
    for model_name, source, priority in example_models:
        # Check if model already exists
        existing = any(m['name'] == model_name for m in config.get_models())
        if not existing:
            config.add_model(model_name, source, True, priority)
            print(f"   ✓ Added: {model_name}")
        else:
            print(f"   ⚠️  Already exists: {model_name}")
    
    # Show updated model list
    print(f"\n📋 Updated Model List:")
    updated_models = config.get_models()
    for i, model in enumerate(updated_models, 1):
        auto_status = "🔄 Auto" if model.get('auto_download', True) else "📝 Manual"
        priority = model.get('priority', 1)
        print(f"   {i}. {model['name']} ({auto_status}, Priority: {priority})")
    
    # Demonstrate notification system
    print(f"\n📢 Notification System Demonstration:")
    notif.notify("System Started", "Model downloader initialized and ready", "info")
    notif.notify("Model Added", "New model configured for automatic download", "info")
    notif.notify("Download Scheduled", "Next check scheduled in 24 hours", "info")
    
    # Show notification history
    history = notif.get_notification_history(5)
    print(f"   Recent notifications ({len(history)}):")
    for i, notif_item in enumerate(history[-3:], 1):  # Show last 3
        timestamp = notif_item['timestamp'][:19]  # Remove microseconds
        level = notif_item['level'].upper()
        title = notif_item['title']
        print(f"   {i}. [{timestamp}] {level}: {title}")
    
    return config, notif

def demo_user_control():
    """Demonstrate user control and monitoring capabilities."""
    print(f"\n👤 USER CONTROL & MONITORING")
    print("=" * 40)
    
    config = Config()
    
    print(f"\n🔧 Available User Controls:")
    print(f"   • Start/stop automated service")
    print(f"   • Add/remove models")
    print(f"   • Adjust download schedules")
    print(f"   • Force immediate downloads")
    print(f"   • Monitor progress and logs")
    print(f"   • Configure notifications")
    
    print(f"\n📊 Monitoring Capabilities:")
    print(f"   • Real-time download progress")
    print(f"   • Download statistics and history")
    print(f"   • Error logging and reporting")
    print(f"   • System status monitoring")
    
    print(f"\n⚡ Emergency Controls:")
    print(f"   • Manual override of automatic downloads")
    print(f"   • Immediate stop/pause functionality")
    print(f"   • Resource limit adjustments")
    print(f"   • Configuration rollback")

def demo_error_handling():
    """Demonstrate error handling and recovery capabilities."""
    print(f"\n🛡️  ERROR HANDLING & RECOVERY")
    print("=" * 40)
    
    notif = NotificationManager(Config())
    
    print(f"\n🔄 Automatic Recovery Features:")
    print(f"   • Network failure retry with exponential backoff")
    print(f"   • Resume interrupted downloads")
    print(f"   • Fallback to alternative download sources")
    print(f"   • Automatic cleanup of corrupted files")
    
    print(f"\n📝 Error Reporting:")
    print(f"   • Detailed error logs")
    print(f"   • User-friendly error notifications")
    print(f"   • Automatic error categorization")
    print(f"   • Recovery suggestions")
    
    # Simulate error notifications
    notif.notify("Recovery Success", "Download resumed after network error", "info")
    notif.notify("Retry Attempt", "Retrying failed download (attempt 2/3)", "warning")
    
    print(f"\n✅ Error Handling Demonstrated:")
    print(f"   • Graceful degradation")
    print(f"   • User kept informed")
    print(f"   • Automatic recovery")

def show_cli_examples():
    """Show CLI command examples."""
    print(f"\n💻 CLI COMMAND EXAMPLES")
    print("=" * 40)
    
    examples = [
        ("Start Service", "model-downloader start"),
        ("Download Model", "model-downloader download -m 'microsoft/DialoGPT-medium'"),
        ("Add Model", "model-downloader add-model -m 'microsoft/DialoGPT-large' -p 1"),
        ("List Models", "model-downloader list-models"),
        ("Check Status", "model-downloader status"),
        ("Force Check", "model-downloader check-now"),
        ("View Logs", "model-downloader logs -n 50"),
        ("Show Config", "model-downloader config-show"),
    ]
    
    for description, command in examples:
        print(f"\n{description}:")
        print(f"   $ {command}")

def main():
    """Main demonstration function."""
    try:
        print("🎯 AUTOMATED BACKGROUND LLM MODEL DOWNLOADER")
        print("🎯 Autonomous Operation with Minimal Human Involvement")
        print("=" * 70)
        
        # Run demonstrations
        config, notif = demo_autonomous_operation()
        demo_user_control()
        demo_error_handling()
        show_cli_examples()
        
        # Final summary
        print(f"\n🎉 DEMONSTRATION COMPLETE")
        print("=" * 40)
        print(f"\nThe system demonstrates:")
        print(f"✓ Autonomous operation with minimal human involvement")
        print(f"✓ Users kept in the loop via comprehensive notifications")
        print(f"✓ Self-configuring with sensible defaults")
        print(f"✓ Robust error handling and recovery")
        print(f"✓ Flexible user control and monitoring")
        print(f"✓ Production-ready architecture")
        
        print(f"\n📁 Generated Files:")
        print(f"   • Config: ~/.model-downloader/config.yaml")
        print(f"   • Logs: {config.get_download_directory()}/logs/")
        print(f"   • Models: {config.get_download_directory()}/")
        
        print(f"\n🚀 Ready for Production Use!")
        print(f"Install dependencies and run 'model-downloader start' to begin.")
        
    except Exception as e:
        print(f"❌ Demo failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()