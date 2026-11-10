package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.response.RouteRespDto;
import com.springboot.simplyfly.model.Route;

public class RouteMapper {

    private RouteMapper() {
    }

    public static RouteRespDto convertEntityToDto(Route route) {
        return new RouteRespDto(
                route.getId(),
                route.getSource().getId(),
                route.getSource().getCode(),
                route.getSource().getCity(),
                route.getDestination().getId(),
                route.getDestination().getCode(),
                route.getDestination().getCity(),
                route.isActive()
        );
    }
}
