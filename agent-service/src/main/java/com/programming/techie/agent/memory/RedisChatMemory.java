package com.programming.techie.agent.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisChatMemory implements ChatMemory {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "chat:memory:";
    private static final long TTL_MINUTES = 30; // Blueprint requirement

    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static class MessageDto {
        private String messageType;
        private String content;

        public MessageDto() {}
        public MessageDto(String messageType, String content) {
            this.messageType = messageType;
            this.content = content;
        }

        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    private MessageDto toDto(Message msg) {
        return new MessageDto(msg.getMessageType().name(), msg.getText());
    }

    private Message toMessage(MessageDto dto) {
        String type = dto.getMessageType();
        if ("USER".equalsIgnoreCase(type)) {
            return new org.springframework.ai.chat.messages.UserMessage(dto.getContent());
        } else if ("ASSISTANT".equalsIgnoreCase(type)) {
            return new org.springframework.ai.chat.messages.AssistantMessage(dto.getContent());
        } else if ("SYSTEM".equalsIgnoreCase(type)) {
            return new org.springframework.ai.chat.messages.SystemMessage(dto.getContent());
        }
        return new org.springframework.ai.chat.messages.UserMessage(dto.getContent());
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = PREFIX + conversationId;
        List<Message> existing = get(conversationId, 100);
        existing.addAll(messages);
        
        List<MessageDto> dtos = new ArrayList<>();
        for (Message msg : existing) {
            dtos.add(toDto(msg));
        }
        
        redisTemplate.opsForValue().set(key, dtos, TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> get(String conversationId, int lastN) {
        String key = PREFIX + conversationId;
        List<Object> raw = (List<Object>) redisTemplate.opsForValue().get(key);
        
        if (raw == null) {
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof MessageDto) {
                messages.add(toMessage((MessageDto) obj));
            } else if (obj instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                String type = (String) map.get("messageType");
                String content = (String) map.get("content");
                messages.add(toMessage(new MessageDto(type, content)));
            }
        }
        
        if (lastN > 0 && messages.size() > lastN) {
            return messages.subList(messages.size() - lastN, messages.size());
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        String key = PREFIX + conversationId;
        redisTemplate.delete(key);
    }
}
