ALTER TABLE registration ADD COLUMN IF NOT EXISTS owner_patient_id BIGINT;
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_type VARCHAR(20);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_id BIGINT;
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_name VARCHAR(80);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_relationship VARCHAR(40);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_gender VARCHAR(20);
ALTER TABLE registration ADD COLUMN IF NOT EXISTS subject_age INT;

UPDATE registration r
SET owner_patient_id = COALESCE(r.owner_patient_id, r.patient_id),
    subject_type = COALESCE(NULLIF(r.subject_type, ''), CASE WHEN UPPER(COALESCE(r.visitor_type, '')) = 'VISITOR' AND r.visitor_id IS NOT NULL THEN 'VISITOR' ELSE 'ACCOUNT' END),
    subject_id = COALESCE(r.subject_id, CASE WHEN UPPER(COALESCE(r.visitor_type, '')) = 'VISITOR' AND r.visitor_id IS NOT NULL THEN r.visitor_id ELSE r.patient_id END),
    subject_name = COALESCE(NULLIF(r.subject_name, ''), NULLIF(r.visitor_name, ''), p.name),
    subject_relationship = COALESCE(NULLIF(r.subject_relationship, ''), NULLIF(r.visitor_relationship, ''), CASE WHEN UPPER(COALESCE(r.visitor_type, '')) = 'VISITOR' THEN '家属' ELSE '本人' END),
    subject_gender = COALESCE(NULLIF(r.subject_gender, ''), NULLIF(r.visitor_gender, ''), p.gender),
    subject_age = COALESCE(r.subject_age, r.visitor_age, p.age)
FROM patient p
WHERE p.id = r.patient_id;

ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS owner_patient_id BIGINT;
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_type VARCHAR(20);
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_id BIGINT;
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_name VARCHAR(80);
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_relationship VARCHAR(40);
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_gender VARCHAR(20);
ALTER TABLE triage_record ADD COLUMN IF NOT EXISTS subject_age INT;

UPDATE triage_record t
SET owner_patient_id = COALESCE(t.owner_patient_id, t.patient_id),
    subject_type = COALESCE(NULLIF(t.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(t.subject_id, t.patient_id),
    subject_name = COALESCE(NULLIF(t.subject_name, ''), p.name),
    subject_relationship = COALESCE(NULLIF(t.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(t.subject_gender, ''), p.gender),
    subject_age = COALESCE(t.subject_age, p.age)
FROM patient p
WHERE p.id = t.patient_id;

ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS owner_patient_id BIGINT;
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_type VARCHAR(20);
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_id BIGINT;
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_name VARCHAR(80);
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_relationship VARCHAR(40);
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_gender VARCHAR(20);
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS subject_age INT;

UPDATE medical_record m
SET owner_patient_id = COALESCE(m.owner_patient_id, r.owner_patient_id, r.patient_id, m.patient_id),
    subject_type = COALESCE(NULLIF(m.subject_type, ''), NULLIF(r.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(m.subject_id, r.subject_id, r.patient_id, m.patient_id),
    subject_name = COALESCE(NULLIF(m.subject_name, ''), NULLIF(r.subject_name, '')),
    subject_relationship = COALESCE(NULLIF(m.subject_relationship, ''), NULLIF(r.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(m.subject_gender, ''), NULLIF(r.subject_gender, '')),
    subject_age = COALESCE(m.subject_age, r.subject_age)
FROM registration r
WHERE r.id = m.registration_id;

UPDATE medical_record m
SET owner_patient_id = COALESCE(m.owner_patient_id, m.patient_id),
    subject_type = COALESCE(NULLIF(m.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(m.subject_id, m.patient_id),
    subject_name = COALESCE(NULLIF(m.subject_name, ''), p.name),
    subject_relationship = COALESCE(NULLIF(m.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(m.subject_gender, ''), p.gender),
    subject_age = COALESCE(m.subject_age, p.age)
FROM patient p
WHERE p.id = m.patient_id;

ALTER TABLE prescription ADD COLUMN IF NOT EXISTS owner_patient_id BIGINT;
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_type VARCHAR(20);
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_id BIGINT;
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_name VARCHAR(80);
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_relationship VARCHAR(40);
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_gender VARCHAR(20);
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS subject_age INT;

UPDATE prescription p
SET owner_patient_id = COALESCE(p.owner_patient_id, m.owner_patient_id, m.patient_id, p.patient_id),
    subject_type = COALESCE(NULLIF(p.subject_type, ''), NULLIF(m.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(p.subject_id, m.subject_id, m.patient_id, p.patient_id),
    subject_name = COALESCE(NULLIF(p.subject_name, ''), NULLIF(m.subject_name, '')),
    subject_relationship = COALESCE(NULLIF(p.subject_relationship, ''), NULLIF(m.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(p.subject_gender, ''), NULLIF(m.subject_gender, '')),
    subject_age = COALESCE(p.subject_age, m.subject_age)
FROM medical_record m
WHERE m.id = p.medical_record_id;

UPDATE prescription p
SET owner_patient_id = COALESCE(p.owner_patient_id, r.owner_patient_id, r.patient_id, p.patient_id),
    subject_type = COALESCE(NULLIF(p.subject_type, ''), NULLIF(r.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(p.subject_id, r.subject_id, r.patient_id, p.patient_id),
    subject_name = COALESCE(NULLIF(p.subject_name, ''), NULLIF(r.subject_name, '')),
    subject_relationship = COALESCE(NULLIF(p.subject_relationship, ''), NULLIF(r.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(p.subject_gender, ''), NULLIF(r.subject_gender, '')),
    subject_age = COALESCE(p.subject_age, r.subject_age)
FROM registration r
WHERE r.id = p.registration_id;

UPDATE prescription pr
SET owner_patient_id = COALESCE(pr.owner_patient_id, pr.patient_id),
    subject_type = COALESCE(NULLIF(pr.subject_type, ''), 'ACCOUNT'),
    subject_id = COALESCE(pr.subject_id, pr.patient_id),
    subject_name = COALESCE(NULLIF(pr.subject_name, ''), pa.name),
    subject_relationship = COALESCE(NULLIF(pr.subject_relationship, ''), '本人'),
    subject_gender = COALESCE(NULLIF(pr.subject_gender, ''), pa.gender),
    subject_age = COALESCE(pr.subject_age, pa.age)
FROM patient pa
WHERE pa.id = pr.patient_id;

CREATE INDEX IF NOT EXISTS idx_registration_owner_subject_slot ON registration(owner_patient_id, subject_type, subject_id, slot_id, status);
CREATE INDEX IF NOT EXISTS idx_triage_owner_subject ON triage_record(owner_patient_id, subject_type, subject_id, created_at);
CREATE INDEX IF NOT EXISTS idx_medical_record_owner_subject ON medical_record(owner_patient_id, subject_type, subject_id, created_at);
CREATE INDEX IF NOT EXISTS idx_prescription_owner_subject ON prescription(owner_patient_id, subject_type, subject_id, created_at);
