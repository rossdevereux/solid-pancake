package com.voucher.admin.controller;

import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherTemplateRepository;
import com.voucher.core.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final VoucherTemplateRepository templateRepository;

    @GetMapping
    public List<VoucherTemplate> getAll() {
        return templateRepository.findByOrgId(TenantContext.getTenantId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoucherTemplate> getById(@PathVariable String id) {
        return templateRepository.findByIdAndOrgId(id, TenantContext.getTenantId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public VoucherTemplate create(@RequestBody VoucherTemplate template) {
        template.setOrgId(TenantContext.getTenantId());
        return templateRepository.save(template);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherTemplate> update(@PathVariable String id, @RequestBody VoucherTemplate template) {
        String orgId = TenantContext.getTenantId();
        return templateRepository.findByIdAndOrgId(id, orgId)
                .map(existing -> {
                    template.setId(id);
                    template.setOrgId(orgId);
                    return ResponseEntity.ok(templateRepository.save(template));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        String orgId = TenantContext.getTenantId();
        if (!templateRepository.findByIdAndOrgId(id, orgId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
