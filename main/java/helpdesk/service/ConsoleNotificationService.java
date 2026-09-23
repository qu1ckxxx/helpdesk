package helpdesk.service;

public class ConsoleNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("[КОНСОЛЬ УВЕДОМЛЕНИЕ] " + message);
    }
}