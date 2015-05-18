package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;


import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.Utils;

import com.billybyte.queries.ComplexQueryResult;

public class MockAtmBasedOnSettleQuery implements QueryInterface<String, ComplexQueryResult<BigDecimal>>{
	private final QueryInterface<String, ComplexQueryResult<SettlementDataInterface>> settleQuery ;
	
	public MockAtmBasedOnSettleQuery(){
		settleQuery = new MockSettlementQuery();
	}
	
	@Override
	public ComplexQueryResult<BigDecimal> get(String key, int timeoutValue,
			TimeUnit timeUnitType) {
		ComplexQueryResult<SettlementDataInterface> db = settleQuery.get(key, timeoutValue, timeUnitType);
		if(db ==null){
			return errRet(key,"null settle");
		}else if(!db.isValidResult()){
			return new ComplexQueryResult<BigDecimal>(db.getException(), null);
		}

		ComplexQueryResult<BigDecimal> ret = 
				new ComplexQueryResult<BigDecimal>(null, db.getResult().getPrice());
		return ret;
	}
	
	private ComplexQueryResult<BigDecimal> errRet(String key,String s){
		Exception e = Utils.IllState(this.getClass(),key+" : "+s);
		return new ComplexQueryResult<BigDecimal>(e, null);
	}
}
