CREATE TABLE languages (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    salt BYTEA NOT NULL,
    hashed_password BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);

CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    language_id BIGSERIAL NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subject_language_id ON subjects(language_id);

CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGSERIAL NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    total_questions INT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('creating','pending','completed')) DEFAULT 'creating',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quiz_user_subject_id ON quizzes(user_id, subject_id);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGSERIAL NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_question_quiz_id ON questions(quiz_id);

CREATE TABLE question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGSERIAL NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    reference_number INT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_question_option_question_id ON question_options(question_id);

CREATE TABLE question_attempts (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGSERIAL NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    user_selected_option BIGSERIAL NOT NULL REFERENCES question_options(id) ON DELETE CASCADE,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_question_attempt_question_id ON question_attempts(question_id);

CREATE TABLE user_themes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    current BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_theme_user_id ON user_themes(user_id);

CREATE TABLE user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGSERIAL NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    quizzes_completed INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);
