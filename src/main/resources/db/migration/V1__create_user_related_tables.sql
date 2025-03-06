CREATE TABLE addresses
(
    id SERIAL PRIMARY KEY,
    zip_code VARCHAR(255) NOT NULL,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(255) NOT NULL,
    complement VARCHAR(255) NULL,
    neighborhood VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL DEFAULT 'Brasil',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE roles
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE permissions
(
    id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE document_types
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    validation_regex VARCHAR(500) NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX index_unique_document_type
    ON document_types (name, country_code)
    WHERE deleted_at IS NULL;

INSERT INTO document_types (name, country_code, validation_regex, description)
VALUES
    ('CPF', 'BR', '^[0-9]{3}\.[0-9]{3}\.[0-9]{3}\-[0-9]{2}$', 'Brazilian individual taxpayer registry'),
    ('SSN', 'US', '^[0-9]{3}-[0-9]{2}-[0-9]{4}$', 'US Social Security Number'),
    ('NIN', 'GB', '^[A-Z]{2}[0-9]{6}[A-Z]$', 'UK National Insurance Number'),
    ('CUIT', 'AR', '^[0-9]{2}-[0-9]{8}-[0-9]$', 'Argentine tax identification number'),
    ('PASSPORT', 'INTL', NULL, 'International passport'),
    ('RNE', 'BR', NULL, 'National Registry of Foreigners in Brazil');

CREATE TABLE users
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    bio VARCHAR(500) NULL,
    phone VARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    email_confirmed_at TIMESTAMP NULL,
    external_customer_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    address_id INTEGER NULL,
    CONSTRAINT fk_address_id FOREIGN KEY (address_id) REFERENCES addresses(id)
);

CREATE TABLE user_documents
(
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    document_type_id INTEGER NOT NULL,
    document_number VARCHAR(255) NOT NULL,
    issuing_country VARCHAR(50) NULL,
    issue_date DATE NULL,
    expiry_date DATE NULL,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_user_documents_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_documents_document_type_id FOREIGN KEY (document_type_id) REFERENCES document_types(id)
);

CREATE UNIQUE INDEX index_unique_user_document
    ON user_documents (document_type_id, document_number)
    WHERE deleted_at IS NULL;

CREATE TABLE user_roles
(
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE role_permissions
(
    id SERIAL PRIMARY KEY,
    role_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE UNIQUE INDEX index_unique_email
    ON users (email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX index_unique_role_name
    ON roles (name)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX index_unique_description
    ON permissions (description)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX index_unique_user_role
    ON user_roles (user_id, role_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX index_unique_role_permission
    ON role_permissions (role_id, permission_id)
    WHERE deleted_at IS NULL;