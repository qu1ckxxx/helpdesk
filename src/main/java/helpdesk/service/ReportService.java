package helpdesk.service;

import helpdesk.model.TicketDetails;
import helpdesk.repository.TicketReportRepository;
import helpdesk.repository.TicketReportRepository.AgentWorkload;
import helpdesk.repository.TicketReportRepository.CategoryCount;
import helpdesk.repository.TicketReportRepository.StatusCount;

import java.util.List;
import java.util.Optional;

public class ReportService {
    private final TicketReportRepository reportRepository;

    public ReportService(TicketReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<TicketDetails> getAllTicketDetails() {
        return reportRepository.findAllDetails();
    }

    public Optional<TicketDetails> getTicketDetails(long ticketId) {
        return reportRepository.findDetailsById(ticketId);
    }

    public List<StatusCount> countByStatus() {
        return reportRepository.countByStatus();
    }

    public List<CategoryCount> countByCategory() {
        return reportRepository.countByCategory();
    }

    public List<AgentWorkload> agentWorkload() {
        return reportRepository.agentWorkload();
    }
}
