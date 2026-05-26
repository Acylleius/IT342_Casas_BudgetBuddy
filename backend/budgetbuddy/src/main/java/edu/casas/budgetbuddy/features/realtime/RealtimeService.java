package edu.casas.budgetbuddy.features.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeService {
    private final List<ClientEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        ClientEmitter client = new ClientEmitter(userId, emitter);
        emitters.add(client);
        emitter.onCompletion(() -> emitters.remove(client));
        emitter.onTimeout(() -> emitters.remove(client));
        emitter.onError(error -> emitters.remove(client));
        send(emitter, "connected", "BudgetBuddy realtime connected");
        return emitter;
    }

    public void publish(String eventName, Object data) {
        for (ClientEmitter client : emitters) {
            send(client.emitter(), eventName, data);
        }
    }

    public void publishToUser(Long userId, String eventName, Object data) {
        for (ClientEmitter client : emitters) {
            if (client.userId().equals(userId)) {
                send(client.emitter(), eventName, data);
            }
        }
    }

    public void publishToUsers(Collection<Long> userIds, String eventName, Object data) {
        for (ClientEmitter client : emitters) {
            if (userIds.contains(client.userId())) {
                send(client.emitter(), eventName, data);
            }
        }
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException ex) {
            emitters.removeIf(client -> client.emitter().equals(emitter));
        }
    }

    private record ClientEmitter(Long userId, SseEmitter emitter) {
    }
}
