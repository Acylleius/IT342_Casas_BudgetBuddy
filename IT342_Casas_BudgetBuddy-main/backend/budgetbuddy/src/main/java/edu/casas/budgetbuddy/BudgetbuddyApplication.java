package edu.casas.budgetbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class BudgetbuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BudgetbuddyApplication.class, args);
	}

}