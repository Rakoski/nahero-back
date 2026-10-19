ALTER TABLE questions
    ADD CONSTRAINT fk_questions_practice_exam_domain
        FOREIGN KEY (practice_exam_id, domain_id)
            REFERENCES practice_exam_domains (practice_exam_id, domain_id);
