from collections.abc import Sequence

from confluent_kafka import Consumer

from src.shared.config import settings


class KafkaConsumer:
    def __init__(
        self,
        group_id: str,
        topics: Sequence[str],
        bootstrap_servers: str | None = None,
    ):
        self._consumer = Consumer(
            {
                "bootstrap.servers": bootstrap_servers or settings.KAFKA_BOOTSTRAP_SERVERS,
                "group.id": group_id,
                "auto.offset.reset": "earliest",
                "enable.auto.commit": False,
            }
        )
        self._consumer.subscribe(list[str](topics))

    def poll(self, timeout: float = 1.0):
        return self._consumer.poll(timeout=timeout)

    def commit(self, message) -> None:
        self._consumer.commit(message=message, asynchronous=False)

    def close(self) -> None:
        self._consumer.close()
