package hu.klavorar.recommendationapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductTypeHit implements Serializable {
    private ProductType type;
    private Long hitCount;
}
