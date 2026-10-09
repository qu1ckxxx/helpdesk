package helpdesk.service;

import helpdesk.model.Category;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.model.User;
import helpdesk.model.UserRole;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class TicketService {
    private final TicketRepository ticketRepository;
    private final StatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository,
                         StatusHistoryRepository historyRepository,
                         UserRepository userRepository,
                         CategoryRepository categoryRepository,
                         NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(String title, String description, TicketPriority priority,
                               long customerId, Long categoryId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Пустая тема заявки");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден: " + customerId));
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Пользователь деактивирован: " + customer.getName());
        }

        TicketPriority effectivePriority = priority;
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена: " + categoryId));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Категория неактивна: " + category.getName());
            }
            if (effectivePriority == null) {
                effectivePriority = category.getDefaultPriority();   
            }
        }
        if (effectivePriority == null) {
            effectivePriority = TicketPriority.MEDIUM;
        }

        Ticket ticket = new Ticket(title.trim(), description, effectivePriority, customerId, categoryId);
        Ticket saved = ticketRepository.save(ticket);
        notificationService.send("Создана заявка №" + saved.getId());
        return saved;
    }

    public Optional<Ticket> findById(long ticketId) {
        return ticketRepository.findById(ticketId);
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public List<Ticket> findByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        return ticketRepository.findByStatusAndPriority(status, priority);
    }

    public List<Ticket> findByCustomerId(long customerId) {
        return ticketRepository.findByCustomerId(customerId);
    }

    public List<Ticket> findOpenByAssigneeId(long assigneeId) {
        return ticketRepository.findOpenByAssigneeId(assigneeId);
    }

    public List<TicketStatusHistory> getHistory(long ticketId) {
        return historyRepository.findByTicketId(ticketId);
    }

    public Ticket editTicket(long ticketId, String title, String description, TicketPriority priority) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена: " + ticketId));
        ticket.edit(title, description, priority);
        ticketRepository.update(ticket);
        return ticket;
    }

    public boolean deleteTicket(long ticketId) {
        return ticketRepository.deleteById(ticketId);
    }

    public void assignTicket(long ticketId, long supportAgentId, long changedByUserId) {
        User agent = userRepository.findById(supportAgentId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + supportAgentId));
        if (agent.getRole() != UserRole.SUPPORT_AGENT) {
            throw new IllegalArgumentException(
                    "Назначить заявку можно только сотруднику поддержки, а " + agent.getName()
                            + " имеет роль " + agent.getRole());
        }
        if (!agent.isActive()) {
            throw new IllegalArgumentException("Сотрудник деактивирован: " + agent.getName());
        }

        ticketRepository.assign(ticketId, supportAgentId, changedByUserId);
        notificationService.send("Заявка №" + ticketId + " назначена: " + agent.getName());
    }

    public void changeStatus(long ticketId, TicketStatus newStatus, Long userId) {
        ticketRepository.changeStatus(ticketId, newStatus, userId);
        notificationService.send("Заявка №" + ticketId + " → " + newStatus);
    }

    public void startTicket(long ticketId, Long userId) {
        changeStatus(ticketId, TicketStatus.IN_PROGRESS, userId);
    }

    public void resolveTicket(long ticketId, Long userId) {
        changeStatus(ticketId, TicketStatus.RESOLVED, userId);
    }

    public void closeTicket(long ticketId, Long userId) {
        changeStatus(ticketId, TicketStatus.CLOSED, userId);
    }

    public void cancelTicket(long ticketId, Long userId) {
        changeStatus(ticketId, TicketStatus.CANCELLED, userId);
    }

    /** Возврат решённой заявки в работу. */
    public void reopenTicket(long ticketId, Long userId) {
        changeStatus(ticketId, TicketStatus.IN_PROGRESS, userId);
    }
}
