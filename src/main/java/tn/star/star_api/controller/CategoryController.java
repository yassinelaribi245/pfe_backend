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

    private final OfferCategoryRepository     categoryRepository;
    private final AssociationMemberRepository memberRepository;

    // GET /api/categories — all categories with member count
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        List<OfferCategory> all = categoryRepository.findAll();
        return ResponseEntity.ok(all.stream().map(c -> {
            long count = memberRepository.countByCategoryId(c.getId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",          c.getId());
            map.put("name",        c.getName());
            map.put("description", c.getDescription());
            map.put("memberCount", count);
            // "assigned" stays for backward compat — true if at least 1 member
            map.put("assigned",    count > 0);
            // "full" = 4 members — admin UI can show this
            map.put("full",        count >= 4);
            return map;
        }).toList());
    }

    // GET /api/categories/free — categories with fewer than 4 members
    // Kept for backward compat but now returns ALL categories
    // (admin can assign to any category regardless)
    @GetMapping("/free")
    public ResponseEntity<?> getFreeCategories() {
        List<OfferCategory> all = categoryRepository.findAll();
        return ResponseEntity.ok(all.stream().map(c -> {
            long count = memberRepository.countByCategoryId(c.getId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",          c.getId());
            map.put("name",        c.getName());
            map.put("memberCount", count);
            map.put("full",        count >= 4);
            return map;
        }).toList());
    }

    // POST /api/categories — admin creates a new category
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Le nom de la catégorie est obligatoire"));
        }
        OfferCategory cat = new OfferCategory();
        cat.setName(name.trim());
        cat.setDescription(body.get("description"));
        OfferCategory saved = categoryRepository.save(cat);
        return ResponseEntity.ok(Map.of(
            "id",   saved.getId(),
            "name", saved.getName()
        ));
    }
}
