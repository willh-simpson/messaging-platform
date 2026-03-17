-- USERS
CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ua_users_email UNIQUE (email)
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

COMMENT ON TABLE users IS 'Platform user accounts';
COMMENT ON COLUMN users.password_has IS 'BCrypt hash of user password';

-- CHANNELS
CREATE TYPE CHANNEL_TYPE AS ENUM ('PUBLIC', 'PRIVATE');

CREATE TABLE channels (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    channel_type CHANNEL_TYPE NOT NULL DEFAULT 'PUBLIC',
    created_by   UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_channels PRIMARY KEY (id),
    CONSTRAINT fk_channels_created_by FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE CASCADE 
);

CREATE INDEX idx_channels_created_by ON channels (created_by);
CREATE INDEX idx_channels_type ON channels (channel_type);
 
COMMENT ON TABLE channels IS 'Chat channels. Can be PUBLIC or PRIVATE.';

-- CHANNEL MEMBERS
CREATE TYPE member_role AS ENUM ('OWNER', 'ADMIN', 'MEMBER');
 
CREATE TABLE channel_members (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    channel_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       member_role NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
 
    CONSTRAINT pk_channel_members PRIMARY KEY (id),
    CONSTRAINT uq_channel_member UNIQUE (channel_id, user_id),
    CONSTRAINT fk_members_channel FOREIGN KEY (channel_id) REFERENCES channels(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);
 
CREATE INDEX idx_channel_members_channel ON channel_members (channel_id);
CREATE INDEX idx_channel_members_user ON channel_members (user_id);
 
COMMENT ON TABLE channel_members IS 'Join table between users and channels. Tracks membership and role.';

-- Auto-updated updated_at on row modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_channels_updated_at
    BEFORE UPDATED ON channels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();