package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.entity.Invoice;
import com.railswad.deliveryservice.entity.Order;
import com.railswad.deliveryservice.entity.OrderItem;
import com.railswad.deliveryservice.repository.InvoiceRepository;
import com.railswad.deliveryservice.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${razorpay.company-name}")
    private String companyName;

    @Value("${razorpay.company-address}")
    private String companyAddress;

    @Value("${razorpay.gst-rate}")
    private double gstRate;

    @Value("${razorpay.gst-number}")
    private String companyGstNumber;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Autowired
    public PaymentService(RazorpayClient razorpayClient, OrderRepository orderRepository, InvoiceRepository invoiceRepository) {
        this.razorpayClient = razorpayClient;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public String createRazorpayOrder(Long orderId) throws RazorpayException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order has no items");
        }

        // Calculate amounts
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        double gstAmount = subtotal * gstRate;
        double deliveryCharges = order.getDeliveryCharges() != null ? order.getDeliveryCharges() : 0.0;
        double finalAmount = subtotal + gstAmount + deliveryCharges;

        // Update order
        order.setTotalAmount(subtotal);
        order.setTaxAmount(gstAmount);
        order.setFinalAmount(finalAmount);
        order.setPaymentStatus("PENDING");
        orderRepository.save(order);

        // Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (finalAmount * 100)); // Amount in paise
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", "order_" + orderId);
        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        return razorpayOrder.get("id");
    }

    @Transactional
    public void verifyAndCapturePayment(Long orderId, String paymentId, String razorpayOrderId, String signature) throws RazorpayException {
        // Create JSONObject for attributes
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_payment_id", paymentId);
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_signature", signature);

        // Construct payload for signature verification
        String payload = razorpayOrderId + "|" + paymentId;
        boolean isValid = com.razorpay.Utils.verifySignature(payload, signature, razorpayKeySecret);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid payment signature");
        }

        // Capture payment
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        JSONObject captureRequest = new JSONObject();
        captureRequest.put("amount", (int) (order.getFinalAmount() * 100)); // Use finalAmount
        captureRequest.put("currency", currency);
        razorpayClient.payments.capture(paymentId, captureRequest);

        // Update order
        order.setPaymentStatus("CAPTURED");
        order.setPaymentMethod("RAZORPAY");
        orderRepository.save(order);

        // Generate invoice
        generateAndSaveInvoice(order, paymentId);
    }

    private void generateAndSaveInvoice(Order order, String paymentId) {
        if (order.getCustomer() == null || order.getVendor() == null || order.getOrderItems() == null) {
            throw new IllegalArgumentException("Order must have a customer, vendor, and items");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yOffset = 750;
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, yOffset);
            contentStream.showText("INVOICE");
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            yOffset -= 20;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Company: " + companyName);
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Address: " + companyAddress);
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("GSTIN: " + companyGstNumber);
            yOffset -= 50;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Invoice Number: INV-" + order.getOrderId() + "-" + UUID.randomUUID().toString().substring(0, 8));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Invoice Date: " + ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            yOffset -= 50;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Customer: " + order.getCustomer().getUsername());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Email: " + order.getCustomer().getEmail());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Vendor: " + order.getVendor().getUser().getUsername());
            yOffset -= 50;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Order ID: " + order.getOrderId());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Payment ID: " + paymentId);
            yOffset -= 50;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Items:");
            for (OrderItem item : order.getOrderItems()) {
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText(item.getItem().getItemName() + " x " + item.getQuantity() + " @ INR " + String.format("%.2f", item.getUnitPrice()));
                yOffset -= 15;
            }
            yOffset -= 50;
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Subtotal: INR " + String.format("%.2f", order.getTotalAmount()));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("GST (" + (gstRate * 100) + "%): INR " + String.format("%.2f", order.getTaxAmount()));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Delivery Charges: INR " + String.format("%.2f", (order.getDeliveryCharges() != null ? order.getDeliveryCharges() : 0.0)));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Total: INR " + String.format("%.2f", order.getFinalAmount()));
            contentStream.endText();
            contentStream.close();

            // Save PDF to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            // Save invoice to database
            Invoice invoice = new Invoice();
            invoice.setOrder(order);
            invoice.setInvoiceNumber("INV-" + order.getOrderId() + "-" + UUID.randomUUID().toString().substring(0, 8));
            invoice.setInvoiceDate(ZonedDateTime.now());
            invoice.setCustomerName(order.getCustomer().getUsername());
            invoice.setCustomerEmail(order.getCustomer().getEmail());
            invoice.setVendorName(order.getVendor().getUser().getUsername());
//            invoice.setVendorGstNumber(order.getVendor().getGstNumber());
            invoice.setSubtotal(order.getTotalAmount());
            invoice.setGstRate(gstRate);
            invoice.setGstAmount(order.getTaxAmount());
            invoice.setTotalAmount(order.getFinalAmount());
            invoice.setPaymentStatus(order.getPaymentStatus());
            invoice.setInvoiceData(baos.toByteArray());
            invoiceRepository.save(invoice);

            order.setInvoice(invoice);
            orderRepository.save(order);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }
}