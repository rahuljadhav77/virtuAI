package com.virtualization.service;

import com.virtualization.engine.MqRuleEngine;
import com.virtualization.model.MqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class MqVirtualizationService {
    private static final Logger log = LoggerFactory.getLogger(MqVirtualizationService.class);
    private final MqRuleEngine ruleEngine;
    private final Map<String, BlockingQueue<MqMessage>> queues = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public MqVirtualizationService(MqRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public void sendMessage(String queueName, MqMessage message) {
        log.info("Message received on virtual queue {}: id={}, correlationId={}", queueName, message.getMessageId(), message.getCorrelationId());
        
        // Simulate async processing like a real MQ listener
        executor.submit(() -> {
            try {
                Optional<MqMessage> response = ruleEngine.process(queueName, message);
                
                if (response.isPresent()) {
                    MqMessage respMsg = response.get();
                    String replyTo = message.getReplyTo();
                    
                    if (replyTo != null) {
                        log.info("Rule matched. Routing response to {} with correlationId {}", replyTo, respMsg.getCorrelationId());
                        deliverMessage(replyTo, respMsg);
                    } else {
                        log.warn("Rule matched but no replyTo queue specified for message {}", message.getMessageId());
                        // In some systems, we might have a default output queue in the rule
                    }
                } else {
                    log.info("No matching rule for message {} on queue {}", message.getMessageId(), queueName);
                }
            } catch (Exception e) {
                log.error("Error in virtual MQ processor: {}", e.getMessage(), e);
            }
        });
    }

    private void deliverMessage(String queueName, MqMessage message) {
        BlockingQueue<MqMessage> queue = queues.computeIfAbsent(queueName, k -> new LinkedBlockingQueue<>());
        queue.offer(message);
    }

    public MqMessage receiveMessage(String queueName, long timeoutMs) throws InterruptedException {
        BlockingQueue<MqMessage> queue = queues.computeIfAbsent(queueName, k -> new LinkedBlockingQueue<>());
        return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
