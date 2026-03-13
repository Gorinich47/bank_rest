package com.example.bankcards.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        //Иинжектим из application.yml
        liquibase.setDataSource(dataSource);
        //Указываем путь к главному файлу миграций
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
        // принудительный запуск
        liquibase.setShouldRun(true);
        // указываем контексты для запуска
        liquibase.setContexts("development,production");
        // Устанавливаем фильтр на изменение таблиц
        liquibase.setLabelFilter(null);
        return liquibase;
    }
}