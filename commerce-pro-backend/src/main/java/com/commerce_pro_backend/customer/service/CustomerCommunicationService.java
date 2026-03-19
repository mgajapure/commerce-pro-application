package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.CommunicationLogDTO;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.entity.CustomerCommunicationLog;
import com.commerce_pro_backend.customer.enums.CommunicationChannel;
import com.commerce_pro_backend.customer.enums.CommunicationDirection;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.CustomerCommunicationLogRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerCommunicationService {

    private final CustomerCommunicationLogRepository logRepo;
    private final CustomerService    customerService;
    private final CustomerMapper     mapper;
    private final CurrentUserService currentUserService;

    public PageResponse<CommunicationLogDTO.Response> getLogs(String customerId, Pageable pageable) {
        customerService.findCustomer(customerId);
        Page<CustomerCommunicationLog> page = logRepo.findByCustomerId(customerId, pageable);
        var dtos = page.getContent().stream().map(mapper::toCommLogResponse).collect(Collectors.toList());
        return PageResponse.from(page, dtos);
    }

    @Transactional
    public CommunicationLogDTO.Response addLog(String customerId, CommunicationLogDTO.Request req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer customer = customerService.findCustomer(customerId);

        CustomerCommunicationLog log = CustomerCommunicationLog.builder()
                .customer(customer)
                .channel(CommunicationChannel.valueOf(req.getChannel()))
                .direction(CommunicationDirection.valueOf(req.getDirection()))
                .subject(req.getSubject()).body(req.getBody())
                .orderId(req.getOrderId()).orderNumber(req.getOrderNumber())
                .loggedBy(actorId)
                .requiresFollowUp(req.getRequiresFollowUp() != null ? req.getRequiresFollowUp() : false)
                .followUpAt(req.getFollowUpAt())
                .build();

        return mapper.toCommLogResponse(logRepo.save(log));
    }

    @Transactional
    public CommunicationLogDTO.Response resolve(String customerId, String logId) {
        customerService.findCustomer(customerId);
        CustomerCommunicationLog log = logRepo.findById(logId)
                .orElseThrow(() -> ApiException.notFound("CommunicationLog", logId));
        log.setIsResolved(true);
        return mapper.toCommLogResponse(logRepo.save(log));
    }
}
