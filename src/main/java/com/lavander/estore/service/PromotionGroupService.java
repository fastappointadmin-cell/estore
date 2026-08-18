package com.lavander.estore.service;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.PromotionGroupDto;
import com.lavander.estore.dto.PromotionGroupRequest;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.PromotionGroup;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PromotionGroupRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PromotionGroupService {

    private final PromotionGroupRepository promotionGroupRepository;
    private final TagRepository tagRepository;
    private final ProductVariantRepository productVariantRepository;

    public PromotionGroupService(
            PromotionGroupRepository promotionGroupRepository,
            TagRepository tagRepository,
            ProductVariantRepository productVariantRepository) {
        this.promotionGroupRepository = promotionGroupRepository;
        this.tagRepository = tagRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<PromotionGroupDto> getAll() {
        return promotionGroupRepository.findAll().stream().map(PromotionGroupDto::fromEntity).toList();
    }

    public PromotionGroupDto getById(Long id) {
        return PromotionGroupDto.fromEntity(findEntityById(id));
    }

    public PromotionGroupDto create(PromotionGroupRequest request) {
        PromotionGroup entity = new PromotionGroup(request.groupName());
        entity.setTags(resolveTags(request.tagIds()));
        return PromotionGroupDto.fromEntity(promotionGroupRepository.save(entity));
    }

    public PromotionGroupDto update(Long id, PromotionGroupRequest request) {
        PromotionGroup entity = findEntityById(id);
        entity.setGroupName(request.groupName());
        entity.setTags(resolveTags(request.tagIds()));
        return PromotionGroupDto.fromEntity(promotionGroupRepository.save(entity));
    }

    public void delete(Long id) {
        promotionGroupRepository.delete(findEntityById(id));
    }

    public List<ProductVariantDto> getVariantsForGroup(Long id) {
        PromotionGroup group = findEntityById(id);
        return productVariantRepository.findDistinctByTagsIn(group.getTags()).stream()
                .map(ProductVariantDto::fromEntity)
                .toList();
    }

    private Set<Tag> resolveTags(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(tagRepository.findAllById(safeIds));
    }

    private PromotionGroup findEntityById(Long id) {
        return promotionGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion group not found with id: " + id));
    }
}
