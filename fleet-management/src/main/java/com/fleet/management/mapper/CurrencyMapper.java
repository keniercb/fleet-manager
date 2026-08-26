package com.fleet.management.mapper;

import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.model.Currency;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface CurrencyMapper {

    CurrencyResponse toResponse(Currency entity);
}
