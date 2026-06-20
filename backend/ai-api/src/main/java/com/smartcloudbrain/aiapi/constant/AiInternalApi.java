package com.smartcloudbrain.aiapi.constant;

public final class AiInternalApi {

  public static final String BASE = "/internal/ai";
  public static final String TRIAGE = BASE + "/triage";
  public static final String MEDICAL_RECORD_GENERATE = BASE + "/medical-record/generate";
  public static final String MEDICAL_RECORD_STREAM = BASE + "/medical-record/generate/stream";
  public static final String PRESCRIPTION_CHECK = BASE + "/prescription/check";
  public static final String SCHEDULE_SUGGEST = BASE + "/schedule/suggest";
  public static final String PROMPT_RESOLVE = BASE + "/prompt-template/resolve";
  public static final String PROMPT_TEST = BASE + "/prompt-template/test";
  public static final String HEALTH = BASE + "/health";

  private AiInternalApi() {
  }
}
