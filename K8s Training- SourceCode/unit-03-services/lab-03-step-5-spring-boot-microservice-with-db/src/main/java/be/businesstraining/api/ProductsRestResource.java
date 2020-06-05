package be.businesstraining.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.businesstraining.domain.Product;
import be.businesstraining.repsoitory.IProductsRepository;

@RestController
@RequestMapping("/products")
public class ProductsRestResource {

	private IProductsRepository repository;

	public ProductsRestResource(IProductsRepository repository) {
		this.repository = repository;
	}

	@GetMapping    // Endpoint (GET) for Listing all the products
	public ResponseEntity<List<Product>> allProducts() {

		return ResponseEntity.ok(repository.findAll());
	}

	@PostMapping    // Endpoint (POST) for adding a new product
	public ResponseEntity<Product> addProduct(@RequestBody Product p) {
		repository.save(p);
		return ResponseEntity.ok(p);
	}
}
