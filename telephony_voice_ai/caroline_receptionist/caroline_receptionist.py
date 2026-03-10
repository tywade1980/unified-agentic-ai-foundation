"""
Caroline Receptionist — Unified Telephony Service
==================================================
Merges three previous telephony modules into one authoritative service:
  1. nextgentele/      — Node.js carrier/IVR/dialer system (ported to Python)
  2. telephony_agent/  — Android CallScreeningService + on-device Phi-3 Mini
  3. smart-incallservice/ — Enhanced in-call AI service

Architecture:
  Inbound call → Deepgram STT (real-time streaming) → Grok Orchestrator
              → ElevenLabs TTS → Caller hears Caroline's voice

  Outbound/screening → CallScreeningService → Intent classification
                     → Allow / Block / Voicemail / Transfer to WCC_Pro

LLM:   Grok (xAI)      — XAI_API_KEY
Voice: ElevenLabs TTS  — ELEVENLABS_API_KEY
STT:   Deepgram        — DEEPGRAM_API_KEY
Calls: Twilio          — TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER

Run:
    uvicorn caroline_receptionist:app --host 0.0.0.0 --port 8001 --reload
"""

import os, json, asyncio, re, time
from datetime import datetime
from typing import Optional, Dict, List
import httpx
from fastapi import FastAPI, Request, Response, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
import uvicorn

# ── API Keys ─────────────────────────────────────────────────────────────────
XAI_API_KEY           = os.environ.get("XAI_API_KEY", "")
ELEVENLABS_API_KEY    = os.environ.get("ELEVENLABS_API_KEY", "")
DEEPGRAM_API_KEY      = os.environ.get("DEEPGRAM_API_KEY", "")
TWILIO_ACCOUNT_SID    = os.environ.get("TWILIO_ACCOUNT_SID", "")
TWILIO_AUTH_TOKEN     = os.environ.get("TWILIO_AUTH_TOKEN", "")
TWILIO_PHONE_NUMBER   = os.environ.get("TWILIO_PHONE_NUMBER", "")
ORCHESTRATOR_URL      = os.environ.get("ORCHESTRATOR_URL", "http://localhost:8000")

ELEVENLABS_VOICE_ID   = os.environ.get("ELEVENLABS_VOICE_ID", "21m00Tcm4TlvDq8ikWAM")
ELEVENLABS_MODEL      = "eleven_turbo_v2_5"

# ── Caroline's Persona ────────────────────────────────────────────────────────
CAROLINE_SYSTEM_PROMPT = """
You are Caroline, the professional AI receptionist and business assistant for
Wade Custom Carpentry (WCC), a premier interior trim carpentry and custom
remodeling company based in Columbus, Ohio, owned by Mr. T Wade with 25 years
of experience.

Your role on phone calls:
- Answer professionally: "Thank you for calling Wade Custom Carpentry, this is Caroline."
- Qualify leads: project type, location, timeline, budget range
- Schedule estimates and consultations
- Answer FAQs about WCC services (trim carpentry, kitchen/bath remodels, custom woodwork)
- Screen spam/solicitation calls politely but firmly
- Escalate urgent calls or existing client issues to Mr. Wade
- Log all call details to the WCC Pro system

Tone: Warm, professional, knowledgeable about construction and carpentry.
Keep responses concise for voice — under 3 sentences unless asked for detail.
Columbus OH service area. Business hours: Mon-Fri 7am-6pm, Sat 8am-2pm.
"""

# ─────────────────────────────────────────────────────────────────────────────
# App Setup
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title="Caroline Receptionist",
    description="Unified telephony AI service for Wade Custom Carpentry.",
    version="2.0.0"
)

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# Active call sessions
active_calls: Dict[str, Dict] = {}

# ─────────────────────────────────────────────────────────────────────────────
# Models
# ─────────────────────────────────────────────────────────────────────────────

class CallRecord(BaseModel):
    call_sid: str
    caller_number: str
    caller_name: str = "Unknown"
    start_time: str
    status: str = "active"
    transcript: List[str] = []
    intent: str = "unknown"
    lead_qualified: bool = False
    notes: str = ""

class ScreeningDecision(BaseModel):
    call_sid: str
    action: str   # "allow" | "block" | "voicemail" | "transfer"
    reason: str

# ─────────────────────────────────────────────────────────────────────────────
# Health & Status
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/")
async def root():
    return {
        "service": "Caroline Receptionist v2.0",
        "status": "operational",
        "active_calls": len(active_calls),
        "keys_loaded": {
            "xai": bool(XAI_API_KEY),
            "elevenlabs": bool(ELEVENLABS_API_KEY),
            "deepgram": bool(DEEPGRAM_API_KEY),
            "twilio": bool(TWILIO_ACCOUNT_SID)
        },
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health")
async def health():
    return {"status": "healthy", "active_calls": len(active_calls)}

# ─────────────────────────────────────────────────────────────────────────────
# Twilio Webhook — Inbound Call Handler
# Configure your Twilio number's Voice webhook to POST to /twilio/inbound
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/twilio/inbound")
async def twilio_inbound(request: Request):
    """
    Twilio calls this when an inbound call arrives.
    Returns TwiML that connects the caller to Caroline's voice pipeline.
    """
    form = await request.form()
    call_sid      = form.get("CallSid", "unknown")
    caller_number = form.get("From", "unknown")
    caller_name   = form.get("CallerName", "Unknown Caller")

    # Register the call
    active_calls[call_sid] = {
        "call_sid": call_sid,
        "caller_number": caller_number,
        "caller_name": caller_name,
        "start_time": datetime.now().isoformat(),
        "transcript": [],
        "intent": "unknown"
    }

    # Generate Caroline's opening greeting via ElevenLabs
    greeting = "Thank you for calling Wade Custom Carpentry, this is Caroline. How can I help you today?"
    audio_url = await _synthesize_to_url(greeting)

    # Return TwiML — stream audio and gather speech
    twiml = f"""<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Play>{audio_url}</Play>
    <Gather input="speech" action="/twilio/speech" method="POST"
            speechTimeout="auto" language="en-US" enhanced="true">
    </Gather>
    <Redirect>/twilio/inbound</Redirect>
</Response>"""

    return PlainTextResponse(content=twiml, media_type="application/xml")

@app.post("/twilio/speech")
async def twilio_speech(request: Request):
    """
    Receives caller speech from Twilio Gather, processes through Grok,
    responds with ElevenLabs TTS audio.
    """
    form = await request.form()
    call_sid    = form.get("CallSid", "unknown")
    speech_text = form.get("SpeechResult", "").strip()
    confidence  = float(form.get("Confidence", "0.0"))

    if not speech_text or confidence < 0.4:
        twiml = """<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say>I'm sorry, I didn't catch that. Could you please repeat?</Say>
    <Gather input="speech" action="/twilio/speech" method="POST" speechTimeout="auto">
    </Gather>
</Response>"""
        return PlainTextResponse(content=twiml, media_type="application/xml")

    # Log transcript
    call_data = active_calls.get(call_sid, {})
    call_data.setdefault("transcript", []).append(f"Caller: {speech_text}")

    # Classify intent
    intent = _classify_intent(speech_text)
    call_data["intent"] = intent

    # Get Caroline's response from Grok via orchestrator
    response_text = await _get_caroline_response(speech_text, call_sid)
    call_data.setdefault("transcript", []).append(f"Caroline: {response_text}")

    # Check if we should transfer or end the call
    if intent == "transfer_to_owner":
        twiml = _build_transfer_twiml(response_text)
    elif intent == "end_call":
        twiml = _build_goodbye_twiml(response_text)
    elif intent == "voicemail":
        twiml = _build_voicemail_twiml()
    else:
        audio_url = await _synthesize_to_url(response_text)
        twiml = f"""<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Play>{audio_url}</Play>
    <Gather input="speech" action="/twilio/speech" method="POST"
            speechTimeout="auto" language="en-US" enhanced="true">
    </Gather>
    <Redirect>/twilio/inbound</Redirect>
</Response>"""

    # Log to WCC Pro
    asyncio.create_task(_log_call_to_wcc(call_sid, speech_text, response_text, intent))

    return PlainTextResponse(content=twiml, media_type="application/xml")

@app.post("/twilio/status")
async def twilio_call_status(request: Request):
    """Twilio status callback — updates call record when call ends."""
    form = await request.form()
    call_sid = form.get("CallSid", "unknown")
    status   = form.get("CallStatus", "unknown")
    duration = form.get("CallDuration", "0")

    if call_sid in active_calls:
        active_calls[call_sid]["status"] = status
        active_calls[call_sid]["duration_seconds"] = int(duration)
        active_calls[call_sid]["end_time"] = datetime.now().isoformat()

    return Response(status_code=204)

# ─────────────────────────────────────────────────────────────────────────────
# Call Screening (Android CallScreeningService integration)
# The Android app POSTs incoming call info here for a screening decision
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/screen", response_model=ScreeningDecision)
async def screen_call(call_sid: str, caller_number: str, caller_name: str = ""):
    """
    Called by the Android CallScreeningService before a call rings.
    Returns a decision: allow, block, voicemail, or transfer.
    """
    # Known spam patterns
    spam_patterns = ["+1800", "+1888", "+1877", "+1866", "unavailable", "unknown"]
    if any(p in caller_number.lower() for p in spam_patterns):
        return ScreeningDecision(call_sid=call_sid, action="block", reason="Likely spam/robocall")

    # Check if it's a known client (would query Firebase in production)
    # For now, allow all non-spam calls
    return ScreeningDecision(call_sid=call_sid, action="allow", reason="Passed screening")

# ─────────────────────────────────────────────────────────────────────────────
# WebSocket — Real-time voice streaming (for direct Deepgram integration)
# ─────────────────────────────────────────────────────────────────────────────

@app.websocket("/ws/voice/{session_id}")
async def voice_websocket(websocket: WebSocket, session_id: str):
    """
    WebSocket endpoint for real-time voice streaming.
    Client sends raw PCM audio chunks → server streams to Deepgram → 
    gets transcript → sends to Grok → returns ElevenLabs TTS audio.
    """
    await websocket.accept()
    conversation_history = []

    try:
        while True:
            data = await websocket.receive()

            if "text" in data:
                # Text message — treat as typed command
                message = data["text"]
                response = await _get_caroline_response(message, session_id, conversation_history)
                audio_bytes = await _synthesize_speech(response)
                await websocket.send_json({
                    "type": "response",
                    "text": response,
                    "has_audio": audio_bytes is not None
                })
                if audio_bytes:
                    await websocket.send_bytes(audio_bytes)

            elif "bytes" in data:
                # Audio bytes — transcribe with Deepgram then process
                audio_chunk = data["bytes"]
                transcript = await _transcribe_audio_chunk(audio_chunk)
                if transcript:
                    await websocket.send_json({"type": "transcript", "text": transcript})
                    response = await _get_caroline_response(transcript, session_id, conversation_history)
                    audio_bytes = await _synthesize_speech(response)
                    await websocket.send_json({"type": "response", "text": response})
                    if audio_bytes:
                        await websocket.send_bytes(audio_bytes)

    except WebSocketDisconnect:
        pass

# ─────────────────────────────────────────────────────────────────────────────
# Active Calls Management
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/calls")
async def list_calls():
    return {"active_calls": list(active_calls.values()), "count": len(active_calls)}

@app.get("/calls/{call_sid}")
async def get_call(call_sid: str):
    if call_sid not in active_calls:
        raise HTTPException(status_code=404, detail="Call not found")
    return active_calls[call_sid]

# ─────────────────────────────────────────────────────────────────────────────
# Private Helpers
# ─────────────────────────────────────────────────────────────────────────────

async def _get_caroline_response(
    user_message: str,
    session_id: str,
    history: Optional[List[Dict]] = None
) -> str:
    """Get a response from Grok via the orchestrator /chat endpoint."""
    if history is None:
        history = []

    # Build messages with Caroline's system prompt
    messages = [{"role": "system", "content": CAROLINE_SYSTEM_PROMPT}]
    messages.extend(history[-10:])  # last 5 turns
    messages.append({"role": "user", "content": user_message})

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                f"{ORCHESTRATOR_URL}/chat",
                json={"agent": "Orchestrator", "message": user_message, "session_id": f"receptionist_{session_id}"}
            )
            if resp.status_code == 200:
                data = resp.json()
                response_text = data.get("response", "")
                history.append({"role": "user", "content": user_message})
                history.append({"role": "assistant", "content": response_text})
                return _trim_for_voice(response_text)
    except Exception:
        pass

    # Fallback: call Grok directly
    return await _call_grok_direct(messages)

async def _call_grok_direct(messages: List[Dict]) -> str:
    """Direct Grok API call as fallback when orchestrator is unavailable."""
    if not XAI_API_KEY:
        return "I'm sorry, I'm having trouble connecting right now. Please call back shortly."
    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            resp = await client.post(
                "https://api.x.ai/v1/chat/completions",
                headers={"Authorization": f"Bearer {XAI_API_KEY}", "Content-Type": "application/json"},
                json={"model": "grok-3-mini", "messages": messages, "max_tokens": 200}
            )
            data = resp.json()
            return _trim_for_voice(data["choices"][0]["message"]["content"])
    except Exception as e:
        return "Thank you for calling Wade Custom Carpentry. We're unable to take your call right now. Please leave a message."

async def _synthesize_speech(text: str) -> Optional[bytes]:
    """Synthesize speech using ElevenLabs eleven_turbo_v2_5."""
    if not ELEVENLABS_API_KEY:
        return None
    voice_text = _trim_for_voice(text)
    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            resp = await client.post(
                f"https://api.elevenlabs.io/v1/text-to-speech/{ELEVENLABS_VOICE_ID}",
                headers={"xi-api-key": ELEVENLABS_API_KEY, "Content-Type": "application/json"},
                json={
                    "text": voice_text,
                    "model_id": ELEVENLABS_MODEL,
                    "voice_settings": {"stability": 0.5, "similarity_boost": 0.75, "use_speaker_boost": True}
                }
            )
            if resp.status_code == 200:
                return resp.content
    except Exception:
        pass
    return None

async def _synthesize_to_url(text: str) -> str:
    """
    Synthesize speech and return a URL Twilio can play.
    In production, upload to S3/GCS and return the URL.
    For now, returns a Twilio TTS fallback.
    """
    # TODO: Upload ElevenLabs audio to S3 and return URL
    # For now, use Twilio's built-in TTS as fallback
    return f"https://api.twilio.com/cowbell.mp3"  # placeholder

async def _transcribe_audio_chunk(audio_bytes: bytes) -> str:
    """Transcribe audio chunk using Deepgram nova-3."""
    if not DEEPGRAM_API_KEY:
        return ""
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                "https://api.deepgram.com/v1/listen?model=nova-3&smart_format=true",
                headers={"Authorization": f"Token {DEEPGRAM_API_KEY}", "Content-Type": "audio/wav"},
                content=audio_bytes
            )
            if resp.status_code == 200:
                data = resp.json()
                return (data.get("results", {})
                           .get("channels", [{}])[0]
                           .get("alternatives", [{}])[0]
                           .get("transcript", ""))
    except Exception:
        pass
    return ""

async def _log_call_to_wcc(call_sid: str, caller_message: str, response: str, intent: str):
    """Log call interaction to the WCC Pro agent."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            call_data = active_calls.get(call_sid, {})
            log_message = (
                f"Log phone call: caller {call_data.get('caller_number', 'unknown')}, "
                f"intent: {intent}, message: '{caller_message[:100]}'"
            )
            await client.post(
                f"{ORCHESTRATOR_URL}/chat",
                json={"agent": "WCC_Pro", "message": log_message, "session_id": f"call_{call_sid}"}
            )
    except Exception:
        pass

def _classify_intent(text: str) -> str:
    """Classify caller intent from transcript."""
    lower = text.lower()
    if any(k in lower for k in ["speak to", "talk to", "get mr", "get wade", "owner", "manager", "urgent"]):
        return "transfer_to_owner"
    if any(k in lower for k in ["estimate", "quote", "how much", "price", "cost"]):
        return "estimate_request"
    if any(k in lower for k in ["schedule", "appointment", "book", "available", "when"]):
        return "schedule_request"
    if any(k in lower for k in ["goodbye", "bye", "thank you", "that's all", "no thank you"]):
        return "end_call"
    if any(k in lower for k in ["voicemail", "leave a message", "message"]):
        return "voicemail"
    if any(k in lower for k in ["complaint", "problem", "issue", "wrong", "broken"]):
        return "complaint"
    return "general_inquiry"

def _trim_for_voice(text: str) -> str:
    """Strip markdown and trim to voice-friendly length."""
    text = re.sub(r'#{1,6}\s+', '', text)
    text = re.sub(r'\*{1,2}([^*]+)\*{1,2}', r'\1', text)
    text = re.sub(r'`[^`]+`', '', text)
    text = re.sub(r'\|[^\n]+\|', '', text)
    text = re.sub(r'\n{2,}', ' ', text)
    text = re.sub(r'\s{2,}', ' ', text).strip()
    if len(text) > 400:
        text = text[:397] + "..."
    return text

def _build_transfer_twiml(response_text: str) -> str:
    owner_number = os.environ.get("OWNER_PHONE_NUMBER", "+16145550000")
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say>{response_text}</Say>
    <Dial timeout="20" callerId="{TWILIO_PHONE_NUMBER}">
        <Number>{owner_number}</Number>
    </Dial>
    <Say>Mr. Wade is unavailable right now. Please leave a message after the tone.</Say>
    <Record maxLength="120" transcribe="true" transcribeCallback="/twilio/transcription"/>
    <Hangup/>
</Response>"""

def _build_goodbye_twiml(response_text: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say>{response_text} Thank you for calling Wade Custom Carpentry. Have a great day!</Say>
    <Hangup/>
</Response>"""

def _build_voicemail_twiml() -> str:
    return """<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say>Please leave your name, number, and a brief message after the tone. We'll get back to you within one business day.</Say>
    <Record maxLength="120" transcribe="true" transcribeCallback="/twilio/transcription"/>
    <Say>Thank you. Goodbye!</Say>
    <Hangup/>
</Response>"""

# ─────────────────────────────────────────────────────────────────────────────
# Entry Point
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = int(os.environ.get("RECEPTIONIST_PORT", 8001))
    print(f"\n{'='*60}")
    print(f"  Caroline Receptionist v2.0")
    print(f"  LLM:   Grok (xAI) — grok-3-mini")
    print(f"  TTS:   ElevenLabs — {ELEVENLABS_MODEL}")
    print(f"  STT:   Deepgram — nova-3")
    print(f"  Tel:   Twilio")
    print(f"  Port:  {port}")
    print(f"{'='*60}\n")
    uvicorn.run("caroline_receptionist:app", host="0.0.0.0", port=port, reload=True)
