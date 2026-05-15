package com.graduation.autograding.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.graduation.autograding.repository.CourseRepository;
import com.graduation.autograding.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:data_seeder_profile_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.startup.open-browser=false"
})
@DirtiesContext
class DataSeederProfileIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void defaultProfileDoesNotSeedDemoUsersOrCourses() {
        assertEquals(0L, userRepository.count());
        assertEquals(0L, courseRepository.count());
    }
}
