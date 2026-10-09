package helpdesk.model;

import java.time.LocalDateTime;

public class SupportAgent extends User {
    public SupportAgent(long id, String name, String email) {
        super(id, name, email);
    }

    public SupportAgent(long id, String name, String email,
                        Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.SUPPORT_AGENT;
    }

    @Override
    public void performAction() {
        System.out.println("Оператор " + getName() + " обрабатывает обращения покупателей.");
    }
}
