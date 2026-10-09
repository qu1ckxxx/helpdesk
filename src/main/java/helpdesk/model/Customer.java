package helpdesk.model;

import java.time.LocalDateTime;

public class Customer extends User {
    public Customer(long id, String name, String email) {
        super(id, name, email);
    }

    public Customer(long id, String name, String email,
                    Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.CUSTOMER;
    }

    @Override
    public void performAction() {
        System.out.println("Покупатель " + getName() + " создает заявку по заказу/доставке.");
    }
}
