CREATE TABLE CODES_MAPPING
(
    ID                   NUMBER(10,0) NOT NULL,
    ELECTION_EVENT_ID    VARCHAR2(100 CHAR) NOT NULL,
    JSON                 CLOB NOT NULL,
    TENANT_ID            VARCHAR2(100 CHAR) NOT NULL,
    VERIFICATION_CARD_ID VARCHAR2(100 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT CODES_MAPPING_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID)
);

CREATE TABLE VERIFICATION_CONTENT
(
    ID                       NUMBER(10,0) NOT NULL,
    ELECTION_EVENT_ID        VARCHAR2(100 CHAR) NOT NULL,
    JSON                     CLOB NOT NULL,
    TENANT_ID                VARCHAR2(100 CHAR) NOT NULL,
    VERIFICATION_CARD_SET_ID VARCHAR2(100 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT VERIFICATION_CONTENT_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
);

CREATE TABLE VERIFICATION_DATA
(
    ID                             NUMBER(10,0) NOT NULL,
    ELECTION_EVENT_ID              VARCHAR2(255 CHAR) NOT NULL,
    SIGNED_VERIFICATION_PUBLIC_KEY CLOB NOT NULL,
    TENANT_ID                      VARCHAR2(255 CHAR) NOT NULL,
    VERIFICATION_CARD_ID           VARCHAR2(255 CHAR) NOT NULL,
    VERIFICATION_CARD_KEYSTORE     CLOB NOT NULL,
    VERIFICATION_CARD_SET_ID       VARCHAR2(255 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT VERIFICATION_DATA_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID)
);

CREATE TABLE VERIFICATION_DERIVED_KEYS
(
    ID                           NUMBER(10,0) NOT NULL,
    BCK_DERIVED_EXP_COMMITMENT   CLOB,
    CCODE_DERIVED_KEY_COMMITMENT CLOB,
    ELECTION_EVENT_ID            VARCHAR2(255 CHAR),
    TENANT_ID                    VARCHAR2(255 CHAR),
    VERIFICATION_CARD_ID         VARCHAR2(255 CHAR),
    PRIMARY KEY (ID),
    CONSTRAINT VERIFICATION_DERIVED_KEYS_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID)
);

CREATE TABLE VERIFICATION_SET_DATA
(
    ID                       NUMBER(10,0) NOT NULL,
    JSON                     CLOB NOT NULL,
    ELECTION_EVENT_ID        VARCHAR2(255 CHAR) NOT NULL,
    SIGNATURE                CLOB,
    TENANT_ID                VARCHAR2(255 CHAR) NOT NULL,
    VERIFICATION_CARD_SET_ID VARCHAR2(255 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT VERIFICATION_SET_DATA_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
);

CREATE TABLE ELECTION_PUBLIC_KEY
(
    ID                     NUMBER(10,0) NOT NULL,
    ELECTION_EVENT_ID      VARCHAR2(100 CHAR) NOT NULL,
    ELECTORAL_AUTHORITY_ID VARCHAR2(100 CHAR) NOT NULL,
    JWT                    CLOB NOT NULL,
    TENANT_ID              VARCHAR2(100 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT ELECTION_PUBLIC_KEY_UK1 UNIQUE (TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID)
);

CREATE TABLE PLATFORM_CERTIFICATE
(
    ID                  NUMBER(19,0) NOT NULL,
    CERTIFICATE_CONTENT CLOB NOT NULL,
    CERTIFICATE_NAME    VARCHAR2(100 CHAR) NOT NULL,
    PLATFORM_NAME       VARCHAR2(100 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT PLATFORM_CERTIFICATE_UK1 UNIQUE (PLATFORM_NAME, CERTIFICATE_NAME)
);

CREATE TABLE TENANT_KEYSTORE
(
    ID               NUMBER(19,0) NOT NULL,
    KEY_TYPE         VARCHAR2(100 CHAR) NOT NULL,
    KEYSTORE_CONTENT CLOB NOT NULL,
    PLATFORM_NAME    VARCHAR2(100 CHAR),
    TENANT_ID        VARCHAR2(100 CHAR) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT TENANT_KEYSTORE_UK1 UNIQUE (PLATFORM_NAME, KEY_TYPE, TENANT_ID)
);

CREATE SEQUENCE CODES_MAPPING_SEQ;
CREATE SEQUENCE VERIFICATION_CONTENT_SEQ;
CREATE SEQUENCE VERIFICATION_DATA_SEQ;
CREATE SEQUENCE VERIFICATION_SET_DATA_SEQ;
CREATE SEQUENCE ELECTION_PUBLIC_KEY_SEQ;
CREATE SEQUENCE PLATFORM_CERTIFICATE_SEQ;
CREATE SEQUENCE TENANT_KEYSTORE_SEQ;
