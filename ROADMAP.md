# ROADMAP: Unified Agentic AI Foundation

This document outlines the development roadmap for the Wade Ecosystem, a unified AI operating system for Wade Custom Carpentry and the Caroline AI platform. Our goal is to build a comprehensive, multi-agent "Super App" that acts as a conversational operating system for the business.

---

## Phase 1: Core Integration (Complete)

- [x] **Catalog All Assets:** Read and catalog all 50+ uploaded files, specs, and codebases.
- [x] **Establish Hub Project:** Identify `unified-agentic-ai-foundation` as the central repository.
- [x] **Integrate WCC Skill:** Update the `wade-custom-carpentry` skill to be the master system skill.
- [x] **Wire WCC Pro Agent:** Implement the `multi_agent_orchestrator.py` with the WCC Pro agent as a callable spoke, complete with all business logic tools.
- [x] **Build UI Foundation:** Create the `ConstructionDashboard.kt` UI screen and the main `AppNavigation.kt` host in the `caroline_superapp`.
- [x] **Document Architecture:** Create this `ROADMAP.md` and the `PRE_PUSH_REVIEW.md`.

---

## Phase 2: UI & ViewModel Wiring

- [ ] **Create `ConstructionViewModel.kt`:** Build the ViewModel to bridge the `ConstructionDashboardScreen` with the backend orchestrator. It will handle state for projects, estimates, and actions.
- [ ] **Wire `onVoiceCommand`:** Connect the UI's `onVoiceCommand` lambda to a function in the ViewModel that sends the query to the Python orchestrator.
- [ ] **Render Live Data:** Replace the sample project data in the UI with live data fetched from the WCC Pro Agent via the ViewModel.
- [ ] **Integrate `GeminiStyleChatInterface`:** Replace the `ChatPlaceholderScreen` with the full `GeminiStyleChatInterface` and wire it to its own `ChatViewModel`.
- [ ] **Integrate `IDEScreen`:** Replace the `IDEPlaceholderScreen` with the full `IDEScreen.kt` and wire it to a `IDEViewModel` for file system access and code execution.

---

## Phase 3: End-to-End Feature Implementation

- [ ] **Full Voice Pipeline:** Implement the end-to-end voice-to-voice loop: Android Mic -> Deepgram STT -> Orchestrator -> Agent Logic -> ElevenLabs TTS -> Android Speaker.
- [ ] **Automated Material Sourcing:** Build out the `Researcher` agent's ability to perform automated web searches on supplier websites using the Playwright MCP server. The WCC Pro agent will call this to get live material pricing.
- [ ] **Telephony Service Merge:** Refactor the three separate telephony modules (`telephony_agent`, `smart-incallservice`, `CSR.AI` spec) into a single, robust `caroline_receptionist` service within the `telephony_voice_ai` directory.
- [ ] **Database Integration:** Connect the WCC Pro agent to a live Firebase/Firestore database using the `firebase-schema.json`. All CRM, project, and estimate data will be persisted.

---

## Phase 4: Advanced AI & Self-Evolution

- [ ] **Activate AutoTooler:** Fully implement the `AutoTooler` agent. The orchestrator will call it when a user asks for a capability that doesn't exist, and the agent will write and register the new tool in real-time.
- [ ] **On-Device Model:** Integrate the `LocalAIEngine.kt` to run the Phi-3 model directly on the Android device for offline capabilities and low-latency tasks.
- [ ] **Autonomous Evolution:** Wire the `AutonomousEvolutionEngine.kt` to allow the system to analyze its own performance and suggest or even implement improvements to its own codebase.

---

## Phase 5: Deployment & Production

- [ ] **CI/CD Pipeline:** Set up a GitHub Actions workflow to automatically build, test, and create a release APK for the `caroline_superapp`.
- [ ] **Play Store Publishing:** Use the `PlayStorePublishingSystem.kt` module to automate the process of uploading new releases to the Google Play Store.
- [ ] **Server Deployment:** Deploy the Python `multi_agent_orchestrator.py` to a persistent cloud server (e.g., RunPod, as mentioned in the `wade-ecosystem` skill) so it's always available to the app.
