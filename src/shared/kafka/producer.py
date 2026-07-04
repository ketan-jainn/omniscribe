from confluent_kafka import Producer
from pydantic import BaseModel

from src.shared.config import settings


class KafkaProducer:
    def __init__(self, bootstrap_servers: str | None = None):
        self._producer = Producer(
            {"bootstrap.servers": bootstrap_servers or settings.KAFKA_BOOTSTRAP_SERVERS}
        )

    def publish(self, topic: str, key: str, message: BaseModel) -> None:
        self._producer.produce(
            topic=topic,
            key=key.encode("utf-8"),
            value=message.model_dump_json().encode("utf-8"),
        )
        self._producer.poll(0)

    def flush(self, timeout: float = 10.0) -> None:
        self._producer.flush(timeout)
