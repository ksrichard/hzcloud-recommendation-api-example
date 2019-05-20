package hu.klavorar.recommendationapi.repository;

import hu.klavorar.recommendationapi.model.Product;
import hu.klavorar.recommendationapi.model.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long> {
    @Query("select p.id from Product p where p.type = ?1 order by function('RAND')")
    List<Long> findRandomProductIdsByType(ProductType type, Pageable pageable);

    @Query("select p.id from Product p where p.type = ?1")
    List<Long> findAllProductIdsByType(ProductType type);

    @Query("select distinct p.type from Product p where p.type not in(?1)")
    List<ProductType> findAllProductTypeNotIn(List<ProductType> types);

    List<Product> findAllByIdIn(List<Long> ids);
}
