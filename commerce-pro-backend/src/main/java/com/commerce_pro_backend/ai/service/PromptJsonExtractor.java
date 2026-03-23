package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.exception.AiResponseParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PromptJsonExtractor — safely extracts and deserialises a JSON object from a
 * raw model response string.
 *
 * <p>Large language models sometimes return JSON wrapped in markdown code fences
 * or surrounded by filler text, even when the system prompt says "return ONLY JSON".
 * This utility handles all common variations so feature services can call one method
 * and get a typed result.
 *
 * <h3>Supported raw response forms</h3>
 * <pre>
 *   // (1) Clean JSON — ideal case
 *   {"riskScore": 80, "riskLevel": "HIGH"}
 *
 *   // (2) Wrapped in ```json ... ``` fence — most common deviation
 *   ```json
 *   {"riskScore": 80, "riskLevel": "HIGH"}
 *   ```
 *
 *   // (3) Plain ``` fence
 *   ```
 *   {"riskScore": 80, "riskLevel": "HIGH"}
 *   ```
 *
 *   // (4) JSON preceded by a preamble sentence
 *   Here is the analysis:
 *   {"riskScore": 80, "riskLevel": "HIGH"}
 * </pre>
 *
 * <h3>Usage example</h3>
 * <pre>
 *   String raw = orchestrator.complete(FRAUD_DETECTION, systemPrompt, userPayload);
 *   FraudResult result = PromptJsonExtractor.parse(raw, FraudResult.class);
 *   // result.riskScore() == 80, result.riskLevel() == "HIGH"
 * </pre>
 */
public final class PromptJsonExtractor {

    /**
     * Shared, thread-safe ObjectMapper configured to ignore unknown JSON properties.
     *
     * <p>FAIL_ON_UNKNOWN_PROPERTIES is disabled because future model versions may
     * return extra fields that do not yet exist in our record classes — we want those
     * calls to succeed rather than throw.
     *
     * Example: if the model adds a "confidence" field to the fraud JSON but our
     * FraudResult record doesn't have it yet, deserialization still works.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Prevent instantiation — this is a pure utility class.
    private PromptJsonExtractor() {}

    /**
     * Extract the JSON object from {@code raw} and deserialise it into {@code type}.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Strip markdown code fences (```json...``` or ```...```)
     *   <li>Find the first {@code {} and last {@code }} in the remaining text
     *   <li>Parse the substring as JSON
     *   <li>Deserialise into the requested type
     * </ol>
     *
     * @param raw     the raw string returned by the AI model
     * @param type    the Java class to deserialise into (must be a Jackson-compatible POJO or record)
     * @param feature label used in the error message (e.g. "FRAUD_DETECTION")
     * @param <T>     the result type
     * @return populated instance of {@code T}
     * @throws AiResponseParseException if no valid JSON block is found or deserialization fails
     *
     * <p>Example:
     * <pre>
     *   // Raw model output (with accidental preamble):
     *   String raw = "Sure! Here is the result:\n{\"score\": 72, \"level\": \"HIGH\"}";
     *
     *   // Parsed safely:
     *   MyRecord result = PromptJsonExtractor.parse(raw, MyRecord.class, "MY_FEATURE");
     *   // result.score() == 72
     * </pre>
     */
    public static <T> T parse(String raw, Class<T> type, String feature) {
        String cleaned = stripMarkdownFences(raw.trim());

        // Locate the JSON object boundaries: first '{' and last '}'
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');

        if (start == -1 || end == -1 || end < start) {
            throw new AiResponseParseException(
                    feature + ": model response contains no JSON object. Raw: " +
                    truncate(raw, 200));
        }

        String json = cleaned.substring(start, end + 1);

        try {
            return MAPPER.readValue(json, type);
        } catch (Exception ex) {
            throw new AiResponseParseException(
                    feature + ": JSON deserialization failed for type " + type.getSimpleName()
                    + ". Extracted JSON: " + truncate(json, 200), ex);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Remove ```json ... ``` and ``` ... ``` markdown fences from the model output.
     *
     * <p>Example input:  "```json\n{\"score\": 72}\n```"
     * <p>Example output: "{\"score\": 72}"
     */
    private static String stripMarkdownFences(String text) {
        // Remove opening ```json or ``` fences and their closing counterpart
        return text
                .replaceAll("(?s)```json\\s*", "") // strip ```json with optional whitespace
                .replaceAll("(?s)```\\s*",     "") // strip plain ```
                .trim();
    }

    /** Truncate a string to at most {@code maxLen} characters for safe log/error output. */
    private static String truncate(String s, int maxLen) {
        if (s == null)           return "(null)";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…";
    }
}
