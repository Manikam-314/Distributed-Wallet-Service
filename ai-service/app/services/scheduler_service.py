from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from app.config.logging_config import logger
from app.config.settings import settings
from app.utils.kafka_producer import publish_notification

def process_reminders_job():
    """
    Simulates checking the database for reminders that are due and pushing to Kafka.
    In a real system, you would query the 'Reminder' table where dueDate <= NOW() and status == 'PENDING'.
    """
    logger.info("Scheduler Triggered: Checking for due reminders...")
    
    # Example simulated payload:
    # 1. Query: SELECT * FROM Reminder WHERE status = 'PENDING' AND dueDate <= NOW()
    # 2. Iterate:
    # 3. Mark status = 'PROCESSING'
    # 4. Push to notification queue
    # 5. Mark status = 'COMPLETED'
    
    # Simulated pending reminder found:
    simulated_reminder = {
        "reminderId": "123e4567-e89b-12d3-a456-426614174000",
        "senderId": "U1001",
        "receiverId": "U2005",
        "amount": 500,
        "message": "Payment reminder for amount 500",
        "dueDate": "2024-05-15T12:00:00Z"
    }
    
    # Try sending notification via Kafka (if enabled)
    success = publish_notification("reminder.due", simulated_reminder)
    
    if success:
        logger.info(f"Successfully processed and published reminder {simulated_reminder['reminderId']}")
    
def start_scheduler():
    scheduler = BackgroundScheduler()
    # Run at the top of every minute
    scheduler.add_job(
        process_reminders_job,
        trigger=CronTrigger(minute='*'),
        id='process_reminders',
        name='Process Pending Reminders',
        replace_existing=True
    )
    scheduler.start()
    logger.info("Background Scheduler started. Jobs scheduled.")
