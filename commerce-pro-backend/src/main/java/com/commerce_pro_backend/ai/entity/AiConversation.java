package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.SessionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AiConversation — stores multi-turn AI conversation history.
 *
 * The full turn history is serialised as JSON in the {@code turns_json} column.
 * Use {@link #getTurnsAsList()} / {@link #addTurn(String, String)} for access —
 * never manipulate the raw JSON directly.
 *
 * Used by:
 *   - Support Chatbot  (SUPPORT_CHAT)
 *   - Natural Language Reports (NL_REPORT)
 */
@Entity
@Table(name = "ai_conversations", indexes = {
    @Index(name = "idx_ai_conv_session_type", columnList = "session_type"),
    @Index(name = "idx_ai_conv_customer",     columnList = "customer_id"),
    @Index(name = "idx_ai_conv_user",         columnList = "user_id"),
    @Index(name = "idx_ai_conv_expires",      columnList = "expires_at"),
    @Index(name = "idx_ai_conv_last_active",  columnList = "last_active_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    /** SUPPORT_CHAT | NL_REPORT */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    /** Customer who owns this chatbot session (SUPPORT_CHAT only) */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    /** Admin user who owns this report session (NL_REPORT only) */
    @Column(name = "user_id", length = 36)
    private String userId;

    // ── Conversation state ────────────────────────────────────────────────────
    /**
     * Full turn history as a JSON array.
     * Each element: {"userMessage":"...","assistantMessage":"...","timestamp":"..."}
     */
    @Column(name = "turns_json", columnDefinition = "TEXT")
    @Builder.Default
    private String turnsJson = "[]";

    @Column(name = "turn_count", nullable = false)
    @Builder.Default
    private Integer turnCount = 0;

    // ── Status ────────────────────────────────────────────────────────────────
    /** ACTIVE | ESCALATED | RESOLVED | EXPIRED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── Metadata ──────────────────────────────────────────────────────────────
    @Column(name = "total_tokens_used")
    @Builder.Default
    private Integer totalTokensUsed = 0;

    /** For NL Reports — the SavedReport ID if this session was saved */
    @Column(name = "linked_report_id", length = 36)
    private String linkedReportId;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Business methods ──────────────────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public List<ConversationTurn> getTurnsAsList() {
        try {
            return MAPPER.readValue(turnsJson, new TypeReference<List<ConversationTurn>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addTurn(String userMessage, String assistantMessage) {
        List<ConversationTurn> turns = getTurnsAsList();
        turns.add(new ConversationTurn(userMessage, assistantMessage, LocalDateTime.now().toString()));
        try {
            this.turnsJson = MAPPER.writeValueAsString(turns);
            this.turnCount = turns.size();
            this.lastActiveAt = LocalDateTime.now();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise conversation turn", e);
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public record ConversationTurn(String userMessage, String assistantMessage, String timestamp) {}
}
