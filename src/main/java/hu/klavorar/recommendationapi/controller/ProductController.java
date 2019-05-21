package hu.klavorar.recommendationapi.controller;

import hu.klavorar.recommendationapi.model.Product;
import hu.klavorar.recommendationapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * Get all products endpoint
     * @return all products
     */
    @GetMapping
    public Flux<Product> getProducts() {
        return productService.getProducts();
    }

    /**
     * Get one product details by ID
     * @param productId Product ID
     * @param session User's HTTP Session
     * @return Product details
     */
    @GetMapping("/{productId}")
    public Mono<Product> getProduct(@PathVariable("productId") Long productId, WebSession session) {
        return productService.getProduct(session, productId);
    }

    /**
     * Get recommendations for a product
     * @param productId Product ID
     * @param limit Maximum number of product recommendations to show
     * @return Recommended products
     */
    @GetMapping("/{productId}/recommendations")
    public Flux<Product> getProductRecommendations(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "limit", defaultValue= "10") Long limit
    ) {
        return productService.getProductRecommendations(productId, limit);
    }

    /**
     * Get general recommendations for the current user's session
     * @param limit Maximum number of product recommendations
     * @param session User's HTTP session
     * @return Recommended products
     */
    @GetMapping("/recommendations")
    public Flux<Product> getRecommendations(
            @RequestParam(name = "limit", defaultValue= "10") Long limit,
            WebSession session
    ) {
        return productService.getRecommendations(session, limit);
    }

}
