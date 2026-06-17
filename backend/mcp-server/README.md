# Smart Cloud Brain MCP Server

Python FastMCP server for Dify or other MCP clients to query Smart Cloud Brain backend data through HTTP APIs.

## Environment

```bash
BACKEND_API_URL=http://localhost:18080
BACKEND_API_TOKEN=<admin-or-doctor-jwt>
MCP_HOST=0.0.0.0
MCP_PORT=8090
```

`BACKEND_API_TOKEN` is required for protected tools such as patient allergy, drug search, schedule, and medical history queries. Public department lookup can work without a token.

## Run

```bash
python -m venv .venv
. .venv/bin/activate
pip install -e .
python app/main.py
```

## Tools

- `query_department_list`
- `query_doctor_schedule(departmentCode)`
- `query_patient_allergy(patientId)`
- `query_drug_info(drugName)`
- `query_patient_medical_history(patientId)`
