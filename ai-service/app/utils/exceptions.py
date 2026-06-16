from fastapi import HTTPException
from starlette.status import HTTP_400_BAD_REQUEST, HTTP_500_INTERNAL_SERVER_ERROR

class AIProcessingException(HTTPException):
    def __init__(self, detail: str, status_code: int = HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(status_code=status_code, detail=detail)

class ModelNotLoadedException(AIProcessingException):
    def __init__(self, model_name: str):
        super().__init__(detail=f"Machine Learning model '{model_name}' is not loaded or missing.")

class LLMEngineException(AIProcessingException):
    def __init__(self, detail: str = "Failed to process text with LLM."):
        super().__init__(detail=detail, status_code=HTTP_500_INTERNAL_SERVER_ERROR)

class InvalidInputDataException(AIProcessingException):
    def __init__(self, detail: str):
        super().__init__(detail=detail, status_code=HTTP_400_BAD_REQUEST)
