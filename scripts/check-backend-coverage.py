#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPORT = Path("backend/coverage-report/target/site/jacoco-aggregate/jacoco.xml")
CORE_CLASSES = {
    "com/smartcloudbrain/auth/service/AuthService",
    "com/smartcloudbrain/registration/service/RegistrationService",
    "com/smartcloudbrain/triage/service/TriageService",
    "com/smartcloudbrain/medicalrecord/service/MedicalRecordService",
    "com/smartcloudbrain/prescription/service/PrescriptionService",
    "com/smartcloudbrain/notification/service/NotificationService",
    "com/smartcloudbrain/ai/application/AiOrchestrationService",
    "com/smartcloudbrain/ai/provider/dify/DifyAiProvider",
    "com/smartcloudbrain/admin/service/AdminCatalogService",
    "com/smartcloudbrain/doctor/service/DoctorScheduleService",
}


def line_counter(node):
    for counter in node.findall("counter"):
        if counter.get("type") == "LINE":
            return int(counter.get("covered", "0")), int(counter.get("missed", "0"))
    return 0, 0


if not REPORT.exists():
    print(f"Coverage report not found: {REPORT}", file=sys.stderr)
    sys.exit(2)

root = ET.parse(REPORT).getroot()
business_covered = 0
business_missed = 0
core_results = {}

for clazz in root.findall(".//class"):
    name = clazz.get("name", "")
    covered, missed = line_counter(clazz)
    if "/service/" in name or "/application/" in name or "/provider/" in name:
        business_covered += covered
        business_missed += missed
    if name in CORE_CLASSES:
        core_results[name] = (covered, missed)

total = business_covered + business_missed
ratio = business_covered / total if total else 0.0
failed = ratio < 0.80
print(f"Business line coverage: {ratio:.2%} ({business_covered}/{total}), required >= 80.00%")

for name in sorted(CORE_CLASSES):
    covered, missed = core_results.get(name, (0, 1))
    class_total = covered + missed
    class_ratio = covered / class_total if class_total else 0.0
    print(f"Core {name}: {class_ratio:.2%} ({covered}/{class_total}), required 100.00%")
    failed = failed or missed != 0 or name not in core_results

sys.exit(1 if failed else 0)
