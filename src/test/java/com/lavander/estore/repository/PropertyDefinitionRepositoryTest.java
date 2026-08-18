package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PropertyDefinitionRepositoryTest {

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void savesAndReloadsPropertyDefinition() {
        PropertyDefinition ram = new PropertyDefinition("RAM");

        PropertyDefinition saved = propertyDefinitionRepository.save(ram);

        PropertyDefinition reloaded = propertyDefinitionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPropertyName()).isEqualTo("RAM");
    }
}
