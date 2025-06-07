package com.railswad.deliveryservice.either;

import io.vavr.control.Either;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class EitherResponse<L, R> {
    private Either<L, R> either;

    public static <L, R> ResponseEntity<?> toResponseEntity(EitherResponse<L, R> eitherResponse) {
        return eitherResponse.getEither()
                .fold(
                        left -> ResponseEntity.badRequest().body(left),
                        right -> ResponseEntity.ok(right)
                );
    }
}