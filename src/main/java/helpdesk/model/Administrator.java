package helpdesk.model;

import java.time.LocalDateTime;

public class Administrator extends User {
    public Administrator(long id, String name, String email) {
        super(id, name, email);
    }

    public Administrator(long id, String name, String email,
                         Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public void performAction() {
        System.out.println("Старший оператор " + getName() + " управляет правилами поддержки и возвратами.");
    }
}
