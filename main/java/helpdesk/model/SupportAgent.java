package helpdesk.model;

import java.util.ArrayList;
import java.util.List;

public class SupportAgent extends User {
    private final List<Ticket> assignedTickets = new ArrayList<>();

    public SupportAgent(long id, String name, String email) {
        super(id, name, email);
    }

    public void assignTicket(Ticket ticket) {
        assignedTickets.add(ticket);
    }

    public List<Ticket> getAssignedTickets() {
        return new ArrayList<>(assignedTickets);
    }

    @Override
    public void performAction() {
        System.out.println("Оператор " + getName() + " обрабатывает обращения покупателей.");
    }
}