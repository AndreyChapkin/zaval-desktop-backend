CREATE SEQUENCE TODO_SEQ
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE TODOS (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(10000),
    status VARCHAR(50),
    priority INTEGER,
    created_on TIMESTAMP,
    interacted_on TIMESTAMP,
    parent_id INTEGER,
    FOREIGN KEY (parent_id) REFERENCES TODOS(id) ON DELETE CASCADE
);

CREATE SEQUENCE ARTICLE_SEQ
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE ARTICLES (
    id INTEGER PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,
    interacted_on TIMESTAMP
);