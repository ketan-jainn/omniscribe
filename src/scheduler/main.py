import asyncio

from src.shared.kafka import KafkaConsumer
from src.shared.kafka.topics import TOPIC_INGRESS
from src.shared.log import configure_logging, get_logger


configure_logging()
logger = get_logger(__name__)


async def main() -> None:
    logger.info("Scheduler starting", topic=TOPIC_INGRESS)
    consumer = KafkaConsumer(
        group_id="omniscribe-scheduler",
        topics=[TOPIC_INGRESS],
    )

    try:
        logger.info("Scheduler started. Waiting for ingress messages.")
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                await asyncio.sleep(0)
                continue
            if msg.error():
                logger.error("Scheduler poll error", error=str(msg.error()))
                continue

            logger.info(
                "Scheduler received message",
                topic=msg.topic(),
                partition=msg.partition(),
                offset=msg.offset(),
            )
            consumer.commit(msg)
    finally:
        consumer.close()


if __name__ == "__main__":
    asyncio.run(main())
