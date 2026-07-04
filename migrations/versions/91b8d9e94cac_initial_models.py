"""add users table

Revision ID: 91b8d9e94cac
Revises: 2c594e18677e
Create Date: 2026-06-06 14:38:03.146385

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '91b8d9e94cac'
down_revision: Union[str, Sequence[str], None] = '2c594e18677e'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        'users',
        sa.Column('id', sa.String(length=128), nullable=False),
        sa.Column('plan', sa.String(length=32), nullable=False),
        sa.Column('rate_limit_tier', sa.String(length=32), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('users')
