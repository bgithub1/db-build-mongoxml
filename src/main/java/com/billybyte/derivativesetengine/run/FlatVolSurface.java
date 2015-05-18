package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.Calendar;

import com.billybyte.commoninterfaces.VolSurfaceInterface;
import com.billybyte.marketdata.SecDef;

public class FlatVolSurface implements VolSurfaceInterface{

	private final BigDecimal vol;
	
	public FlatVolSurface(BigDecimal vol){
		this.vol = vol;
	}
	
	@Override
	public BigDecimal getVol(SecDef secDef, BigDecimal atmPrice,
			BigDecimal atmVol, Double rate, Object[] params, Calendar evaluationDate) {
		if(atmVol==null){
			return vol;			
		}else{
			return atmVol;
		}
	}

	@Override
	public BigDecimal getAtmVol(SecDef secDef, BigDecimal atmPrice,
			BigDecimal atmVol, Double rate, Object[] params, Calendar evaluationDate) {
		if(atmVol==null){
			return vol;			
		}else{
			return atmVol;
		}
	}

	@Override
	public String toString(){
		return this.getClass().getSimpleName()+","+vol;
	}
	
}
