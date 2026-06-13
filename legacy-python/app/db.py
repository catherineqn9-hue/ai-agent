import os
from contextlib import contextmanager
from collections.abc import Iterator

import psycopg
from psycopg.rows import dict_row


DEFAULT_DATABASE_URL = "postgresql://sherry:sherry_dev_password@127.0.0.1:5432/sherry"


def database_url() -> str:
    return os.getenv("DATABASE_URL", DEFAULT_DATABASE_URL)


@contextmanager
def get_connection() -> Iterator[psycopg.Connection]:
    conn = psycopg.connect(database_url(), row_factory=dict_row)
    try:
        yield conn
    finally:
        conn.close()
