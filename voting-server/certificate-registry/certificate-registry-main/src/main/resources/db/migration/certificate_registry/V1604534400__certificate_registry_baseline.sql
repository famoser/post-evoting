CREATE TABLE CERTIFICATE
(
    ID                  NUMBER(19,0) NOT NULL,
    CERTIFICATE_CONTENT CLOB NOT NULL,
    CERTIFICATE_NAME    VARCHAR2(100 CHAR) NOT NULL,
    PLATFORM_NAME       VARCHAR2(100 CHAR) NOT NULL,
    ELECTION_EVENT_ID   VARCHAR2(100 CHAR),
    TENANT_ID           VARCHAR2(100 CHAR),
    PRIMARY KEY (ID),
    CONSTRAINT CERTIFICATE_UK1 UNIQUE (CERTIFICATE_NAME, PLATFORM_NAME, TENANT_ID)
);

CREATE SEQUENCE CERTIFICATE_SEQ;