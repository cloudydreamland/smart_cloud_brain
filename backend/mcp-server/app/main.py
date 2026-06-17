import os
from typing import Any

import httpx
from fastmcp import FastMCP


mcp = FastMCP("smart-cloud-brain-mcp")


def backend_base_url() -> str:
    return os.getenv("BACKEND_API_URL", "http://localhost:18080").rstrip("/")


def backend_token() -> str:
    return os.getenv("BACKEND_API_TOKEN", "").strip()


def timeout_seconds() -> float:
    try:
        return float(os.getenv("MCP_TIMEOUT_SECONDS", "10"))
    except ValueError:
        return 10.0


def headers(require_token: bool = False) -> dict[str, str]:
    token = backend_token()
    if require_token and not token:
        raise ValueError("BACKEND_API_TOKEN is required for this tool")
    return {"Authorization": f"Bearer {token}"} if token else {}


def unwrap_result(payload: Any) -> Any:
    if isinstance(payload, dict) and {"code", "data"}.issubset(payload.keys()):
        if payload.get("code") != 0:
            raise ValueError(payload.get("message") or f"backend returned code={payload.get('code')}")
        return payload.get("data")
    return payload


def get_json(path: str, params: dict[str, Any] | None = None, require_token: bool = False) -> Any:
    try:
        with httpx.Client(base_url=backend_base_url(), timeout=timeout_seconds()) as client:
            response = client.get(path, params=params or {}, headers=headers(require_token=require_token))
            response.raise_for_status()
            return unwrap_result(response.json())
    except Exception as exc:
        return {
            "ok": False,
            "error": str(exc),
            "path": path,
            "backendApiUrl": backend_base_url(),
        }


def ok(data: Any) -> dict[str, Any]:
    return {"ok": True, "data": data}


@mcp.tool
def query_department_list() -> dict[str, Any]:
    """Query all departments from the Smart Cloud Brain backend."""
    data = get_json("/api/doctor/department/list")
    return data if isinstance(data, dict) and data.get("ok") is False else ok(data)


@mcp.tool
def query_doctor_schedule(departmentCode: str) -> dict[str, Any]:
    """Query published doctor schedules by department code."""
    departments = get_json("/api/doctor/department/list")
    if isinstance(departments, dict) and departments.get("ok") is False:
        return departments
    department = next(
        (item for item in departments if str(item.get("code", "")).lower() == departmentCode.lower()),
        None,
    )
    if not department:
        return {"ok": False, "error": f"departmentCode not found: {departmentCode}"}
    schedules = get_json("/internal/doctors/schedules/list", require_token=True)
    if isinstance(schedules, dict) and schedules.get("ok") is False:
        return schedules
    department_id = department.get("id")
    filtered = [item for item in schedules if item.get("departmentId") == department_id]
    return ok(filtered)


@mcp.tool
def query_patient_allergy(patientId: int) -> dict[str, Any]:
    """Query patient allergy and past history by patient id."""
    data = get_json(f"/internal/patients/{patientId}/summary", require_token=True)
    if isinstance(data, dict) and data.get("ok") is False:
        return data
    return ok({
        "patientId": data.get("id"),
        "name": data.get("name"),
        "allergyHistory": data.get("allergyHistory", ""),
        "pastHistory": data.get("pastHistory", ""),
    })


@mcp.tool
def query_drug_info(drugName: str) -> dict[str, Any]:
    """Query drug contraindication and interaction information by drug name."""
    data = get_json("/api/search/drugs", params={"q": drugName}, require_token=True)
    return data if isinstance(data, dict) and data.get("ok") is False else ok(data)


@mcp.tool
def query_patient_medical_history(patientId: int) -> dict[str, Any]:
    """Query medical records by patient id."""
    data = get_json(f"/internal/medical-records/patient/{patientId}", require_token=True)
    return data if isinstance(data, dict) and data.get("ok") is False else ok(data)


def run() -> None:
    host = os.getenv("MCP_HOST", "0.0.0.0")
    port = int(os.getenv("MCP_PORT", "8090"))
    try:
        mcp.run(transport="http", host=host, port=port)
    except TypeError:
        mcp.run(transport="streamable-http", host=host, port=port)


if __name__ == "__main__":
    run()
