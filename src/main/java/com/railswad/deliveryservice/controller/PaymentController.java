package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.entity.Invoice;
import com.railswad.deliveryservice.repository.InvoiceRepository;
import com.railswad.deliveryservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public PaymentController(PaymentService paymentService, InvoiceRepository invoiceRepository) {
        this.paymentService = paymentService;
        this.invoiceRepository = invoiceRepository;
    }

    @PostMapping("/create-order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> createOrder(@PathVariable Long orderId) throws Exception {
        String razorpayOrderId = paymentService.createRazorpayOrder(orderId);
        return ResponseEntity.ok(razorpayOrderId);
    }

    @PostMapping("/verify-payment/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> verifyPayment(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> paymentDetails) throws Exception {
        paymentService.verifyAndCapturePayment(
                orderId,
                paymentDetails.get("razorpay_payment_id"),
                paymentDetails.get("razorpay_order_id"),
                paymentDetails.get("razorpay_signature"));
        return ResponseEntity.ok("Payment verified and captured");
    }

    @GetMapping("/invoice/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'CUSTOMER')")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // Role-based access check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_CUSTOMER".equals(role) && !invoice.getOrder().getCustomer().getEmail().equals(auth.getName())) {
            throw new AccessDeniedException("Access denied");
        }
        if ("ROLE_VENDOR".equals(role) && !invoice.getOrder().getVendor().getUser().getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Access denied");
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice-" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice.getInvoiceData());
    }
}