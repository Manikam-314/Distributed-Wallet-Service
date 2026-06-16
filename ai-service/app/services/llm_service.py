import json
import re
import dateparser
from datetime import datetime
import google.generativeai as genai
from pydantic import ValidationError
from typing import Dict, Any, Optional

from app.config.settings import settings
from app.config.logging_config import logger
from app.models.schemas import ReminderExtractionResponse

# Configure LLM provider statically for this example, focusing on Gemini as per the prompt request.
# In a real app we could use a factory pattern based on `settings.llm_provider`
if settings.gemini_api_key:
    genai.configure(api_key=settings.gemini_api_key)
    # Using the standard genai model. You could swap for pro/flash as needed.
    _model = genai.GenerativeModel('gemini-1.5-flash-latest')
else:
    _model = None
    logger.warning("No Gemini API key supplied. LLM fallback mode will be highly active.")

PROMPT_TEMPLATE = """
You are a production-grade NLP and AI extraction engine for a fintech application.
Your task is to extract structured payment reminder information from informal user messages.

Input text: "{text}"

Extraction Rules:
1. AMOUNT: Extract numeric monetary values (₹, $, etc.). If multiple amounts, choose the most relevant OR mark confidence low. If no amount, return null.
2. DATE (CRITICAL): You MUST extract relative and absolute dates ("tomorrow", "today", "next week", "next friday", "tonight", "by evening", "later"). Convert to exact ISO 8601 format (YYYY-MM-DDTHH:MM:SS) or null if ambiguous. NEVER ignore clear words like "tomorrow". 
3. INTENT: "I will send", "I'll give" -> "repay". "send me", "pay me" -> "request". Otherwise -> "unknown".
4. CONFIDENCE SCORING:
   - amount only -> 0.5
   - amount + intent -> 0.6
   - amount + date -> 0.75
   - amount + date + intent -> 0.85-0.95
   - ambiguous inputs -> reduce confidence
5. VALIDATION: valid = true ONLY IF amount is present AND dueDate is valid (not null). Otherwise valid = false.
6. EDGE CASES:
   - "send later" -> dueDate = null, valid = false
   - "garbage input" -> safe defaults (nulls, unknown, false)
7. STRICT RULES: DO NOT hallucinate values. DO NOT skip date extraction. ONLY reply with valid JSON format.

Output JSON Format:
{{
  "amount": number|null,
  "dueDate": "string"|null,
  "intent": "repay"|"request"|"unknown",
  "confidence": number,
  "valid": boolean
}}
"""

class LLMEngine:
    
    async def extract_reminder(self, message: str) -> ReminderExtractionResponse:
        logger.info(f"Extracting reminder from text: {message}")

        # ALWAYS RUN FALLBACK CHECK FIRST
        fallback = self._fallback_extract(message)

        llm_result = None
        if _model:
            try:
                prompt = PROMPT_TEMPLATE.format(text=message)
                response = await _model.generate_content_async(prompt)

                raw_text = response.text.strip('` \n').removeprefix('json')
                parsed_json = json.loads(raw_text)

                llm_result = parsed_json

            except Exception as e:
                logger.error(f"LLM failed: {e}")

        # MERGE LOGIC
        amount = fallback.amount
        due_date = fallback.dueDate
        intent = fallback.intent
        
        llm_success = False

        if llm_result:
            llm_success = True
            if llm_result.get("amount") is not None:
                amount = llm_result.get("amount")
            if llm_result.get("dueDate") is not None:
                due_date = llm_result.get("dueDate")
            if llm_result.get("intent") and llm_result.get("intent") != "unknown":
                intent = llm_result.get("intent")

        # VALIDATION RULE (STRICT)
        valid = bool(amount is not None and due_date is not None)

        # CONFIDENCE FIX
        confidence = 0.0
        if amount is not None and due_date is not None and intent != "unknown":
            confidence = 0.85
            if llm_success and llm_result.get("amount") is not None and llm_result.get("dueDate") is not None:
                confidence = 0.95
        elif amount is not None and due_date is not None:
            confidence = 0.75
        elif amount is not None and intent != "unknown":
            confidence = 0.6
        elif amount is not None:
            confidence = 0.5
        else:
            confidence = 0.2

        if fallback.dueDate and (llm_result is None or llm_result.get("dueDate") is None):
            confidence += 0.05
            confidence = min(confidence, 1.0)

        final = {
            "amount": amount,
            "dueDate": due_date,
            "intent": intent,
            "confidence": confidence,
            "valid": valid
        }

        res = ReminderExtractionResponse(**final)

        logger.info(f"FINAL RESULT: {res.model_dump_json()}")

        return res
    def _fallback_extract(self, message: str) -> ReminderExtractionResponse:
        """Rule-based extraction when LLM fails or is unavailable."""
        logger.info("Using Regex/Rule-based fallback for reminder extraction.")
        
        # 1. Regex for amount - ignore if followed by time units (seconds, minutes, hours, days)
        # Use negative lookahead to ignore if followed by time-related words
        amount_match = re.search(r'(?:₹|\$|rs\.?)?\s*(\d+(?:\.\d{1,2})?)(?!\s*(?:sec|min|hour|day|week|month|year))', message.lower())
        amount = float(amount_match.group(1)) if amount_match else None
        
        # 2. Simple intent guessing
        intent = "unknown"
        if any(w in message.lower() for w in ['pay', 'send', 'give', 'repay', 'will give']):
            intent = "repay"
        elif any(w in message.lower() for w in ['request', 'need', 'ask']):
            intent = "request"
            
        # 3. Date extraction fallback using dateparser
        due_date = None
        
        # Explicit handling for common relative terms to avoid dateparser "hallucination"
        low_msg = message.lower()
        from datetime import timedelta
        base_now = datetime.utcnow()
        
        if 'tomorrow' in low_msg:
            due_date = (base_now + timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        elif 'today' in low_msg or 'tonight' in low_msg:
            due_date = base_now.replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        
        if not due_date:
            # Remove common words that dateparser misinterprets (like "pay" -> payday -> 30th)
            msg_for_date = re.sub(r'\b(pay|send|give|rs|rs\.|inr|request|money|for)\b', '', low_msg)
            # Remove amount-only numbers (ignore if followed by time units like seconds)
            msg_for_date = re.sub(r'(?:₹|\$|rs\.?)?\s*(\d+(?:\.\d{1,2})?)(?!\s*(?:sec|min|hour|day|week|month|year))', '', msg_for_date)
            
            import dateparser.search
            dates = dateparser.search.search_dates(msg_for_date, settings={'TIMEZONE': 'UTC', 'PREFER_DATES_FROM': 'future'})
            if dates:
                dt = dates[0][1]
                # If message implies a specific time (seconds, minutes, hours), keep full precision
                # Otherwise, default to start-of-day for general "tomorrow" etc
                if any(word in message.lower() for word in ['second', 'minute', 'hour', 'am', 'pm', ':']):
                    due_date = dt.strftime('%Y-%m-%dT%H:%M:%SZ')
                else:
                    due_date = dt.replace(hour=0, minute=0, second=0, microsecond=0).strftime('%Y-%m-%dT%H:%M:%SZ')
            
        # Valid if we found a due date (amount is auxiliary if not in text)
        valid = bool(due_date is not None)
        
        # If amount is not found in text, it's fine (we use the transaction amount anyway)
        if amount is None:
            amount = 0.0
        
        # Calculate confidence using the new specific strict rules
        confidence = 0.0
        if amount is not None and due_date is not None and intent != "unknown":
            confidence = 0.85
        elif amount is not None and due_date is not None:
            confidence = 0.75
        elif amount is not None and intent != "unknown":
            confidence = 0.6
        elif amount is not None:
            confidence = 0.5
        else:
            confidence = 0.2
            
        return ReminderExtractionResponse(
            amount=amount,
            dueDate=due_date,
            intent=intent,
            confidence=confidence,
            valid=valid
        )

llm_engine = LLMEngine()
