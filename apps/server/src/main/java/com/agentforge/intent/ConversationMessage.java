package com.agentforge.intent;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_messages")
@Data
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    private String role; // user, assistant

    private String content;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
