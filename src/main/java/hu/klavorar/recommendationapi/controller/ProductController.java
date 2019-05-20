package hu.klavorar.recommendationapi.controller;

import hu.klavorar.recommendationapi.model.Product;
import hu.klavorar.recommendationapi.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(value = "/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Flux<Product> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{productId}")
    public Mono<Product> getProduct(@PathVariable("productId") Long productId, WebSession session) {
        return productService.getProduct(session, productId);
    }

    @GetMapping("/{productId}/recommendations")
    public Flux<Product> getProductRecommendations(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "limit", defaultValue= "10") Long limit
    ) {
        return productService.getProductRecommendations(productId, limit);
    }

    @GetMapping("/recommendations")
    public Flux<Product> getRecommendations(
            @RequestParam(name = "limit", defaultValue= "10") Long limit,
            WebSession session
    ) {
        return productService.getRecommendations(session, limit);
    }

}
