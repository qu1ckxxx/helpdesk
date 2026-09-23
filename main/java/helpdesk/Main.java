package helpdesk;

import helpdesk.model.*;
import helpdesk.repository.TicketRepository;
import helpdesk.service.*;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println(" HELP DESK: ИНТЕРНЕТ-МАГАЗИН\n");

        Customer customer = new Customer(1, "Анна Петрова", "anna@mail.ru");
        SupportAgent agent = new SupportAgent(2, "Сергей Иванов", "sergey@helpdesk.ru");
        Administrator admin = new Administrator(3, "Олег Сидоров", "admin@helpdesk.ru");

        System.out.println("--- Роли пользователей ---");
        List<User> users = new ArrayList<>();
        users.add(customer);
        users.add(agent);
        users.add(admin);

        for (User user : users) {
            user.performAction();
        }
        System.out.println();

        Ticket ticket1 = new Ticket(
                1,
                "Задержка доставки заказа #4829",
                "Курьер должен был приехать вчера, статус не меняется",
                TicketPriority.HIGH
        );

        System.out.println("Создана новая заявка:");
        System.out.println(ticket1);
        System.out.println();

        NotificationService notificationService = new EmailNotificationService(customer.getEmail());
        TicketService ticketService = new TicketService(notificationService);

        System.out.println("--- Жизненный цикл заявки ---");
        System.out.println("Начальный статус: " + ticket1.getStatus());

        ticketService.startTicket(ticket1);
        System.out.println("Статус: " + ticket1.getStatus());

        ticketService.resolveTicket(ticket1);
        System.out.println("Статус: " + ticket1.getStatus());

        ticketService.closeTicket(ticket1);
        System.out.println("Статус: " + ticket1.getStatus());
        System.out.println();

        TicketRepository repository = new TicketRepository();
        repository.add(ticket1);

        Ticket ticket2 = new Ticket(
                2,
                "Запрос на возврат товара",
                "Размер одежды не подошел",
                TicketPriority.MEDIUM
        );
        repository.add(ticket2);

        System.out.println("--- Список всех заявок в репозитории ---");
        for (Ticket ticket : repository.findAll()) {
            System.out.println(ticket);
        }
    }
}