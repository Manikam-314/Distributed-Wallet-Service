from confluent_kafka import Producer
import json
from app.config.settings import settings
from app.config.logging_config import logger

_producer = None

if settings.enable_kafka:
    try:
        conf = {'bootstrap.servers': settings.kafka_bootstrap_servers}
        _producer = Producer(**conf)
        logger.info(f"Kafka Producer initialized for bootstrap: {settings.kafka_bootstrap_servers}")
    except Exception as e:
        logger.error(f"Failed to initialize Kafka Producer: {e}")

def delivery_report(err, msg):
    if err is not None:
        logger.error(f"Message delivery failed: {err}")
    else:
        logger.debug(f"Message delivered to {msg.topic()} [{msg.partition()}]")

def publish_notification(event_type: str, payload: dict) -> bool:
    """
    Publishes an event to the notification topic.
    Returns True if published, False if Kafka is disabled or failed.
    """
    if not settings.enable_kafka or not _producer:
        logger.info(f"[MOCK KAFKA] Would publish event '{event_type}' with payload: {payload}")
        return False
        
    try:
        topic = settings.kafka_notifications_topic
        envelope = {
            "eventType": event_type,
            "payload": payload
        }
        _producer.produce(
            topic,
            value=json.dumps(envelope).encode('utf-8'),
            callback=delivery_report
        )
        _producer.poll(0)
        return True
    except Exception as e:
        logger.error(f"Error publishing to Kafka: {e}")
        return False
