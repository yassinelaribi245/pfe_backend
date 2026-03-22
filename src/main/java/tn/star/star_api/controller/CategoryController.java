package tn.star.star_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.entity.OfferCategory;
import tn.star.star_api.repository.AssociationMemberRepository;
import tn.star.star_api.repository.OfferCategoryRepository;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final OfferCategoryRepository    categoryRepository;
    private final AssociationMemberRepository memberRepository;

    // GET /api/categories — all 12 categories with assignment status
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        List<OfferCategory> all = categoryRepository.findAll();
        return ResponseEntity.ok(all.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",       c.getId());
            map.put("name",     c.getName());
            map.put("description", c.getDescription());
            map.put("assigned", memberRepository.existsByCategoryId(c.getId()));
            return map;
        }).toList());
    }

    // GET /api/categories/free — only categories without a member
    @GetMapping("/free")
    public ResponseEntity<?> getFreeCategories() {
        List<OfferCategory> all = categoryRepository.findAll();
        return ResponseEntity.ok(all.stream()
                .filter(c -> !memberRepository.existsByCategoryId(c.getId()))
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id",   c.getId());
                    map.put("name", c.getName());
                    return map;
                }).toList());
    }
}
