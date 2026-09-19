package letsPlay.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import letsPlay.dto.ProductRequest;
import letsPlay.enums.Role;
import letsPlay.exception.GlobalException;
import letsPlay.models.ProductModel;
import letsPlay.models.UserModel;
import letsPlay.repository.ProductRepository;
import letsPlay.repository.UserRepository;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    public List<ProductModel> getAllProducts() {
        return productRepository.findAll();
    }

    public ProductModel getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new GlobalException("Product not found", HttpStatus.NOT_FOUND));
    }

    public ProductModel createProduct(ProductRequest request, String ownerName) {
        UserModel owner = userRepository.findByName(ownerName)
                .orElseThrow(() -> new GlobalException("user not found", HttpStatus.UNAUTHORIZED));

        ProductModel product = new ProductModel();
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setUserId(owner.getId());

        return productRepository.save(product);
    }

    public ProductModel updateProduct(String id, ProductRequest request, String ownerName, Role role) {
        ProductModel product = getProduct(id);
        ensureCanModify(product, ownerName, role);

        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());

        return productRepository.save(product);
    }

    public void deleteProduct(String id, String ownerName, Role role) {
        ProductModel product = getProduct(id);
        ensureCanModify(product, ownerName, role);
        productRepository.delete(product);
    }

    private void ensureCanModify(ProductModel product, String ownerName, Role role) {
        if (role == Role.ADMIN) {
            return;
        }
        UserModel owner = userRepository.findByName(ownerName)
                .orElseThrow(() -> new GlobalException("Authenticated user not found", HttpStatus.UNAUTHORIZED));

        if (!product.getUserId().equals(owner.getId())) {
            throw new GlobalException("You can only modify your own products", HttpStatus.FORBIDDEN);
        }
    }
}