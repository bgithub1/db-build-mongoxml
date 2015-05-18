package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.queries.ComplexQueryResult;

public class MockBigDecimalQuery implements QueryInterface<String, ComplexQueryResult<BigDecimal>>{
	private final BigDecimal value;
	
	public MockBigDecimalQuery(BigDecimal value) {
		super();
		this.value = value;
	}

	@Override
	public ComplexQueryResult<BigDecimal> get(String key, int timeoutValue,
			TimeUnit timeUnitType) {
		return new ComplexQueryResult<BigDecimal>(null,value);
	}

}
