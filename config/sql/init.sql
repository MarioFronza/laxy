-- Insert into languages
INSERT INTO languages (name, code) VALUES
    ('English', 'en'),
    ('Spanish', 'es'),
    ('French', 'fr'),
    ('German', 'de'),
    ('Portuguese', 'pt');

-- Insert into subjects
INSERT INTO subjects (language_id, name, description) VALUES
    ((SELECT id FROM languages WHERE code = 'en'), 'Present Perfect Continuous', 'Grammar topic covering the usage of the present perfect continuous tense in English.'),
    ((SELECT id FROM languages WHERE code = 'en'), 'Phrasal Verbs', 'Common English phrasal verbs and their meanings.'),
    ((SELECT id FROM languages WHERE code = 'es'), 'Subjuntivo', 'Uso del modo subjuntivo en español.'),
    ((SELECT id FROM languages WHERE code = 'es'), 'Expresiones Idiomáticas', 'Frases comunes y modismos en español.'),
    ((SELECT id FROM languages WHERE code = 'fr'), 'Passé Composé', 'Utilisation du passé composé en français.'),
    ((SELECT id FROM languages WHERE code = 'fr'), 'Expressions Courantes', 'Expressions idiomatiques et leur usage en français.'),
    ((SELECT id FROM languages WHERE code = 'de'), 'Perfekt', 'Bildung und Verwendung des Perfekts im Deutschen.'),
    ((SELECT id FROM languages WHERE code = 'de'), 'Redewendungen', 'Häufig verwendete deutsche Redewendungen und ihre Bedeutung.'),
    ((SELECT id FROM languages WHERE code = 'pt'), 'Verbos no Pretérito Perfeito', 'Uso do pretérito perfeito no português brasileiro.'),
    ((SELECT id FROM languages WHERE code = 'pt'), 'Expressões Populares', 'Expressões idiomáticas comuns no português brasileiro.');
