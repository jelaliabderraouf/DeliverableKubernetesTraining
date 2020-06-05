package be.businesstraining.repsoitory;

import org.springframework.data.jpa.repository.JpaRepository;

import be.businesstraining.domain.Product;

public interface IProductsRepository extends JpaRepository<Product, String> {

}