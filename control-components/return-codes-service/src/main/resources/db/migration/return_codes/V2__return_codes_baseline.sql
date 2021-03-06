CREATE TABLE CC_NODE_KEYS
(
    NODE_ID VARCHAR2(10) NOT NULL,
    KEYS    BLOB NOT NULL,
    CONSTRAINT CC_NODE_KEYS_PK PRIMARY KEY (NODE_ID)
);

CREATE TABLE CC_ELECTION_SIGNING_KEYS
(
    NODE_ID           VARCHAR2(10) NOT NULL,
    ELECTION_EVENT_ID VARCHAR2(100) NOT NULL,
    KEYS              BLOB NOT NULL,
    PASSWORD          BLOB NOT NULL,
    CONSTRAINT CC_ELECTION_SIGNING_KEYS_PK PRIMARY KEY (NODE_ID, ELECTION_EVENT_ID)
);

CREATE TABLE CCR_RETURN_CODES_KEYS
(
    NODE_ID                                                  VARCHAR2(10) NOT NULL,
    ELECTION_EVENT_ID                                        VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_SET_ID                                 VARCHAR2(100) NOT NULL,
    CCRJ_RETURN_CODES_GENERATION_SECRET_KEY                  BLOB NOT NULL,
    CCRJ_RETURN_CODES_GENERATION_PUBLIC_KEY                  BLOB NOT NULL,
    CCRJ_RETURN_CODES_GENERATION_PUBLIC_KEY_SIGNATURE        BLOB NOT NULL,
    CCRJ_CHOICE_RETURN_CODES_ENCRYPTION_SECRET_KEY           BLOB NOT NULL,
    CCRJ_CHOICE_RETURN_CODES_ENCRYPTION_PUBLIC_KEY           BLOB NOT NULL,
    CCRJ_CHOICE_RETURN_CODES_ENCRYPTION_PUBLIC_KEY_SIGNATURE BLOB NOT NULL,
    CONSTRAINT CCR_RETURN_CODES_KEYS_PK PRIMARY KEY (NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
);

CREATE TABLE CC_COMBINED_CORRECTNESS_INFORMATION
(
    ELECTION_EVENT_ID                VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_SET_ID         VARCHAR2(100) NOT NULL,
    COMBINED_CORRECTNESS_INFORMATION BLOB NOT NULL,
    CONSTRAINT CC_COMBINED_CORRECTNESS_INFORMATION_PK PRIMARY KEY (ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
);

CREATE TABLE CC_VERIFICATION_CARD_PUBLIC_KEY
(
    ELECTION_EVENT_ID            VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_ID         VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_SET_ID     VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_PUBLIC_KEY BLOB NOT NULL,
    CONSTRAINT CC_VERIFICATION_CARD_PUBLIC_KEY PRIMARY KEY (VERIFICATION_CARD_ID)
);

CREATE TABLE COMPUTED_VERIFICATION_CARDS
(
    ELECTION_EVENT_ID       VARCHAR2(100) NOT NULL,
    VERIFICATION_CARD_ID    VARCHAR2(100) NOT NULL,
    CONFIRMATION_ATTEMPTS   INTEGER DEFAULT 0   NOT NULL,
    EXPONENTIATION_COMPUTED CHAR(1) DEFAULT 'N' NOT NULL,
    CONSTRAINT COMPUTED_VERIFICATION_CARDS_PK PRIMARY KEY (ELECTION_EVENT_ID, VERIFICATION_CARD_ID)
);
