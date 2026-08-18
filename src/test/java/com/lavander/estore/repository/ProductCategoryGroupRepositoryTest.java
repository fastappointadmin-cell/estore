package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductCategoryGroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private ProductSubCategoryGroupRepository subGroupRepository;

    @Test
    void subGroupIsVisibleFromParentGroupAfterReload() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        subGroupRepository.save(new ProductSubCategoryGroup("Computers", electronics));

        entityManager.flush();
        entityManager.clear();

        ProductCategoryGroup reloaded = groupRepository.findById(electronics.getId()).orElseThrow();
        assertThat(reloaded.getSubGroups()).hasSize(1);
        assertThat(reloaded.getSubGroups().get(0).getGroupName()).isEqualTo("Computers");
    }

    @Test
    void deletingSubGroupDoesNotCascadeDeleteParentGroup() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductSubCategoryGroup computers = subGroupRepository.save(new ProductSubCategoryGroup("Computers", electronics));
        entityManager.flush();

        subGroupRepository.delete(computers);
        entityManager.flush();

        assertThat(groupRepository.findById(electronics.getId())).isPresent();
    }
}
