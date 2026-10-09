package helpdesk.service;

public class EmailNotificationService implements NotificationService {
    private final String emailTo;

    public EmailNotificationService(String emailTo) {
        this.emailTo = emailTo;
    }

    @Override
    public void send(String message) {
        System.out.println("[Отправлено на " + emailTo + "] " + message);
    }
}