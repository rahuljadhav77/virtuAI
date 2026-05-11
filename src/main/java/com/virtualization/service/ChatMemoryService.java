package com.virtualization.service;

import com.virtualization.entity.ChatMessageEntity;
import com.virtualization.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatMemoryService {

    private final ChatMessageRepository repository;

    public ChatMemoryService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public void addMessage(String sessionId, String role, String content) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        repository.save(message);
        log.debug("Saved {} message to session {}", role, sessionId);
    }

    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, "user", content);
    }

    public void addAssistantMessage(String sessionId, String content) {
        addMessage(sessionId, "assistant", content);
    }

    public List<String> getConversationHistory(String sessionId, int maxMessages) {
        List<ChatMessageEntity> messages = repository
                .findBySessionIdOrderByTimestampAsc(sessionId);

        // Get last N messages
        int fromIndex = Math.max(0, messages.size() - maxMessages);
        List<ChatMessageEntity> recent = messages.subList(fromIndex, messages.size());

        return recent.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.toList());
    }

    public String buildContextForAI(String sessionId, int maxMessages) {
        List<String> history = getConversationHistory(sessionId, maxMessages);
        if (history.isEmpty()) {
            return "";
        }
        return "Conversation history:\n" + String.join("\n", history) + "\n";
    }

    public void clearSession(String sessionId) {
        repository.deleteBySessionId(sessionId);
        log.info("Cleared chat history for session: {}", sessionId);
    }

    public long getSessionMessageCount(String sessionId) {
        return repository.findBySessionIdOrderByTimestampAsc(sessionId).size();
    }
}