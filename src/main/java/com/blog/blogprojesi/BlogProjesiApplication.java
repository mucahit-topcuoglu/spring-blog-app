package com.blog.blogprojesi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.blog.blogprojesi.repository")
public class BlogProjesiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogProjesiApplication.class, args);
    }

}