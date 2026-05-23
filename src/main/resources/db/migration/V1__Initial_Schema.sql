-- V1__Initial_Schema.sql
-- Multi-tenant Team Chat System Schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Workspaces (tenants)
CREATE TABLE workspaces (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Users
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(50) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url   VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- Workspace memberships (users belong to workspaces)
CREATE TABLE workspace_members (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER, ADMIN, MEMBER
    joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    invited_by   UUID REFERENCES users(id),
    UNIQUE (workspace_id, user_id)
);

-- Channels
CREATE TABLE channels (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    is_private   BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    is_archived  BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (workspace_id, name)
);

-- Channel memberships
CREATE TABLE channel_members (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id   UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_read_at TIMESTAMP,
    UNIQUE (channel_id, user_id)
);

-- Messages (append-only, ordered by sequence within channel)
CREATE TABLE messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id   UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id),
    content      TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',  -- TEXT, SYSTEM, FILE
    sequence_num BIGINT NOT NULL,  -- monotonically increasing per channel
    client_msg_id VARCHAR(100),    -- idempotency key from client
    reply_to     UUID REFERENCES messages(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (channel_id, sequence_num),
    UNIQUE (channel_id, client_msg_id)
);

-- Message delivery acknowledgements
CREATE TABLE message_acks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    acked_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (message_id, user_id)
);

-- Invitations
CREATE TABLE invitations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    token        VARCHAR(255) NOT NULL UNIQUE,
    invited_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP,
    is_used      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Channel sequence counter (for ordered messages)
CREATE TABLE channel_sequences (
    channel_id   UUID PRIMARY KEY REFERENCES channels(id) ON DELETE CASCADE,
    last_seq     BIGINT NOT NULL DEFAULT 0
);

-- Indexes for performance
CREATE INDEX idx_messages_channel_seq ON messages(channel_id, sequence_num DESC);
CREATE INDEX idx_messages_channel_created ON messages(channel_id, created_at DESC);
CREATE INDEX idx_messages_workspace ON messages(workspace_id);
CREATE INDEX idx_workspace_members_user ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_workspace ON workspace_members(workspace_id);
CREATE INDEX idx_channel_members_user ON channel_members(user_id);
CREATE INDEX idx_channel_members_channel ON channel_members(channel_id);
CREATE INDEX idx_channels_workspace ON channels(workspace_id);
CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_email ON invitations(email);