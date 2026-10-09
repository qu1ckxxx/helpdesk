package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Category;
import helpdesk.model.Customer;
import helpdesk.model.Ticket;
import helpdesk.model.TicketComment;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketReportRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcCommentRepository;
import helpdesk.repository.jdbc.JdbcStatusHistoryRepository;
import helpdesk.repository.jdbc.JdbcTicketReportRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcUserRepository;
import helpdesk.service.CommentService;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.NotificationService;
import helpdesk.service.ReportService;
import helpdesk.service.TicketService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            run();
        } catch (RuntimeException e) {
            System.err.println("Ошибка: " + e.getMessage());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                System.err.println("  причина: " + cause.getMessage());
            }
        }
    }

    private static void run() {
        UserRepository userRepository = new JdbcUserRepository();
        CategoryRepository categoryRepository = new JdbcCategoryRepository();
        TicketRepository ticketRepository = new JdbcTicketRepository();
        StatusHistoryRepository historyRepository = new JdbcStatusHistoryRepository();
        CommentRepository commentRepository = new JdbcCommentRepository();
        TicketReportRepository reportRepository = new JdbcTicketReportRepository();
        NotificationService notification = new ConsoleNotificationService();

        TicketService ticketService = new TicketService(
                ticketRepository, historyRepository, userRepository, categoryRepository, notification);
        CommentService commentService = new CommentService(commentRepository, ticketRepository, userRepository);
        ReportService reportService = new ReportService(reportRepository);

        step(1, "Подключение к PostgreSQL");
        checkConnection();

        List<Ticket> existing = ticketService.findAll();
        System.out.println("Заявок в БД: " + existing.size());

        step(2, "Поиск клиента по email");
        User customer = userRepository.findByEmail("anna@example.org")
                .orElseThrow(() -> new IllegalStateException(
                        "Клиент anna@example.org не найден — выполните sql/02_seed.sql"));
        System.out.println("Найден: " + customer);
        System.out.println("Несуществующий email: " + userRepository.findByEmail("nobody@example.org"));

        if (userRepository.findByEmail("petr@example.org").isEmpty()) {
            User newUser = userRepository.save(new Customer(0, "Пётр Смирнов", "petr@example.org"));
            System.out.println("Создан новый клиент: " + newUser + " (id выдан PostgreSQL)");
        }

        step(3, "Список категорий");
        List<Category> categories = categoryRepository.findAll();
        categories.forEach(c -> System.out.println("  " + c));
        Category category = categories.stream().filter(Category::isActive).findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет активных категорий"));

        step(4, "Создание заявки от имени клиента");
        Ticket ticket = ticketService.createTicket(
                "Не открывается корпоративная почта",
                "После обновления клиент почты выдаёт ошибку авторизации",
                TicketPriority.HIGH, customer.getId(), category.getId());
        long ticketId = ticket.getId();
        step(5, "id, выданный PostgreSQL");
        System.out.println("id заявки = " + ticketId);

        step(6, "Чтение заявки через findById");
        System.out.println(ticketService.findById(ticketId).orElseThrow());

        step(7, "Назначение сотрудника поддержки");
        User agent = userRepository.findByEmail("ivan.support@example.org")
                .orElseThrow(() -> new IllegalStateException("Сотрудник не найден — выполните sql/02_seed.sql"));
        ticketService.assignTicket(ticketId, agent.getId(), agent.getId());
        System.out.println("Статус после назначения: " + ticketService.findById(ticketId).orElseThrow().getStatus());

        step(8, "Комментарий клиента");
        commentService.addComment(ticketId, customer.getId(), "Это мешает работе всего отдела.", false);

        step(9, "IN_PROGRESS -> RESOLVED");
        ticketService.resolveTicket(ticketId, agent.getId());

        step(10, "Внутренний комментарий сотрудника");
        commentService.addComment(ticketId, agent.getId(), "Сбросил токен в профиле, проверить через день.", true);

        step(11, "RESOLVED -> CLOSED");
        ticketService.closeTicket(ticketId, agent.getId());
        Ticket closed = ticketService.findById(ticketId).orElseThrow();
        System.out.println("Статус: " + closed.getStatus() + ", закрыта: " + closed.getClosedAt()
                + ", обновлена: " + closed.getUpdatedAt());

        step(12, "История статусов заявки #" + ticketId);
        for (TicketStatusHistory h : ticketService.getHistory(ticketId)) {
            System.out.println("  " + h);
        }

        System.out.println("\nВсе комментарии (для сотрудника):");
        commentService.getComments(ticketId).forEach(c -> System.out.println("  " + c));
        System.out.println("Только внешние (для клиента):");
        List<TicketComment> publicComments = commentService.getPublicComments(ticketId);
        publicComments.forEach(c -> System.out.println("  " + c));

        step(13, "JOIN-запрос: заявка с именами");
        reportService.getTicketDetails(ticketId).ifPresent(d -> System.out.println("  " + d));
        System.out.println("Все заявки:");
        reportService.getAllTicketDetails().forEach(d -> System.out.println("  " + d));

        step(14, "Фильтры");
        System.out.println("CLOSED + HIGH: " + ticketService.findByStatusAndPriority(
                TicketStatus.CLOSED, TicketPriority.HIGH).size() + " шт.");
        System.out.println("Незакрытых заявок у " + agent.getName() + ": "
                + ticketService.findOpenByAssigneeId(agent.getId()).size());
        System.out.println("Заявок клиента " + customer.getName() + ": "
                + ticketService.findByCustomerId(customer.getId()).size());

        step(15, "Отчёты");
        System.out.println("По статусам:");
        reportService.countByStatus().forEach(r -> System.out.println("  " + r.status() + ": " + r.count()));
        System.out.println("По категориям:");
        reportService.countByCategory().forEach(r -> System.out.println("  " + r.categoryName() + ": " + r.ticketCount()));
        System.out.println("Нагрузка сотрудников:");
        reportService.agentWorkload().forEach(r -> System.out.println("  " + r.agentName() + ": " + r.activeTickets()));

        step(16, "Демонстрация ROLLBACK");
        demonstrateRollback(ticketService, customer, category);

    }

    private static void demonstrateRollback(TicketService ticketService, User customer, Category category) {
        Ticket temp = ticketService.createTicket(
                "Проверка отката транзакции", "Временная заявка для демонстрации rollback",
                TicketPriority.LOW, customer.getId(), category.getId());
        try {
            ticketService.changeStatus(temp.getId(), TicketStatus.IN_PROGRESS, 999_999L);
            System.out.println("Ошибка не возникла — rollback не продемонстрирован");
        } catch (RuntimeException e) {
            System.out.println("Ожидаемая ошибка: " + rootMessage(e));
        }
        TicketStatus after = ticketService.findById(temp.getId()).orElseThrow().getStatus();
        System.out.println("Статус после отката: " + after);
        System.out.println("Историй статусов у заявки: " + ticketService.getHistory(temp.getId()).size());
        ticketService.deleteTicket(temp.getId());
    }

    private static void checkConnection() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно: "
                    + connection.getMetaData().getDatabaseProductVersion());
            System.out.println("AutoCommit = " + connection.getAutoCommit());
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось подключиться к PostgreSQL", e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private static void step(int number, String title) {
        System.out.println("\n" + number + ". " + title);
    }
}
