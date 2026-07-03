package com.example.ordercancel.mapper;

import com.example.ordercancel.dto.OrderResponseDto;
import com.example.ordercancel.model.Order;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper — converts JPA Order entity to public-facing OrderResponseDto.
 * componentModel = "spring" means MapStruct generates a Spring @Component,
 * injectable via @Autowired.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponseDto toDto(Order order);

    List<OrderResponseDto> toDtoList(List<Order> orders);
}
