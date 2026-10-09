package helpdesk.service;

import helpdesk.model.TicketComment;
import helpdesk.model.User;
import helpdesk.model.UserRole;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;

import java.util.List;

public class CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public TicketComment addComment(long ticketId, long authorId, String text, boolean internal) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        if (ticketRepository.findById(ticketId).isEmpty()) {
            throw new IllegalArgumentException("Заявка не найдена: " + ticketId);
        }
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Автор не найден: " + authorId));
        if (!author.isActive()) {
            throw new IllegalArgumentException("Пользователь деактивирован: " + author.getName());
        }
        if (internal && author.getRole() == UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Внутренние заметки доступны только сотрудникам");
        }
        return commentRepository.add(new TicketComment(ticketId, authorId, text, internal));
    }

    /** Все комментарии, включая внутренние (для сотрудников). */
    public List<TicketComment> getComments(long ticketId) {
        return commentRepository.findByTicketId(ticketId);
    }

    /** Только внешние комментарии (для клиента). */
    public List<TicketComment> getPublicComments(long ticketId) {
        return commentRepository.findPublicByTicketId(ticketId);
    }
}
