package com.gigtasker.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskServiceApplication {

    private TaskServiceApplication() {}

	static void main(String[] args) {
		SpringApplication.run(TaskServiceApplication.class, args);
	}

}
