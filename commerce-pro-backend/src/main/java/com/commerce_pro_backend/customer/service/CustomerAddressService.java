package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.AddressDTO;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.entity.CustomerAddress;
import com.commerce_pro_backend.customer.enums.AddressType;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepo;
    private final CustomerService           customerService;
    private final CustomerMapper            mapper;

    public List<AddressDTO.Response> getAddresses(String customerId) {
        customerService.findCustomer(customerId);
        return addressRepo.findByCustomerId(customerId)
                .stream().map(mapper::toAddressResponse).collect(Collectors.toList());
    }

    @Transactional
    public AddressDTO.Response addAddress(String customerId, AddressDTO.Request req) {
        Customer customer = customerService.findCustomer(customerId);

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepo.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(a -> { a.setIsDefault(false); addressRepo.save(a); });
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .addressLabel(req.getAddressLabel())
                .addressType(req.getAddressType() != null ? AddressType.valueOf(req.getAddressType()) : AddressType.SHIPPING)
                .fullName(req.getFullName()).phone(req.getPhone()).company(req.getCompany())
                .addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .city(req.getCity()).state(req.getState())
                .postalCode(req.getPostalCode()).country(req.getCountry())
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : false)
                .build();

        return mapper.toAddressResponse(addressRepo.save(address));
    }

    @Transactional
    public AddressDTO.Response updateAddress(String customerId, String addressId, AddressDTO.Request req) {
        customerService.findCustomer(customerId);
        CustomerAddress address = findAddress(addressId, customerId);

        if (Boolean.TRUE.equals(req.getIsDefault()) && !address.getIsDefault()) {
            addressRepo.clearDefaultExcept(customerId, addressId);
        }

        address.setAddressLabel(req.getAddressLabel());
        if (req.getAddressType() != null) address.setAddressType(AddressType.valueOf(req.getAddressType()));
        address.setFullName(req.getFullName()); address.setPhone(req.getPhone());
        address.setCompany(req.getCompany()); address.setAddressLine1(req.getAddressLine1());
        address.setAddressLine2(req.getAddressLine2()); address.setCity(req.getCity());
        address.setState(req.getState()); address.setPostalCode(req.getPostalCode());
        address.setCountry(req.getCountry());
        if (req.getIsDefault() != null) address.setIsDefault(req.getIsDefault());

        return mapper.toAddressResponse(addressRepo.save(address));
    }

    @Transactional
    public void deleteAddress(String customerId, String addressId) {
        customerService.findCustomer(customerId);
        CustomerAddress address = findAddress(addressId, customerId);
        addressRepo.delete(address);
    }

    @Transactional
    public AddressDTO.Response setDefault(String customerId, String addressId) {
        customerService.findCustomer(customerId);
        addressRepo.clearDefaultExcept(customerId, addressId);
        CustomerAddress address = findAddress(addressId, customerId);
        address.setIsDefault(true);
        return mapper.toAddressResponse(addressRepo.save(address));
    }

    private CustomerAddress findAddress(String addressId, String customerId) {
        CustomerAddress a = addressRepo.findById(addressId)
                .orElseThrow(() -> ApiException.notFound("CustomerAddress", addressId));
        if (!a.getCustomer().getId().equals(customerId))
            throw ApiException.forbidden("Address does not belong to this customer");
        return a;
    }
}
