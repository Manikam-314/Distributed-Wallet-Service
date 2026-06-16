import asyncio
import json
import traceback
from app.services.llm_service import llm_engine

async def main():
    try:
        res = await llm_engine.extract_reminder('I will send 500 tomorrow')
        print("RESULT_JSON:", res.model_dump_json())
    except Exception as e:
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
