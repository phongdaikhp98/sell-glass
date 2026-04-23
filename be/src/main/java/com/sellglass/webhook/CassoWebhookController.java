package com.sellglass.webhook;

import com.sellglass.order.Order;
import com.sellglass.order.OrderRepository;
import com.sellglass.order.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/v1/webhooks/casso")
@RequiredArgsConstructor
@Slf4j
public class CassoWebhookController {

    @Value("${casso.secure-token}")
    private String secureToken;

    private final OrderRepository orderRepository;

    private static final Pattern ORDER_CODE_PATTERN =
            Pattern.compile("SG-([A-Fa-f0-9]{8})", Pattern.CASE_INSENSITIVE);

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "secure-token", required = false) String token,
            @RequestBody CassoWebhookPayload payload) {

        if (token == null || !MessageDigest.isEqual(
                secureToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            log.warn("Casso webhook: invalid secure token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload.getError() != 0 || payload.getData() == null) {
            return ResponseEntity.ok().build();
        }

        CassoWebhookPayload.Data data = payload.getData();

        if (!"in".equals(data.getTransactionType())) {
            return ResponseEntity.ok().build();
        }

        String description = data.getDescription();
        if (description == null) {
            return ResponseEntity.ok().build();
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(description);
        if (!matcher.find()) {
            log.debug("Casso webhook: no order code in description: {}", description);
            return ResponseEntity.ok().build();
        }

        String suffix = matcher.group(1).toLowerCase();

        Optional<Order> orderOpt = orderRepository.findUnpaidByOrderCodeSuffix(suffix);
        if (orderOpt.isEmpty()) {
            log.debug("Casso webhook: no unpaid order found for suffix SG-{}", suffix.toUpperCase());
            return ResponseEntity.ok().build();
        }

        Order order = orderOpt.get();
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
        log.info("Casso webhook: order {} marked as PAID (SG-{})", order.getId(), suffix.toUpperCase());

        return ResponseEntity.ok().build();
    }
}
