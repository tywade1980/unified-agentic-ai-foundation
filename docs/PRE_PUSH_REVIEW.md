# Pre-Push Review & Integration Plan
### Project: `unified-agentic-ai-foundation`
**Date:** March 7, 2026

---

## 1. Overall Status

Excellent call on pausing to review. After a full audit of all skills, the local repository state, and the assets you provided, I have a complete picture. 

We have successfully **cataloged and staged** a massive amount of intellectual property into a single, coherent repository structure. This includes multiple Android application scaffolds, numerous AI engine concepts, detailed technical specifications, and the core logic for the Wade Custom Carpentry (WCC) business.

However, the current state is a **well-organized collection of parts, not a running application.** Significant integration work is required to connect these components, resolve duplicate functionalities, and build out the user interfaces. The `wade-custom-carpentry` skill provides the exact blueprint for how to wire many of these parts together.

This document outlines the full inventory of what we have and the action plan to unify it.

---

## 2. Feature & Function Inventory

Here is a breakdown of all major features currently prepared in the repository.

| Feature Area | Component(s) | Status | Description |
| :--- | :--- | :--- | :--- |
| **Core AI Orchestration** | `multi_agent_orchestrator.py` | **Implemented (Python)** | A functional, multi-agent orchestrator using the Grok API. Supports dynamic agent/tool registration and parallel tasks. This is the backend brain. |
| **Android "Super App"** | `caroline_superapp/` | **Scaffolded (Kotlin)** | A complete directory structure for the main Android app, containing 11 distinct AI engine modules, UI components, and configuration files. The code is present but not yet compiled or wired together. |
| **On-Device AI** | `LocalAIEngine.kt`, `Phi3ModelDownloader_Version2.java` | **Implemented (Concept)** | Code exists to download and run a Phi-3 ONNX model on-device. Needs to be integrated into the main app's lifecycle. |
| **AI Self-Evolution** | `AutoToolingEngine.kt`, `AutonomousEvolutionEngine.kt` | **Implemented (Concept)** | Core logic for the AI to generate its own tools and evolve its codebase is present. This is a powerful, high-level feature that needs to be wired into the Python orchestrator. |
| **Telephony / AI Receptionist** | `telephony_agent/`, `smart-incallservice/`, `CSR.AI` & `CarolineReceptionist` specs | **Partially Implemented** | Multiple implementations exist. We have a full Android `CallScreener` agent, a separate `AIInCallService`, and detailed specs. These need to be merged into a single, robust telephony service. |
| **Construction Management** | `wcc_pro/`, `Constructpro` spec, `mindmap.png` | **Partially Implemented** | We have the core JS logic for WCC, the Firebase schema, the command map, the pricebook template, and the master assembly mind map. This is a strong foundation. |
| **Voice Interface (TTS/STT)** | `deepgramvopiceagentandsdk.txt`, `generate_voice_update.py` | **Implemented (Integrations)** | We have clear instructions for integrating Deepgram (for real-time conversation) and a Python script for generating voice updates (using OpenAI TTS). These need to be connected to the Android UI and backend agents. |

---

## 3. UI/UX Audit

You asked if we have a UI built out. The answer is **yes, we have several UI components, but they are not yet integrated into a single, navigable application.**

- **`GeminiStyleChatInterface.kt`**: This is a Jetpack Compose UI component that provides a modern, message-based chat interface, similar to the Gemini app. This will be the primary conversational view within the Caroline app.

- **`IDEScreen.kt`**: This is a more complex, full-screen Jetpack Compose UI that provides a functional Integrated Development Environment (IDE) inside the app. It includes a file explorer, a text editor, and hooks for AI code assistance and voice commands. This is a powerful, specialized view.

- **`nextjs-ai-chatbot/`**: This is a separate, web-based chatbot interface built with Next.js. It is currently a scaffold and not fully functional. It could serve as a web-based portal for interacting with Caroline from a desktop.

**Conclusion:** We have the core building blocks for a rich user experience on Android. The immediate task is to create the navigation logic within the `caroline_superapp` to move between the `GeminiStyleChatInterface`, the `IDEScreen`, and other new screens we will build for construction project management.

---

## 4. Integration Plan: `wade-custom-carpentry` Skill

The `wade-custom-carpentry` skill is the missing link that ties the business logic to the AI engines. Here is the plan to integrate it:

1.  **Make the `wcc-pro-engine` an Agent:** The JavaScript-based `wcc-pro-engine` will be wrapped as a specialized agent that the main Python orchestrator can call. This agent will be responsible for all construction-specific tasks.

2.  **Connect the Command Map:** The `commands.map.json` will be used by the central orchestrator to route natural language commands to the new WCC Agent. For example, when the user says, "Create an estimate for a bathroom remodel," the orchestrator will know to call the `estimates.create` function on the WCC Agent.

3.  **Implement the Assembly Mind Map:** The `home_assembly_mindmap.png` provides the taxonomy for all construction tasks. The WCC Agent will use this as its internal knowledge graph for generating take-offs and building project plans. When a user says "bathroom remodel," the agent will traverse the mind map to identify all child assemblies (demo, plumbing, tile, etc.).

4.  **Integrate Material Sourcing:** The `material_sourcing.md` guide will be converted into a series of automated browser tasks using the Playwright MCP. The WCC Agent will be able to search Home Depot, Lowe's, etc., for materials, get pricing, and add them to a project estimate in the Firebase database.

5.  **Activate the Proposal Template:** The `proposal_template.md` will be used by the WCC Agent to generate professional PDF proposals. The agent will populate the template with data from the project estimate (tasks, materials, costs) and present it to the user for approval.

6.  **Enable Voice Briefings:** The `generate_voice_update.py` script will be integrated as a core tool. After any significant project update (e.g., a material cost change, a schedule adjustment), the WCC Agent will automatically generate an audio summary for hands-free listening.

---

## 5. Merge & Refactor Candidates

To create a clean, unified codebase, we must address several areas of overlapping functionality.

- **Single Telephony Service:** Merge the `telephony_agent/`, `smart-incallservice/`, and the concepts from the `CSR.AI` spec into a single, authoritative `telephony_voice_ai/caroline_receptionist/` module. This module will handle all call screening, IVR, and in-call assistance.

- **Unified Android App:** The `caroline_superapp/` is the designated primary application. Other Android projects like `ai-agent-android/`, `ngbp-v2-0/`, and `Signal-Android/` should be treated as sources of features and ideas to be merged into `caroline_superapp`, not as separate, parallel apps.

- **Centralized AI Core:** The `agentic_core/` directory contains multiple high-level concepts (`action_engineering_team`, `kilocode_src`, etc.). These should be refactored and merged into the primary Python orchestrator and the Kotlin AI engines. The goal is one backend brain and one mobile brain.

---

## 6. Action Plan Before Push

Based on this review, here is the recommended plan before we attempt another push:

1.  **Refactor Repository:** Create a new `PRE_PUSH_REVIEW.md` (this document) and a `ROADMAP.md` in the root of the repository. Move the `PROJECT_SYNTHESIS.md` into a `/docs` directory.

2.  **Integrate WCC Skill:** Begin the integration described in Section 4. The first step is to wrap the `wcc-pro-engine` as a callable agent.

3.  **Build Basic UI Navigation:** In the `caroline_superapp`, build a simple bottom navigation bar that allows switching between the `GeminiStyleChatInterface` and a new, placeholder screen for `WCC Projects`.

4.  **Resolve a Key Merge:** Perform the first major refactor by merging the multiple telephony modules into the single `caroline_receptionist` service.

**This is a significant undertaking.** My recommendation is to tackle **Action Plan item #1 immediately**. Then, we can decide which integration point to focus on first. This ensures the repository is clean and well-documented before we add more complex code.

**Do you approve of this plan?** Once you confirm, I will begin by restructuring the repository and creating the roadmap document.
