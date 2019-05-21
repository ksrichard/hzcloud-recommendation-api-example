package hu.klavorar.recommendationapi.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import hu.klavorar.recommendationapi.model.Product;
import hu.klavorar.recommendationapi.model.ProductType;
import hu.klavorar.recommendationapi.model.ProductTypeHit;
import hu.klavorar.recommendationapi.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ProductService {

    public static final String PRODUCT_HIT_COUNTER_MAP_NAME = "PRODUCT_HIT_COUNTER";
    public static final String PERSONAL_PRODUCT_CATEGORY_HIT_COUNTER_MAP_NAME = "PERSONAL_PRODUCT_CATEGORY_HIT_COUNTER";

    public static IMap<Long, Long> productHitCounterMap;
    public static IMap<String, List<ProductTypeHit>> typeHits;

    @PostConstruct
    public void setup() {
        productHitCounterMap = hazelcast.getMap(PRODUCT_HIT_COUNTER_MAP_NAME);
        typeHits = hazelcast.getMap(PERSONAL_PRODUCT_CATEGORY_HIT_COUNTER_MAP_NAME);
    }

    @Autowired
    private HazelcastInstance hazelcast;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get all products from DB
     * @return all products
     */
    public Flux<Product> getProducts() {
        return Flux.fromIterable(productRepository.findAll());
    }

    /**
     * Get recommendations for a product
     * @param id Product ID
     * @param limit Number of products to return
     * @return recommended products
     */
    public Flux<Product> getProductRecommendations(Long id, Long limit) {
        Optional<Product> productOpt = productRepository.findById(id);
        if(productOpt.isPresent()) {
            Product product = productOpt.get();
            ProductType productType = product.getType();

            // collect all products from DB with type and recommendation count
            List<Long> productIdsByType = productRepository.findAllProductIdsByType(productType);
            List<Long> recommendedProductIds = productHitCounterMap.entrySet().stream()
                    .filter(entry -> productIdsByType.contains(entry.getKey()) && !entry.getKey().equals(id))
                    .sorted((o1, o2) -> {
                        if (o1.getValue() < o2.getValue()) return 1;
                        if (o1.getValue() > o2.getValue()) return -1;
                        return 0;
                    })
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // fill empty spaces with non visited products
            if(recommendedProductIds.size() < limit) {
                productIdsByType.stream()
                        .filter(productId -> !recommendedProductIds.contains(productId) && !productId.equals(id))
                        .limit(limit-recommendedProductIds.size())
                        .forEach(recommendedProductIds::add);
            }

            // get all details
            List<Product> productList = productRepository.findAllByIdIn(recommendedProductIds);

            return Flux.fromStream(
                    recommendedProductIds.stream().map(productId -> {
                        if(productList.stream().anyMatch(prod -> prod.getId().compareTo(productId) == 0)) {
                            Optional<Product> foundProductOpt = productList.stream()
                                    .filter(prod -> prod.getId().compareTo(productId) == 0)
                                    .findFirst();
                            if(foundProductOpt.isPresent()) {
                                return foundProductOpt.get();
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull)
            );
        }
        return Flux.empty();
    }

    /**
     * Get one product details and update product visits
     * @param session HTTP session of the user
     * @param id Product ID
     * @return Product details
     */
    public Mono<Product> getProduct(WebSession session, Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if(productOpt.isPresent()) {
            // increase global product visit count
            Product product = productOpt.get();
            increaseProductVisit(product);

            // increase personal product visit count
            increasePersonalProductVisit(session, product);

            return Mono.fromCallable(productOpt::get);
        }
        return Mono.empty();
    }

    /**
     * Increase global product visit counter
     * @param product Product to have the visits increased
     */
    public void increaseProductVisit(Product product) {
        if(productHitCounterMap.containsKey(product.getId())) {
            Long productHitCount = productHitCounterMap.get(product.getId());
            productHitCount += 1;
            productHitCounterMap.set(product.getId(), productHitCount);
            log.info("GLOBAL HIT COUNTER: {} -> {}", product, productHitCount);
        } else {
            productHitCounterMap.put(product.getId(), 1L);
            log.info("GLOBAL HIT COUNTER: {} -> {}", product, 1L);
        }
    }

    /**
     * Get common recommendations
     * @param session HTTP session of the user
     * @param limit Maximum number of Products to return as recommendation
     * @return Recommended products
     */
    public Flux<Product> getRecommendations(WebSession session, Long limit) {
        List<ProductTypeHit> hitsForSession = typeHits.get(session.getId());
        List<Product> recommendedProducts = new ArrayList<>();

        if(hitsForSession != null && hitsForSession.size() > 0) {
            // collect all recommendations from user visits first
            hitsForSession.stream()
                    .sorted((o1, o2) -> {
                        if (o1.getHitCount() < o2.getHitCount()) return 1;
                        if (o1.getHitCount() > o2.getHitCount()) return -1;
                        return 0;
                    })
                    .limit(limit)
                    .forEach(productTypeHit -> {
                        log.info("{}", productTypeHit);
                        List<Long> productIdsByType = productRepository.findRandomProductIdsByType(
                                productTypeHit.getType(),
                                PageRequest.of(0, 1)
                        );
                        if(productIdsByType.size() > 0) {
                            Optional<Product> foundProductOpt = productRepository.findById(productIdsByType.get(0));
                            if(foundProductOpt.isPresent()) {
                                if (!recommendedProducts.contains(foundProductOpt.get())){
                                    recommendedProducts.add(foundProductOpt.get());
                                }
                            }
                        }
                    });

            // fill empty spaces with non visited product categories
            if(recommendedProducts.size() < limit) {
                List<ProductType> productTypes = productRepository.findAllProductTypeNotIn(hitsForSession.stream()
                        .map(ProductTypeHit::getType).collect(Collectors.toList()));
                productTypes.stream()
                        .limit(limit - recommendedProducts.size())
                        .map(productType -> {
                            List<Long> productIdsByType = productRepository.findRandomProductIdsByType(
                                    productType,
                                    PageRequest.of(0, 1)
                            );
                            if (productIdsByType.size() > 0) {
                                Optional<Product> foundProductOpt = productRepository.findById(productIdsByType.get(0));
                                if(foundProductOpt.isPresent()) {
                                    return foundProductOpt.get();
                                }
                            }
                            return null;
                        }).filter(Objects::nonNull)
                .forEach(recommendedProducts::add);
            }
        }

        return Flux.fromIterable(recommendedProducts);
    }

    /**
     * Increase per-session product visit counter
     * @param session HTTP session of the user
     * @param product Product to have the visits increased
     */
    public void increasePersonalProductVisit(WebSession session, Product product) {
        List<ProductTypeHit> hitsForSession = typeHits.get(session.getId());

        // no value has been set before to this user
        if(hitsForSession == null) {
            typeHits.set(session.getId(),
                    Stream.of(
                            ProductTypeHit.builder()
                                    .type(product.getType())
                                    .hitCount(1L)
                                    .build()
                    ).collect(Collectors.toList())
            );
            log.info("{} - PERSONAL HIT COUNTER: {} -> {}", session.getId(), product.getType(), 1L);
        } else {

            // we have values and have product type stored before
            if(hitsForSession.stream().anyMatch(hit -> hit.getType().equals(product.getType()))) {
                Optional<ProductTypeHit> optionalProductTypeHit = hitsForSession.stream()
                        .filter(hit -> hit.getType().equals(product.getType()))
                        .findFirst();
                if(optionalProductTypeHit.isPresent()) {
                    ProductTypeHit foundTypeHit = optionalProductTypeHit.get();
                    hitsForSession.remove(foundTypeHit);
                    foundTypeHit.setHitCount(foundTypeHit.getHitCount() + 1);
                    hitsForSession.add(foundTypeHit);
                    typeHits.set(session.getId(), hitsForSession);
                    log.info("{} - PERSONAL HIT COUNTER: {} -> {}", session.getId(), product.getType(), foundTypeHit);
                }
            }

            // we have values, but has no product type stored before
            if(hitsForSession.stream().noneMatch(hit -> hit.getType().equals(product.getType()))) {
                hitsForSession.add(ProductTypeHit.builder()
                        .type(product.getType())
                        .hitCount(1L)
                        .build());
                typeHits.set(session.getId(),hitsForSession);
                log.info("{} - PERSONAL HIT COUNTER: {} -> {}", session.getId(), product.getType(), 1L);
            }

        }
    }

}
