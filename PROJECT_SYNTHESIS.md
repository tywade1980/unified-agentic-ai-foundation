# Project Synthesis: Unified Agentic AI Foundation
### Wade Custom Carpentry & Centauri OS — Master Integration Document
**Last Updated:** March 7, 2026
**Repository:** `tywade1980/unified-agentic-ai-foundation`
**Status:** Active Development — Raw Data Compilation Phase

---

## 1. The Vision

The **Unified Agentic AI Foundation** is not a single app. It is a **multi-agent operating system** — a platform that thinks, talks, builds tools, manages a business, handles phone calls, codes, learns, and evolves. It is the digital backbone of Wade Custom Carpentry and the technological expression of the Centauri OS concept.

The system has one primary persona: **Caroline** — a voice-first AI companion and business operating system that replaces the traditional Android UI, handles inbound calls as a receptionist, manages projects, generates estimates, and orchestrates a fleet of specialized sub-agents to handle every domain of the business.

**Core Identity:**
- Conversational and voice-first (hands-free is mandatory — tools in hand on the job site)
- Multi-agent and orchestrated (not a single chatbot, but a coordinated team of AI agents)
- Self-evolving (can identify gaps in its own tooling and generate new tools to fill them)
- Business-integrated (construction project management, CRM, estimating, invoicing)
- Telephony-capable (AI receptionist, call screening, IVR, voicemail, lead qualification)
- Mobile-native (Android, Samsung S22, on-device inference where possible)
- Open and extensible (MCP servers, APIs, Web3, local LLMs)

---

## 2. Architecture: Multi-Pattern Hybrid

The system uses a **combined hub-and-spoke + hierarchical architecture**, which provides both centralized coordination and deep specialization.

```
┌─────────────────────────────────────────────────────────────────┐
│                    CENTAURI OS LAYER                            │
│              (Device-Level OS Replacement)                      │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                 CAROLINE — MASTER ORCHESTRATOR                  │
│         (Hub: Routes all intent to specialized agents)          │
│         Voice-First | Persistent | Context-Aware               │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
[Telephony] [Business] [Dev/Code] [Intel]  [Evolution]
 Agent       Agent      Agent     Agent     Agent
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
[MCP Layer: GitHub | Playwright | Zapier | Monday | HuggingFace | Firecrawl]
   │
   ▼
[External Services: Deepgram | ElevenLabs | Twilio | Firebase | GCP]
```

### 2.1 Layer Definitions

| Layer | Name | Purpose |
|---|---|---|
| L0 | Centauri OS | Device-level Android OS replacement; Caroline is the UI |
| L1 | Caroline Orchestrator | Central hub; routes intent, manages context, speaks |
| L2 | Specialized Agents | Domain-specific agents (telephony, business, code, intel) |
| L3 | Auto-Tooling Engine | Identifies missing capabilities and generates new tools |
| L4 | MCP Servers | Standardized external service integrations |
| L5 | Mobile/Desktop Interfaces | Android app, IDE extensions, web dashboards |
| L6 | Construction Vertical | WCC-specific logic: estimates, projects, CRM, pricebook |

---

## 3. Complete Asset Catalog

This section maps every uploaded file and concept to its role and target location in the repository.

### 3.1 Core Agentic Engines (Kotlin — Android)

All of these belong under `mobile_desktop_ai_interfaces/caroline_superapp/` and form the engine layer of the Android application.

| File | Class | Role | Target Module |
|---|---|---|---|
| `AutoToolingEngine.kt` | `AutoToolingEngine` | Detects missing capabilities; generates new tools dynamically via AI; registers and tests them at runtime | `core/tooling/` |
| `AutonomousEvolutionEngine.kt` | `AutonomousEvolutionEngine` | Enables the AI to learn from interactions, adapt behavior, and evolve its own code over time | `core/evolution/` |
| `LocalAIEngine.kt` | `LocalAIEngine` | Manages on-device LLM inference (Phi-3, ONNX); provides offline AI capability | `core/inference/` |
| `HybridDataStreamEngine.kt` | `HybridDataStreamEngine` | Manages local + cloud data streams; real-time sync between device and backend | `core/data/` |
| `AdaptiveAIPromptingSystem.kt` | `AdaptiveAIPromptingSystem` | Analyzes context, optimizes prompts, evaluates responses, adapts personality; learning engine | `core/prompting/` |
| `NeuralInterfaceSystem.kt` | `NeuralInterfaceSystem` | Sensor fusion (accelerometer, gyroscope, etc.); device-aware context for the AI | `core/neural/` |
| `OpenSourceIntelligenceEngine.kt` | `OpenSourceIntelligenceEngine` | Gathers intelligence from GitHub, StackOverflow, documentation, and code analysis | `core/intelligence/` |
| `Web3IntegrationSystem.kt` | `Web3IntegrationSystem` | Blockchain/Web3 connectivity; wallet integration; decentralized data access | `core/web3/` |
| `PlayStorePublishingSystem.kt` | `PlayStorePublishingSystem` | Manages app publishing pipeline to Google Play Store | `core/publishing/` |
| `RealTimeDeploymentSystem.kt` | `RealTimeDeploymentSystem` | Handles real-time app updates, hot-patching, and deployment orchestration | `core/deployment/` |

### 3.2 UI & Conversation Layer (Kotlin — Android)

| File | Class | Role | Target Module |
|---|---|---|---|
| `GeminiStyleChatInterface.kt` | Composable UI | Primary chat interface; Gemini-style design with Jetpack Compose | `ui/chat/` |
| `ChatViewModel.kt` | `ChatViewModel` | Chat state management; connects UI to AI engines and API calls | `ui/chat/` |
| `ConversationManager.kt` | `ConversationManager` | Manages conversation history, context memory, and user preferences | `core/conversation/` |
| `MainViewModel.kt` | `MainViewModel` | Top-level app state; connects all engines to the main UI | `ui/main/` |
| `MainActivity.kt` | `MainActivity` | Entry point; permission handling; Hilt injection root | `app/` |
| `MasterCoderApplication.kt` | `MasterCoderApplication` | Application class; initializes all engines and logging | `app/` |
| `ai_code_assist_ui(2).kt` | `IDEScreen` | Full IDE interface with AI code assist, voice commands, file management | `ui/ide/` |

### 3.3 App Configuration & Build

| File | Role | Notes |
|---|---|---|
| `complete_AndroidManifest.xml` | Full permissions manifest | Includes RECORD_AUDIO, CALL_SCREENING, SYSTEM_ALERT_WINDOW, BLUETOOTH, NFC, FOREGROUND_SERVICE, INSTALL_PACKAGES, biometrics |
| `complete_build.gradle` | Full dependency set | Hilt, Compose BOM, Navigation, OkHttp, Timber, Coroutines, Material3 |
| `AndroidManifest.xml` | Base manifest | Simpler version; merge with complete_ version |
| `build.gradle` | Base build file | Merge with complete_ version |
| `strings.xml` | App strings | UI copy and labels |
| `Typography.kt` | Type system | Compose typography definitions |
| `google-services.json` | Firebase config | Firebase project credentials for WCC |

### 3.4 Multi-Agent Orchestrator (Python)

**File:** `pasted_content.txt` → Target: `agentic_core/orchestrator/multi_agent_orchestrator.py`

This is a working Python multi-agent orchestrator using the Grok (xAI) API. Key capabilities:
- Central `Orchestrator` agent with tool-calling loop
- Dynamic agent registration (`add_agent`)
- Dynamic tool injection (`add_tool_to_agent`) — this is the Python equivalent of `AutoToolingEngine`
- Agent handoff (`handoff_to_agent`)
- Parallel task execution via threading (`run_parallel_tasks`)
- Self-building: users can add agents and tools at runtime via `/add_agent` commands

**Integration:** This Python orchestrator is the **backend brain**. The Android app (Caroline) is the **voice/mobile frontend**. They communicate via API.

### 3.5 WCC Pro Engine (JavaScript/Firebase)

**Source:** `wcc_pro_engine.zip` → Target: `construction_ai/wcc_pro/`

| File | Content |
|---|---|
| `caroline-core.js` | Caroline's behavioral logic: task enforcement, reinforcement weighting, command interpretation, revenue-generating action detection |
| `wcc-app-logic.js` | Project creation, task templates (bathroom remodel, etc.), client management |
| `firebase-schema.json` | Firestore schema: `clients`, `projects`, `tasks`, `estimates` collections |
| `commands.map.json` | Natural language → function mapping: "create estimate" → `estimates.create`, "add client" → `crm.addClient`, etc. |

### 3.6 Centauri OS & Caroline Design Proposals

**Source:** `Centauri_SuperApp_Release.zip` → Target: `agentic_core/caroline_soul_core_pack/centauri_design/`

| Document | Content Summary |
|---|---|
| `Centauri_OS_and_Caroline_AI__Full_Design_Proposal.md` | **Master design document** — 7 chapters covering: Core Architecture, Foundational Capabilities, Voice Interface Design, Backend Memory & Learning, Admin CLI & Developer Interface, API Permissions & Web Access, Security & User Control |
| `Centauri_NeuralLogic_Chain_Definitions.docx` | Neural logic chain definitions for Caroline's reasoning pathways |
| `Centauri_Packet1A_ModularAutomation_Revised.docx` | Modular automation framework; how Caroline executes multi-step workflows |
| `Centauri_Packet2A_AutonomationOS_Revised.docx` | Autonomation OS spec; how Centauri OS handles autonomous task execution |

**Key Design Principles from Centauri Proposal:**
- Caroline replaces the Android launcher/UI entirely
- Voice is primary input; always-on wake word detection
- Short-term memory (working memory) + long-term memory (Firebase/Firestore)
- Admin Command Line Interface (ACLI) for developer/power-user access
- Firebase/Firestore for persistent memory and cross-device sync
- GCP for heavy compute tasks
- Contextual awareness: time, location, device state, user history

### 3.7 Telephony & Receptionist Systems

| Source | Target | Content |
|---|---|---|
| `telephony_agent_app_enhanced.zip` | `telephony_voice_ai/telephony_agent/` | Full Android telephony agent: `CallScreener`, `WorkflowAgent`, `AiProcessor`, `ApiManager`, `ModelLoader`, `SecretManager` |
| `CarolineReceptionistTechSpecV3.pdf` | `telephony_voice_ai/caroline_receptionist/TECH_SPEC_V3.md` | Chapter 5: Android telephony surfaces, permission matrix, Play Store policy boundaries, carrier constraints, risk register |
| `deepgramvopiceagentandsdk.txt` | `telephony_voice_ai/DG-voice/` | Deepgram Voice Agent SDK integration: STT (Nova-3), TTS (Aura-2), real-time audio streaming config |
| `deepgramapiownerrole.txt` | Secure secrets store | Deepgram API key + Google API key (do NOT commit to repo) |

**Deepgram Integration Stack:**
- STT: `nova-3` model
- TTS: `aura-2-thalia-en` voice
- LLM Think Layer: `gpt-4-mini` (OpenAI) or swap for Grok
- Audio: `linear16` input, `mp3` output at 24kHz

### 3.8 Construction Business Management Specs

| Source | Target | Content |
|---|---|---|
| `Constuctpro.Blitzy.AI.Technical.Specification.pdf` | `construction_ai/Constructpro/TECH_SPEC.md` | 323-page spec: AI Agent System, Lead Management, Assembly Library, Material Takeoff Engine, Universal Navigation, Mobile Field Operations |
| `CSR.AIBlitzyAITechnicalSpecification.pdf` | `construction_ai/csr.ai/TECH_SPEC.md` | 280-page spec: AI-Powered Construction Pro Dialer — automated call handling, lead qualification, material ordering workflows, real-time call monitoring |
| `pricebook_materials_template.csv` | `construction_ai/wcc_pro/data/pricebook_materials_template.csv` | Materials pricebook template: HVAC parts, cleaning products, garage door parts — category hierarchy, pricing, markup flags |
| `mindmap.png` | `construction_ai/wcc_pro/docs/home_assembly_mindmap.png` | **Home Remodeling Assembly Logic Pattern** — complete mind map of all home assemblies |

### 3.9 Home Remodeling Assembly Mind Map

The uploaded mind map (`mindmap.png`) defines the complete **Assembly Logic Pattern** for remodeling a home. This is the foundational taxonomy for the construction AI's understanding of project scope and sequencing.

**Top-Level Assembly Categories:**

| Category | Sub-Assemblies |
|---|---|
| **Structural Shell** | Foundation (footings, slab, waterproofing), Framing (load-bearing, roof trusses, subfloor), Exterior Envelope (siding, roofing, windows, exterior doors) |
| **Mechanical Systems** | Plumbing (supply, DWV, fixtures, water heater), HVAC (furnace, ductwork, thermostat, insulation), Electrical (service panel, branch circuits, lighting, smart home) |
| **Interior Assemblies** | Kitchens (cabinets, counters, appliances, sink, backsplash, lighting), Bathrooms (vanity, shower/tub, toilet, tile, ventilation), Doors & Trim (interior doors, hardware, baseboards, window casing, crown), Flooring (subfloor prep, hardwood/laminate/tile/carpet, underlayment, transitions), Windows (rough opening, unit install, casing, weatherstripping) |
| **Finishes & Surfaces** | Paint/wall finishes, Tile & grout, Countertop finishes, Sealants/caulking |
| **Site & Exterior Features** | Landscaping, Driveways, Decks/porches, Exterior lighting |
| **Project Management** | Schedule/phasing (demo → structural → rough-ins → inspections → finishes), Budgeting by assembly, Permits & codes, Subcontractor roles, Quality control & punch list |
| **Interfaces & Integration** | Structural-to-finish transitions, Mechanical penetrations, Waterproofing at wet assemblies, Electrical/AV in cabinets, Thermal & moisture control |
| **Reuse & Sustainability** | Salvageable components, Energy-efficient systems, Low-VOC finishes, Waste management |

**Key Design Principle from Mind Map:** *"Treat each listed item as a modular 'child assembly' that can be specified, procured, installed, inspected, and managed semi-independently while maintaining defined interfaces to the parent home assembly."*

### 3.10 Local AI Model Infrastructure

| File | Role | Target |
|---|---|---|
| `Phi3ModelDownloader_Version2.java` | Downloads Phi-3 Mini ONNX model to device; moves from external to internal storage | `mobile_desktop_ai_interfaces/model-downloader/` |
| `LocalAIEngine.kt` | On-device inference engine using downloaded ONNX model | `mobile_desktop_ai_interfaces/caroline_superapp/core/inference/` |

**Local Model Stack:** Phi-3 Mini 4K (ONNX) → Android DownloadManager → Internal storage → ONNX Runtime inference

### 3.11 Supporting Infrastructure

| File | Role | Target |
|---|---|---|
| `NeuralInterfaceSystem(2).kt` | Updated version of NeuralInterfaceSystem | Supersedes original; use this version |
| `Caroline_Centauri_SuperApp_Complete(1).zip` | Scaffold of Caroline Android app (stub files) | Reference for project structure |
| `caroline_android_complete_project.zip` | Another Caroline Android scaffold | Reference for project structure |
| `ai-filter-blocker-complete.tar.gz` | AI filter bypass/blocker system | `agentic_core/caroline_soul_core_pack/ai-filter-blocker-complete/` |
| `playwright-mcp-extension-0.0.40.zip` | Playwright MCP browser extension | `mcp_servers/playwright-mcp/` |

---

## 4. Integration Roadmap

### Phase 1: Foundation (Current)
- [x] Repository structure established (`unified-agentic-ai-foundation`)
- [x] All raw assets cataloged (this document)
- [ ] Copy all Kotlin engine files into `mobile_desktop_ai_interfaces/caroline_superapp/`
- [ ] Copy Python orchestrator into `agentic_core/orchestrator/`
- [ ] Copy WCC Pro Engine into `construction_ai/wcc_pro/`
- [ ] Copy Centauri design docs into `agentic_core/caroline_soul_core_pack/centauri_design/`
- [ ] Copy telephony agent into `telephony_voice_ai/telephony_agent/`

### Phase 2: Core Wiring
- [ ] Connect Python orchestrator to Android app via REST API
- [ ] Integrate Deepgram STT/TTS into telephony layer
- [ ] Wire `AutoToolingEngine` to `AutonomousEvolutionEngine`
- [ ] Connect `ConversationManager` to Firebase long-term memory
- [ ] Integrate `LocalAIEngine` (Phi-3) as offline fallback

### Phase 3: Business Logic
- [ ] Implement WCC command map in Caroline's intent routing
- [ ] Build estimate generation agent using Assembly Mind Map taxonomy
- [ ] Connect pricebook CSV to material takeoff engine
- [ ] Integrate ConstructPro AI agent spec into project management module

### Phase 4: Telephony & Receptionist
- [ ] Deploy `CallScreener` + `WorkflowAgent` from telephony_agent
- [ ] Integrate Deepgram Voice Agent for live call handling
- [ ] Build IVR tree for WCC inbound calls
- [ ] Connect call data to CRM (Firebase `clients` collection)

### Phase 5: Evolution & Autonomy
- [ ] Enable `AutoToolingEngine` to call LLM for tool generation
- [ ] Connect `AutonomousEvolutionEngine` to performance telemetry
- [ ] Build admin CLI (ACLI) per Centauri spec Chapter 5
- [ ] Enable Caroline to add new agents at runtime

---

## 5. Key API Keys & Credentials

> **SECURITY NOTE:** These are cataloged here for reference only. Store all keys in environment variables or a secrets manager — NEVER commit to the repository.

| Service | Key Reference | Use |
|---|---|---|
| Deepgram | `deepgramapiownerrole.txt` | STT (Nova-3), TTS (Aura-2), Voice Agent |
| Google APIs | `deepgramapiownerrole.txt` | NNGBP (Next Gen Business Platform) |
| Firebase | `google-services.json` | Firestore DB, Auth, Storage |
| Twilio | `Centauri_SuperApp_Release.zip/twilio_2FA_recovery_code.txt` | Telephony, 2FA |
| ElevenLabs | Environment: `ELEVENLABS_API_KEY` | Caroline's voice synthesis |
| Grok/xAI | Environment: `XAI_API_KEY` | Multi-agent orchestrator LLM |
| OpenAI | Environment: `OPENAI_API_KEY` | Deepgram think layer, fallback LLM |

---

## 6. Technology Stack Summary

| Layer | Technology |
|---|---|
| **Mobile App** | Android (Kotlin, Jetpack Compose, Hilt, Coroutines, Material3) |
| **On-Device AI** | Phi-3 Mini 4K ONNX via Android ONNX Runtime |
| **Cloud LLM** | Grok (xAI) primary, GPT-4 Mini fallback |
| **Voice STT** | Deepgram Nova-3 |
| **Voice TTS** | ElevenLabs (Caroline's voice) + Deepgram Aura-2 |
| **Telephony** | Android CallScreeningService + Deepgram Voice Agent + Twilio |
| **Backend Orchestration** | Python (multi-agent orchestrator, CrewAI, FastAPI) |
| **Database** | Firebase Firestore (CRM, projects, memory) + Neon Postgres |
| **External Integrations** | MCP servers: GitHub, Playwright, Zapier, Monday, HuggingFace, Firecrawl |
| **Construction Logic** | Node.js (WCC Pro Engine) + Python agents |
| **Web Frontend** | Next.js (AI chatbot interface) |
| **Build/Deploy** | Gradle (Android), GitHub Actions, Vercel (web) |

---

## 7. Repository Directory Map

```
unified-agentic-ai-foundation/
│
├── agentic_core/                          # L1-L3: Orchestration & AI Reasoning
│   ├── orchestrator/                      # Python multi-agent orchestrator
│   │   └── multi_agent_orchestrator.py    # ← from pasted_content.txt
│   ├── caroline_soul_core_pack/           # Caroline's core identity & behavior
│   │   ├── centauri_design/               # ← from Centauri_SuperApp_Release.zip
│   │   ├── AutonomousEvolutionEngine.kt   # ← already present
│   │   └── VoiceInterfaces.kt             # ← already present
│   ├── action_engineering_team/           # CrewAI engineering team
│   ├── action_deep_research/              # Deep research agent
│   └── action_trading_floor/             # Trading/market agent (reference)
│
├── construction_ai/                       # L6: WCC Business Vertical
│   ├── wcc_pro/                           # ← NEW: from wcc_pro_engine.zip
│   │   ├── caroline-core.js
│   │   ├── wcc-app-logic.js
│   │   ├── firebase-schema.json
│   │   ├── commands.map.json
│   │   └── data/pricebook_materials_template.csv
│   ├── Constructpro/                      # ConstructPro AI spec
│   ├── csr.ai/                            # CSR.AI dialer spec
│   ├── Bms/                               # Business management system
│   └── ngbp-v2-0/                         # Next Gen Business Platform Android
│
├── mobile_desktop_ai_interfaces/          # L5: User Interfaces
│   ├── caroline_superapp/                 # ← NEW: Primary Caroline Android App
│   │   ├── app/
│   │   │   ├── core/
│   │   │   │   ├── tooling/AutoToolingEngine.kt
│   │   │   │   ├── evolution/AutonomousEvolutionEngine.kt
│   │   │   │   ├── inference/LocalAIEngine.kt
│   │   │   │   ├── data/HybridDataStreamEngine.kt
│   │   │   │   ├── prompting/AdaptiveAIPromptingSystem.kt
│   │   │   │   ├── neural/NeuralInterfaceSystem.kt
│   │   │   │   ├── intelligence/OpenSourceIntelligenceEngine.kt
│   │   │   │   ├── web3/Web3IntegrationSystem.kt
│   │   │   │   ├── conversation/ConversationManager.kt
│   │   │   │   ├── publishing/PlayStorePublishingSystem.kt
│   │   │   │   └── deployment/RealTimeDeploymentSystem.kt
│   │   │   └── ui/
│   │   │       ├── chat/GeminiStyleChatInterface.kt
│   │   │       ├── chat/ChatViewModel.kt
│   │   │       ├── main/MainViewModel.kt
│   │   │       └── ide/IDEScreen.kt
│   │   └── AndroidManifest.xml (complete)
│   ├── model-downloader/                  # Phi-3 model downloader
│   ├── ai-agent-android/                  # Existing AI agent Android
│   ├── Signal-Android/                    # Signal-based comms layer
│   ├── Hface-IDE/                         # HuggingFace IDE extension
│   └── nextjs-ai-chatbot/                 # Web chat interface
│
├── telephony_voice_ai/                    # L3: Voice & Call Handling
│   ├── telephony_agent/                   # ← Enhanced telephony agent
│   │   ├── CallScreener.kt
│   │   ├── WorkflowAgent.kt
│   │   ├── AiProcessor.kt
│   │   └── ApiManager.kt
│   ├── DG-voice/                          # Deepgram voice integration
│   ├── caroline_receptionist/             # Caroline receptionist spec
│   ├── voice-ai-app/
│   └── smart-incallservice/
│
└── mcp_servers/                           # L4: External Service Integrations
    ├── github-mcp-server/
    ├── playwright-mcp/
    ├── hf-mcp-server/
    ├── firecrawl-mcp-server/
    ├── zapier-mcp/
    └── mcp-monday/
```

---

## 8. Cross-Cutting Concerns

### 8.1 Memory Architecture
Caroline uses a **three-tier memory system**:
1. **Working Memory** — In-process `ConversationManager` (session context, last N turns)
2. **Short-Term Persistent** — Local SQLite/Room database on device
3. **Long-Term Cloud Memory** — Firebase Firestore (`clients`, `projects`, `conversations`, `learned_preferences`)

### 8.2 Voice Pipeline
```
Microphone → Deepgram Nova-3 (STT) → Caroline Orchestrator → Response Text
→ ElevenLabs (TTS) → Speaker
```
For telephony calls:
```
Incoming Call → CallScreeningService → Deepgram Voice Agent → WorkflowAgent
→ Caroline Orchestrator → Response → Deepgram Aura-2 TTS → Caller
```

### 8.3 Tool Generation Flow (AutoToolingEngine)
```
Agent identifies missing capability
→ AutoToolingEngine.needTool(description)
→ ProblemAnalyzer.analyzeProblem()
→ WorkaroundGenerator.generateUltimateWorkaround()
→ LLM generates tool code
→ Tool registered in tool registry
→ Tool tested
→ Tool available to all agents
```

### 8.4 Construction Workflow (WCC)
```
Lead comes in (call/web)
→ Caroline answers (CallScreener + Deepgram)
→ Lead qualified (WorkflowAgent)
→ Client added to Firestore (crm.addClient)
→ Project created (projects.createProject)
→ Estimate generated (estimates.create using Assembly Mind Map)
→ Proposal sent
→ Project managed (tasks, materials, schedule)
→ Invoice sent (finance.sendInvoice)
```

---

## 9. Notes & Decisions

- The `NeuralInterfaceSystem(2).kt` supersedes the original `NeuralInterfaceSystem.kt` — use version 2.
- The `complete_AndroidManifest.xml` and `complete_build.gradle` are the authoritative versions; the simpler `AndroidManifest.xml` and `build.gradle` should be merged into them.
- The `Caroline_Centauri_SuperApp_Complete(1).zip` and `caroline_android_complete_project.zip` contain stub/scaffold files only — use as structural reference, not as source of truth for code.
- The `PluginList.csv` was empty (0 bytes) — no data to catalog.
- Twilio 2FA recovery code is in `Centauri_SuperApp_Release.zip` — store securely, not in repo.
- The `ai-filter-blocker` system is already present in `agentic_core/caroline_soul_core_pack/` — the new version from the zip should be compared and merged.

---

*This document is a living synthesis. As new raw data is provided, it will be updated and expanded.*
