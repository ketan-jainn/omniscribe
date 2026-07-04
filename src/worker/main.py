import asyncio

from src.shared.kafka import KafkaConsumer
from src.shared.kafka.topics import TOPIC_JOBS
from src.shared.log import configure_logging, get_logger


configure_logging()
logger = get_logger(__name__)


async def main() -> None:
    logger.info("Worker starting", topic=TOPIC_JOBS)
    consumer = KafkaConsumer(
        group_id="whisper-workers",
        topics=[TOPIC_JOBS],
    )

    logger.info("Worker started. Waiting for messages...")
    try:
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                await asyncio.sleep(0)
                continue
            if msg.error():
                logger.error("Worker poll error", error=str(msg.error()))
                continue

            logger.info(
                "Worker received message",
                topic=msg.topic(),
                partition=msg.partition(),
                offset=msg.offset(),
            )
            consumer.commit(msg)
    finally:
        consumer.close()


if __name__ == "__main__":
    asyncio.run(main())
