package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VectorStore vectorStore;

    @Test
    void shouldIndexAllProductsAsDocuments() {

        Product product = Product.builder()
                .name("Tattoo Ink - Black")
                .description("Professional black tattoo ink")
                .price(BigDecimal.valueOf(45.90))
                .stock(98)
                .build();
        product.setId(7L);

        when(productRepository.findAll()).thenReturn(List.of(product));

        IndexProductCatalogService service = new IndexProductCatalogService(productRepository, vectorStore);

        int indexed = service.indexAll();

        assertEquals(1, indexed);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        Document document = captor.getValue().get(0);
        String expectedId = UUID.nameUUIDFromBytes("product-7".getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(expectedId, document.getId());
        assertEquals(7L, document.getMetadata().get("productId"));
        assertEquals("Tattoo Ink - Black", document.getMetadata().get("productName"));
    }
}
