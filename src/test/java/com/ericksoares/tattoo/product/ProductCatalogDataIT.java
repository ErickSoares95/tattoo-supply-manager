package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.order.OrderTestFixtures;
import com.ericksoares.tattoo.user.UserTestFixtures;
import com.ericksoares.tattoo.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4 (soft delete), P7 (units sold as a column, sorted/filtered in SQL). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductCatalogDataIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductInventory productInventory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Product save(String name, int stock) {
        return productRepository.saveAndFlush(Product.builder()
                .name(name).price(new BigDecimal("10.00")).stock(stock).category(ProductCategory.INKS).build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aDeletedProductLeavesTheCatalogButItsRowStays() throws Exception {
        Product product = save("Soft Delete Ink " + System.nanoTime(), 5);

        mockMvc.perform(delete("/products/" + product.getId())).andExpect(status().isNoContent());
        productRepository.flush();
        // Each request has its own session in production; here the test's session still holds the
        // entity, and a cached entity skips @SQLRestriction.
        entityManager.clear();

        mockMvc.perform(get("/products/" + product.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get("/products").param("name", product.getName()))
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThrows(ProductNotFoundException.class, () -> productInventory.reserve(product.getId(), 1));

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from products where id = ? and deleted_at is not null", Integer.class, product.getId());
        assertEquals(1, rows, "the row is kept for order history and the sales report");
    }

    @Test
    void unitsSoldFollowsReservationsAndCancellations() {
        Product product = save("Units Ink " + System.nanoTime(), 10);

        productInventory.reserve(product.getId(), 3);
        productInventory.reserve(product.getId(), 2);
        productInventory.release(product.getId(), 2);
        productRepository.flush();

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(3, reloaded.getUnitsSold());
        assertEquals(7, reloaded.getStock());
    }

    @Test
    void bestSellersComeFirstStraightFromTheDatabase() throws Exception {
        String prefix = "Ranking " + System.nanoTime() + " ";
        Product slow = save(prefix + "Slow", 50);
        Product fast = save(prefix + "Fast", 50);
        productInventory.reserve(slow.getId(), 1);
        productInventory.reserve(fast.getId(), 5);
        productRepository.flush();

        mockMvc.perform(get("/products").param("name", prefix).param("sort", "unitsSold,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(prefix + "Fast"))
                .andExpect(jsonPath("$.content[0].unitsSold").value(5))
                .andExpect(jsonPath("$.content[1].name").value(prefix + "Slow"));
    }

    @Test
    void onDealIsFilteredInSql() throws Exception {
        String prefix = "Deal " + System.nanoTime() + " ";
        Product old = save(prefix + "Old", 5);
        save(prefix + "New", 5);
        jdbcTemplate.update("update products set creation_date = now() - interval '4 months' where id = ?", old.getId());

        mockMvc.perform(get("/products").param("name", prefix).param("onDeal", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(prefix + "Old"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aUserWithOrdersCannotBeDeleted() throws Exception {
        var client = UserTestFixtures.createUser(context, "has-orders" + System.nanoTime() + "@test.com", UserType.CLIENT);
        OrderTestFixtures.saveOrder(context, client.getId(), 1L, "Ink", 1, BigDecimal.TEN);

        mockMvc.perform(delete("/users/" + client.getId())).andExpect(status().isConflict());
    }

}
