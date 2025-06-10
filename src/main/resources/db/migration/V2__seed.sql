INSERT INTO languages (name, code) VALUES
    ('English', 'en');

INSERT INTO subjects (language_id, name, description) VALUES
    ((SELECT id FROM languages WHERE code = 'en'), 'Present Perfect Continuous', 'Grammar topic covering the usage of the present perfect continuous tense in English.'),
    ((SELECT id FROM languages WHERE code = 'en'), 'Phrasal Verbs', 'Common English phrasal verbs and their meanings.');
