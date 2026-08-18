package com.lavander.estore.service;

import com.lavander.estore.dto.TagDto;
import com.lavander.estore.dto.TagRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PromotionGroupRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final PromotionGroupRepository promotionGroupRepository;
    private final ProductVariantRepository productVariantRepository;

    public TagService(
            TagRepository tagRepository,
            PromotionGroupRepository promotionGroupRepository,
            ProductVariantRepository productVariantRepository) {
        this.tagRepository = tagRepository;
        this.promotionGroupRepository = promotionGroupRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<TagDto> getAll() {
        return tagRepository.findAll().stream().map(TagDto::fromEntity).toList();
    }

    public TagDto getById(Long id) {
        return TagDto.fromEntity(findEntityById(id));
    }

    public TagDto create(TagRequest request) {
        Tag entity = new Tag(request.tagName());
        return TagDto.fromEntity(tagRepository.save(entity));
    }

    public TagDto update(Long id, TagRequest request) {
        Tag entity = findEntityById(id);
        entity.setTagName(request.tagName());
        return TagDto.fromEntity(tagRepository.save(entity));
    }

    public void delete(Long id) {
        Tag entity = findEntityById(id);
        if (promotionGroupRepository.existsByTagsId(id)) {
            throw new ConflictException("Cannot delete tag " + id + ": still used by a promotion group");
        }
        if (productVariantRepository.existsByTagsId(id)) {
            throw new ConflictException("Cannot delete tag " + id + ": still used by a variant");
        }
        tagRepository.delete(entity);
    }

    private Tag findEntityById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tag not found with id: " + id));
    }
}
