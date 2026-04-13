package com.zestwear.backend.config;

import com.zestwear.backend.models.Product;
import com.zestwear.backend.models.Category;
import com.zestwear.backend.models.User;
import com.zestwear.backend.models.Coupon;
import com.zestwear.backend.models.Banner;
import com.zestwear.backend.repositories.ProductRepository;
import com.zestwear.backend.repositories.CategoryRepository;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.repositories.CouponRepository;
import com.zestwear.backend.repositories.BannerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CouponRepository couponRepository;
    private final BannerRepository bannerRepository;

    public DataSeeder(ProductRepository productRepository, CategoryRepository categoryRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, CouponRepository couponRepository, BannerRepository bannerRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.couponRepository = couponRepository;
        this.bannerRepository = bannerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            Category cat1 = categoryRepository.save(new Category("Áo T-Shirt","ao-tshirt"));
            Category cat2 = categoryRepository.save(new Category("Quần Jeans","quan-jeans"));

            productRepository.save(new Product("T-Shirt Basic", "Cotton t-shirt", new BigDecimal("19.99"), "/images/tshirt.jpg", 100, "t-shirt-basic", true, cat1.getId()));
            productRepository.save(new Product("Jeans Classic", "Regular fit jeans", new BigDecimal("49.99"), "/images/jeans.jpg", 50, "jeans-classic", false, cat2.getId()));
        }

        if (userRepository.count() == 0) {
            User admin = new User("admin@zestwear.test", passwordEncoder.encode("admin123"), "Admin User");
            admin.setRole("Admin");
            admin.setActive(true);
            String refreshToken = UUID.randomUUID().toString();
            admin.setRefreshToken(refreshToken);
            userRepository.save(admin);
        }

        if (couponRepository.count() == 0) {
            couponRepository.save(new Coupon("WELCOME10", "PERCENT", 10.0, true, 0.0, LocalDateTime.now().plusMonths(1)));
            couponRepository.save(new Coupon("SAVE5", "FIXED", 5.0, true, 20.0, LocalDateTime.now().plusMonths(2)));
        }

        if (bannerRepository.count() == 0) {
            bannerRepository.save(new Banner("Spring Sale", "/images/banner1.jpg", "/products", true, 1));
            bannerRepository.save(new Banner("New Arrivals", "/images/banner2.jpg", "/products", true, 2));
        }
    }
}
