-- Behavioral Survey Database Schema
-- Version: 1.0.0
-- Description: Tables for user onboarding behavioral survey feature

-- ================================================
-- 1. Survey Definition (versioned survey templates)
-- ================================================
CREATE TABLE IF NOT EXISTS survey_definition (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    estimated_minutes INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_survey_definition_active ON survey_definition(is_active);

-- ================================================
-- 2. Survey Question
-- ================================================
CREATE TABLE IF NOT EXISTS survey_question (
    id BIGSERIAL PRIMARY KEY,
    survey_definition_id BIGINT NOT NULL REFERENCES survey_definition(id) ON DELETE CASCADE,
    section_code VARCHAR(10) NOT NULL,
    question_code VARCHAR(10) NOT NULL UNIQUE,
    question_text TEXT NOT NULL,
    question_order INTEGER NOT NULL,
    weight DECIMAL(3,2) DEFAULT 1.00,
    is_required BOOLEAN DEFAULT TRUE,
    answer_enum_class VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_survey_question_definition ON survey_question(survey_definition_id);
CREATE INDEX idx_survey_question_section ON survey_question(section_code);

-- ================================================
-- 3. Survey Answer Option
-- ================================================
CREATE TABLE IF NOT EXISTS survey_answer_option (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES survey_question(id) ON DELETE CASCADE,
    answer_code VARCHAR(50) NOT NULL,
    answer_text VARCHAR(500) NOT NULL,
    score_value INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(question_id, answer_code)
);

CREATE INDEX idx_survey_answer_question ON survey_answer_option(question_id);

-- ================================================
-- 4. User Survey Response (immutable, versioned)
-- ================================================
CREATE TABLE IF NOT EXISTS user_survey_response (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    survey_definition_id BIGINT NOT NULL REFERENCES survey_definition(id),
    response_version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_timestamp TIMESTAMP,
    answers JSONB DEFAULT '{}'::JSONB,
    is_latest BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, survey_definition_id, response_version)
);

CREATE INDEX idx_user_survey_response_user ON user_survey_response(user_id);
CREATE INDEX idx_user_survey_response_user_latest ON user_survey_response(user_id, is_latest) WHERE is_latest = TRUE;
CREATE INDEX idx_user_survey_response_status ON user_survey_response(status);
CREATE INDEX idx_user_survey_response_expires ON user_survey_response(expires_at) WHERE status = 'IN_PROGRESS';

-- ================================================
-- 5. User Behavior Profile
-- ================================================
CREATE TABLE IF NOT EXISTS user_behavior_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    survey_response_id BIGINT REFERENCES user_survey_response(id),
    profile_version INTEGER NOT NULL DEFAULT 1,
    
    -- Computed Scores (0-100)
    overspending_score DECIMAL(5,2),
    debt_risk_score DECIMAL(5,2),
    savings_capacity_score DECIMAL(5,2),
    financial_anxiety_index DECIMAL(5,2),
    
    -- Segment Classification
    segment VARCHAR(50) NOT NULL,
    segment_confidence DECIMAL(3,2),
    
    -- Explainability
    explanation_factors JSONB,
    first_action_plan JSONB,
    
    -- Feature Vector for AI
    feature_vector JSONB,
    
    -- Timestamps
    survey_completed_at TIMESTAMP,
    synced_to_ai_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_behavior_profile_user ON user_behavior_profile(user_id);
CREATE INDEX idx_user_behavior_profile_segment ON user_behavior_profile(segment);
CREATE INDEX idx_user_behavior_profile_risk ON user_behavior_profile(overspending_score, debt_risk_score);

-- ================================================
-- Comments for documentation
-- ================================================
COMMENT ON TABLE survey_definition IS 'Versioned survey templates - allows A/ B testing and gradual rollout';
COMMENT ON TABLE survey_question IS 'Individual questions within a survey, ordered within sections';
COMMENT ON TABLE survey_answer_option IS 'Predefined answer choices with score values';
COMMENT ON TABLE user_survey_response IS 'Immutable record of user survey answers - versioned for audit trail';
COMMENT ON TABLE user_behavior_profile IS 'Computed behavioral profile - scores, segment, and AI features';

COMMENT ON COLUMN user_survey_response.answers IS 'JSONB map of question_code -> answer_code';
COMMENT ON COLUMN user_survey_response.is_latest IS 'Only one response per user should be latest=true';
COMMENT ON COLUMN user_behavior_profile.feature_vector IS 'Normalized features (0-1) for AI cold-start';
COMMENT ON COLUMN user_behavior_profile.explanation_factors IS 'Top contributing factors for transparency';
