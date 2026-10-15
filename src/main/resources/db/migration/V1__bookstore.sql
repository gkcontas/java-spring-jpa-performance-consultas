-- A catalogue shaped to provoke the query plans the project is about: a
-- to-one that every listing needs (publisher), two collections on the same
-- aggregate (authors and reviews, which is what makes a double fetch join
-- cartesian), and an optional one-to-one on the inverse side.

CREATE TABLE publisher (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(120) NOT NULL,
    country VARCHAR(60)  NOT NULL
);

CREATE TABLE author (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    nationality VARCHAR(60)  NOT NULL
);

CREATE TABLE book (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(200)   NOT NULL,
    isbn           VARCHAR(20)    NOT NULL UNIQUE,
    published_year INTEGER        NOT NULL,
    price          NUMERIC(10, 2) NOT NULL,
    format         VARCHAR(20)    NOT NULL,
    publisher_id   BIGINT         NOT NULL REFERENCES publisher (id)
);

CREATE TABLE book_author (
    book_id   BIGINT NOT NULL REFERENCES book (id),
    author_id BIGINT NOT NULL REFERENCES author (id),
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE review (
    id            BIGSERIAL PRIMARY KEY,
    book_id       BIGINT       NOT NULL REFERENCES book (id),
    reviewer_name VARCHAR(120) NOT NULL,
    rating        INTEGER      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body          TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE book_cover (
    book_id        BIGINT PRIMARY KEY REFERENCES book (id),
    dominant_color VARCHAR(20) NOT NULL,
    width          INTEGER     NOT NULL,
    height         INTEGER     NOT NULL
);

-- The indexes a real schema would have. Measuring an N+1 on an unindexed
-- foreign key would inflate every number here for the wrong reason: the cost
-- being demonstrated is the round trip, not a sequential scan.
CREATE INDEX idx_book_publisher ON book (publisher_id);
CREATE INDEX idx_review_book ON review (book_id);
CREATE INDEX idx_book_author_author ON book_author (author_id);
