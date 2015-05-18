package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.VolSurfaceInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
//import com.billybyte.marketdata.options.FixedAtmSplineVolSurface;
//import com.billybyte.marketdata.options.FlatVolSurface;
//import com.billybyte.marketdata.options.FloatingSplineVolSurface;
import com.billybyte.queries.ComplexQueryResult;

public class MockFloatingVolSurfaceQuery implements QueryInterface<String, ComplexQueryResult<VolSurfaceInterface>> {
//	private MockAtmBasedOnSettleQuery atmQuery ;//= new MockAtmBasedOnSettleQuery();
	private QueryInterface<String, ComplexQueryResult<BigDecimal>> atmQuery ;//= new MockAtmBasedOnSettleQuery();
	static double[] stdArray = {-3.0,-1.0,0.0,1.0,3.0};
	static double[] volPumps = {0.18,0.055,0.0,0.03,0.12};
	static double[][] stdVsPumps = {
			{stdArray[0],volPumps[0]},
			{stdArray[1],volPumps[1]},
			{stdArray[2],volPumps[2]},
			{stdArray[3],volPumps[3]},
			{stdArray[4],volPumps[4]},
	};
	
	static double atmVol = .470;

	public MockFloatingVolSurfaceQuery(){
		atmQuery = 
				new MockAtmBasedOnSettleQuery();
	}
	
	public MockFloatingVolSurfaceQuery(
			QueryInterface<Set<String>,Map<String,ComplexQueryResult<BigDecimal>>> atmSetQuery){
		final QueryInterface<Set<String>,Map<String,ComplexQueryResult<BigDecimal>>> atmSetQueryFinal = 
				atmSetQuery;
		
		this.atmQuery = new QueryInterface<String, ComplexQueryResult<BigDecimal>>() {
			
			@Override
			public ComplexQueryResult<BigDecimal> get(String key, int timeoutValue,
					TimeUnit timeUnitType) {
				Map<String, ComplexQueryResult<BigDecimal>> bdMap = 
						atmSetQueryFinal.get(CollectionsStaticMethods.setFromArray(new String[]{key}), timeoutValue, timeUnitType);
				return bdMap.get(key);
			}
		};
	}
	private static final FloatingSplineVolSurface createFloatingSpline(
			double[][] stdsAwayFromAtmVsVolPumpForVolAtStrike,
			double[][] stdsAwayFromAtmVsVolPumpForVolAtmVol,
			int precision,
			double curveCenter, double volAtCenterOfAtmVolSurface){
		return new FloatingSplineVolSurface(stdsAwayFromAtmVsVolPumpForVolAtStrike, 
				stdsAwayFromAtmVsVolPumpForVolAtmVol, 
				precision, curveCenter, volAtCenterOfAtmVolSurface);
	}
	
	private  final FloatingSplineVolSurface createFloatingSpline(String key){
		ComplexQueryResult<BigDecimal> atmCqr = this.atmQuery.get(key, 1, TimeUnit.SECONDS);
		if(!atmCqr.isValidResult())return null;
		double atmStrike = atmCqr.getResult().doubleValue();
		return createFloatingSpline(
				stdVsPumps, stdVsPumps, 4, atmStrike, atmVol);
	}


	@Override
	public ComplexQueryResult<VolSurfaceInterface> get(String key,
			int timeoutValue, TimeUnit timeUnitType) {
		FloatingSplineVolSurface surf = createFloatingSpline(key);
		if(surf==null)return new ComplexQueryResult<VolSurfaceInterface>(Utils.IllState(this.getClass(),"can't make mode"), null);
		return new ComplexQueryResult<VolSurfaceInterface>(null,surf);		
	}


}
