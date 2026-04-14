package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Address;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.AddressRepository;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public AddressController(AddressRepository addressRepository, UserRepository userRepository, AuthService authService) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    // auth resolved via AuthService

    @GetMapping
    public ResponseEntity<ApiResponse<List<Address>>> getAll(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<List<Address>>(false, "Unauthorized", null));
        return ResponseEntity.ok(new ApiResponse<List<Address>>(true, null, addressRepository.findByUserId(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Address>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Address address) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<Address>(false, "Unauthorized", null));
        address.setUserId(user.getId());
        Address saved = addressRepository.save(address);
        return ResponseEntity.ok(new ApiResponse<Address>(true, "Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Address>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Address updated) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<Address>(false, "Unauthorized", null));
        return addressRepository.findById(id).map(a -> {
            if (!a.getUserId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse<Address>(false, "Forbidden", null));
            a.setFullName(updated.getFullName());
            a.setPhone(updated.getPhone());
            a.setAddressLine(updated.getAddressLine());
            a.setIsDefault(updated.getIsDefault());
            addressRepository.save(a);
            return ResponseEntity.ok(new ApiResponse<Address>(true, "Updated", a));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Address>(false, "Not Found", null)));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<Address>> setDefault(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<Address>(false, "Unauthorized", null));
        return addressRepository.findById(id).map(a -> {
            if (!a.getUserId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse<Address>(false, "Forbidden", null));
            // unset other defaults
            addressRepository.findByUserId(user.getId()).forEach(addr -> { addr.setIsDefault(false); addressRepository.save(addr); });
            a.setIsDefault(true);
            addressRepository.save(a);
            return ResponseEntity.ok(new ApiResponse<Address>(true, "Updated", a));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Address>(false, "Not Found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<Object>(false, "Unauthorized", null));
        return addressRepository.findById(id).map(a -> {
            if (!a.getUserId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse<Object>(false, "Forbidden", null));
            addressRepository.delete(a);
            return ResponseEntity.ok(new ApiResponse<Object>(true, "Deleted", null));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Object>(false, "Not Found", null)));
    }
}
