package com.sabbir.ums_assignment.controller;

import com.sabbir.ums_assignment.dto.*;
import com.sabbir.ums_assignment.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {
    private final AccountService service;
    private static final String ROLE = "ADMIN";

    @GetMapping
    public List<AccountResponse> findAll() {
        return service.findAll(ROLE);
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        return service.findById(ROLE, id);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody AccountRequest request) {
        AccountResponse result = service.create(ROLE, request);
        return ResponseEntity.created(URI.create("/api/admins/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @RequestBody AccountRequest request) {
        return service.update(ROLE, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(ROLE, id);
        return ResponseEntity.noContent().build();
    }
}
