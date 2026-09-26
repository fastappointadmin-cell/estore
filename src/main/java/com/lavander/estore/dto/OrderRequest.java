package com.lavander.estore.dto;

import com.lavander.estore.model.DeliveryMethod;
import com.lavander.estore.model.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank String customerFullName,
        @NotBlank String customerPhone,
        @NotBlank @Email String customerEmail,
        @NotNull DeliveryMethod deliveryMethod,
        @NotBlank String shippingStreet,
        @NotBlank String shippingCity,
        @NotBlank String shippingCounty,
        @NotBlank String shippingPostalCode,
        @NotNull PaymentMethod paymentMethod,
        String promoCode) {
}
