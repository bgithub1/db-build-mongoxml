package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;


import com.billybyte.commoninterfaces.VolSurfaceInterface;
import com.billybyte.commonstaticmethods.Dates;
//import com.billybyte.commonstaticmethods.DatesComLib;
//import com.billybyte.commonstaticmethods.MathStuff;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.marketdata.SecDef;
import com.billybyte.mathstuff.MathStuff;

/**
 * 
 * @author bperlman1
 *
 */
public class FloatingSplineVolSurface implements VolSurfaceInterface{
	private final double[][] stdsAwayFromAtmVsVolPumpForVolAtStrike;
	private final double[][] stdsAwayFromAtmVsVolPumpForVolAtmVol;
	private final double curveCenterPrice;
	private final double volAtCenterOfAtmVolSurface;
	private final int precision;
		
	/**
	 * Creates a floating spline volatility surface in which the user can define individual splines for the 
	 * 	movement of the ATM volatility as well as the volatility pump for each strike
	 * @param stdsAwayFromAtmVsVolPumpForVolAtStrike
	 * @param stdsAwayFromAtmVsVolPumpForVolAtmVol
	 * @param precision
	 * @param curveCenter
	 * @param volAtCenterOfAtmVolSurface
	 */
	public FloatingSplineVolSurface(double[][] stdsAwayFromAtmVsVolPumpForVolAtStrike,
			double[][] stdsAwayFromAtmVsVolPumpForVolAtmVol,
			int precision, double curveCenter, double volAtCenterOfAtmVolSurface) {
		super();
		this.stdsAwayFromAtmVsVolPumpForVolAtStrike = stdsAwayFromAtmVsVolPumpForVolAtStrike;
		this.stdsAwayFromAtmVsVolPumpForVolAtmVol = stdsAwayFromAtmVsVolPumpForVolAtmVol;
		this.precision = precision;
		this.curveCenterPrice =  curveCenter;
		this.volAtCenterOfAtmVolSurface = volAtCenterOfAtmVolSurface;
	}

	@Override
	/**
	 * Returns the volatility at the strike supplied in the SecDef at the supplied atmPrice and atmVol 
	 * 		(*NOTE: by passing null to the atmVol argument it will use the volAtCenterOfAtmVolSurface 
	 * 			field specified when the surface was created as the basis volatility)
	 */
	public BigDecimal getVol(SecDef secDef, BigDecimal atmPrice,
			BigDecimal atmVol, Double rate, Object[] params, Calendar evaluationDate) {
		BigDecimal basisAtmVol = (atmVol==null) ? getAtmVol(secDef,atmPrice, null,rate,params,evaluationDate) : atmVol;
		if(basisAtmVol==null)return null;
		double dte = getDaysToExpiryFromSecDef(secDef, evaluationDate)/365.0;
		BigDecimal strike = secDef.getStrike();
		if(strike == null)return basisAtmVol;
		double pumpedVol = basisAtmVol.doubleValue() + MathStuff.cubsplineFromStds(
				strike.doubleValue(),atmPrice.doubleValue(), 
				basisAtmVol.doubleValue(), dte, stdsAwayFromAtmVsVolPumpForVolAtStrike);

		return new BigDecimal(pumpedVol).setScale(precision,RoundingMode.HALF_EVEN);
	}

	@Override
	/**
	 * Returns the ATM volatility associated with the supplied atmPrice 
	 * 		(*NOTE: by passing null to the atmVol argument it will use the volAtCenterOfAtmVolSurface 
	 * 			field specified when the surface was created as the basis volatility)
	 */
	public BigDecimal getAtmVol(SecDef secDef, BigDecimal atmPrice,
			BigDecimal atmVol, Double rate, Object[] params, Calendar evaluationDate) {
		double basisAtmVol = (atmVol==null) ? volAtCenterOfAtmVolSurface : atmVol.doubleValue();
		double dte = getDaysToExpiryFromSecDef(secDef, evaluationDate)/365.0;
		double pumpedVol = basisAtmVol + MathStuff.cubsplineFromStds(
				atmPrice.doubleValue(), curveCenterPrice, 
				basisAtmVol, dte, stdsAwayFromAtmVsVolPumpForVolAtmVol);
		if(Double.isNaN(pumpedVol) || Double.isInfinite(pumpedVol)){
			Utils.prtObErrMess(this.getClass(), secDef.getShortName()+ " has pumpedVol Value of NaN or Infinate");
			Utils.prtObErrMess(this.getClass(), secDef.getShortName()+ " inputs:");
			Utils.prtObErrMess(this.getClass(), "atmPrice : "+atmPrice+" atmVol : "+atmVol+" rate : "+rate);
			return null;
		}
		return new BigDecimal(pumpedVol).setScale(precision,RoundingMode.HALF_EVEN);
	}
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName()+","+volAtCenterOfAtmVolSurface+","+curveCenterPrice+","+precision+","+
				Arrays.deepToString(stdsAwayFromAtmVsVolPumpForVolAtStrike)+","+Arrays.deepToString(stdsAwayFromAtmVsVolPumpForVolAtmVol);
	}

	
	private static long getDaysToExpiryFromSecDef(SecDef sd, Calendar today){
		int expYear = sd.getExpiryYear();
		int expMonth = sd.getExpiryMonth();
		int expDay = sd.getExpiryDay();
		Calendar expiry = Calendar.getInstance();
		expiry.set(expYear, expMonth-1,expDay);
		return Dates.getDifference(today,expiry, TimeUnit.DAYS)+1;
		
	}

}
