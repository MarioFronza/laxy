-- Languages
INSERT INTO languages (name, code) VALUES ('English', 'en')
ON CONFLICT (code) DO NOTHING;

-- Subjects (English)
INSERT INTO subjects (language_id, name, description) VALUES
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Present Simple',
        'The present simple tense is used to describe habits, routines, general truths, and fixed arrangements.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Present Continuous',
        'The present continuous tense is used to describe actions happening right now or around the current moment.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Present Perfect',
        'The present perfect tense connects past actions or experiences to the present moment.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Present Perfect Continuous',
        'The present perfect continuous describes actions that started in the past and are still continuing or have recently stopped.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Past Simple',
        'The past simple tense is used to describe completed actions or events that happened at a specific time in the past.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Past Continuous',
        'The past continuous tense describes actions that were in progress at a specific moment in the past.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Past Perfect',
        'The past perfect tense describes an action that was completed before another action or point in the past.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Future Simple',
        'The future simple tense is used to make predictions, express spontaneous decisions, or talk about future facts.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Future Continuous',
        'The future continuous tense describes actions that will be in progress at a specific moment in the future.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Conditional',
        'Conditional sentences express hypothetical situations and their consequences, covering real and unreal conditions.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Passive Voice',
        'The passive voice shifts the focus from the subject performing the action to the object receiving it.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Modal Verbs',
        'Modal verbs such as can, could, may, might, must, shall, should, will, and would express ability, possibility, permission, and obligation.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Phrasal Verbs',
        'Phrasal verbs are combinations of a verb and one or more particles that together create a new meaning.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Reported Speech',
        'Reported speech is used to relay what someone said without quoting them directly, often requiring tense shifts.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Articles',
        'Articles (a, an, the) are used to define whether a noun refers to something specific or non-specific.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Prepositions',
        'Prepositions indicate relationships between nouns, pronouns, and other elements in a sentence, such as time, place, and direction.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Relative Clauses',
        'Relative clauses provide additional information about a noun using relative pronouns such as who, which, and that.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Countable and Uncountable Nouns',
        'Understanding the difference between countable and uncountable nouns is essential for correct use of articles and quantifiers.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Comparatives and Superlatives',
        'Comparatives and superlatives are used to compare two or more things in terms of quality, quantity, or degree.'
    ),
    (
        (SELECT id FROM languages WHERE code = 'en'),
        'Vocabulary: Business English',
        'Essential vocabulary and expressions used in professional and corporate English communication.'
    );
