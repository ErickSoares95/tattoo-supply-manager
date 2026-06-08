package com;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnUnauthorizedWhenNoToken() throws Exception {

		mockMvc.perform(
						get("/products")
				)
				.andExpect(
						status().isUnauthorized()
				);
	}

	@Test
	@WithMockUser(
			username = "admin",
			roles = "ADMIN"
	)
	void shouldAllowAdminAccess() throws Exception {

		mockMvc.perform(
						get("/products")
				)
				.andExpect(
						status().isOk()
				);
	}

	@Test
	@WithMockUser(
			username = "employee",
			roles = "EMPLOYEE"
	)
	void shouldAllowEmployeeAccess() throws Exception {

		mockMvc.perform(
						get("/products")
				)
				.andExpect(
						status().isOk()
				);
	}
}
