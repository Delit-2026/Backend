package com.dealit.dealit.domain.location.client;

import java.math.BigDecimal;

public interface KakaoLocalClient {

	KakaoCoordToAddressResult resolve(BigDecimal latitude, BigDecimal longitude);
}
