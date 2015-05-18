package com.billybyte.derivativesetengine.run.debundles;

import java.math.BigDecimal;

import java.util.Calendar;
import java.util.List;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.QueryManager;
import com.billybyte.dse.inputs.diotypes.AtmDiot;
import com.billybyte.dse.inputs.diotypes.CorrDiot;
import com.billybyte.dse.inputs.diotypes.CorrPairDiot;
import com.billybyte.dse.inputs.diotypes.DivDiot;
import com.billybyte.dse.inputs.diotypes.DteFromSettleDiot;
import com.billybyte.dse.inputs.diotypes.ImpliedCorr;
import com.billybyte.dse.inputs.diotypes.PriceForImpliedCalcDiot;
import com.billybyte.dse.inputs.diotypes.RateDiot;
import com.billybyte.dse.inputs.diotypes.SettlePriceDiot;
import com.billybyte.dse.inputs.diotypes.UnderlingVolsFromVsDiot;
import com.billybyte.dse.inputs.diotypes.VolDiot;
import com.billybyte.dse.inputs.diotypes.VolForKdDiot;
import com.billybyte.dse.inputs.diotypes.VolSurfDiot;
import com.billybyte.dse.models.spread.CsoModel;
import com.billybyte.dse.models.vanilla.VanOptBlackEuropean;
import com.billybyte.dse.queries.DseInputQuery;
import com.billybyte.dse.queries.DteDseInputQuery;
import com.billybyte.dse.queries.ImpliedCsoCorrelationSetQuery;
import com.billybyte.dse.queries.MarJunSepDecSerialOptUnderQuery;
import com.billybyte.dse.queries.SerialOptUnderQuery;
import com.billybyte.dse.queries.SettleToBigDecSetQuery;
import com.billybyte.marketdata.SecDef;



/**
 * Base reference implementation of {@code AbstractDeBundle}
 * @author bperlman1
 *
 */
public class DeBundleQueries extends AbstractDeBundle {
	/**
	 * 
	 * @param secDefQuery
	 * @param settlementQuery
	 * @param atmQuery
	 * @param rateQuery
	 * @param divQuery
	 * @param volQuery
	 * @param volSurfaceQuery
	 * @param corrFromSnSetCqrRetQuery
	 * @param impliedCsoCorrSetQuery
	 * @param baseUnderlyingSdQuery
	 * @param calSwapUnderSdQuery
	 * @param csoUnderSdQuery
	 */
	public DeBundleQueries(
			QueryInterface<String, SecDef> secDefQuery,
			DseInputQuery<SettlementDataInterface> settlementQuery,
			DseInputQuery<BigDecimal> atmQuery,
			DseInputQuery<BigDecimal> rateQuery,
			DseInputQuery<BigDecimal> divQuery,
			DseInputQuery<BigDecimal> volQuery,
			DseInputQuery<BigDecimal> volSurfaceQuery,
			DseInputQuery<BigDecimal> corrFromSnSetCqrRetQuery,
			DseInputQuery<BigDecimal> impliedCsoCorrSetQuery,
			QueryInterface<String, List<SecDef>> baseUnderlyingSdQuery,
			QueryInterface<String, List<SecDef>> calSwapUnderSdQuery,
			QueryInterface<String, List<SecDef>> csoUnderSdQuery) {
		super();
		this.secDefQuery = secDefQuery;
		this.settlementQuery = settlementQuery;
		this.atmQuery = atmQuery;
		this.rateQuery = rateQuery;
		this.divQuery = divQuery;
		this.volQuery = volQuery;
		this.volSurfaceQuery = volSurfaceQuery;
		this.corrFromSnSetCqrRetQuery = corrFromSnSetCqrRetQuery;
		this.impliedCsoCorrSetQuery = impliedCsoCorrSetQuery;
		this.baseUnderlyingSdQuery = baseUnderlyingSdQuery;
		this.calSwapUnderSdQuery = calSwapUnderSdQuery;
		this.csoUnderSdQuery = csoUnderSdQuery;
		this.dteDseInputQuery = new DteDseInputQuery(secDefQuery);
		this.derivativeSetEngine = createDe();
		registerUnderlyingQueries();
		registerDiotTypes();
		registerModels();

	}
	
	
	/**
	 * 
	 * @param secDefQuery
	 * @param settlementQuery
	 * @param atmQuery
	 * @param rateQuery
	 * @param divQuery
	 * @param volQuery
	 * @param volSurfaceQuery
	 * @param corrFromSnSetCqrRetQuery
	 * @param corrPairFromSnSetCqrRetQuery
	 * @param impliedCsoCorrSetQuery
	 * @param baseUnderlyingSdQuery
	 * @param calSwapUnderSdQuery
	 * @param csoUnderSdQuery
	 */
	public DeBundleQueries(
			QueryInterface<String, SecDef> secDefQuery,
			DseInputQuery<SettlementDataInterface> settlementQuery,
			DseInputQuery<BigDecimal> atmQuery,
			DseInputQuery<BigDecimal> rateQuery,
			DseInputQuery<BigDecimal> divQuery,
			DseInputQuery<BigDecimal> volQuery,
			DseInputQuery<BigDecimal> volSurfaceQuery,
			DseInputQuery<BigDecimal> corrFromSnSetCqrRetQuery,
			DseInputQuery<BigDecimal> corrPairFromSnSetCqrRetQuery,
			DseInputQuery<BigDecimal> impliedCsoCorrSetQuery,
			QueryInterface<String, List<SecDef>> baseUnderlyingSdQuery,
			QueryInterface<String, List<SecDef>> calSwapUnderSdQuery,
			QueryInterface<String, List<SecDef>> csoUnderSdQuery) {
	

		this(secDefQuery, settlementQuery, atmQuery, rateQuery, divQuery, 
				volQuery, volSurfaceQuery, 
				corrFromSnSetCqrRetQuery, impliedCsoCorrSetQuery, 
				baseUnderlyingSdQuery, calSwapUnderSdQuery, 
				csoUnderSdQuery);
		getDerivativeSetEngine().getQueryManager().registerDioType(new CorrPairDiot(), corrPairFromSnSetCqrRetQuery);
	}


	private final  QueryInterface<String, SecDef> secDefQuery;

	private final  DseInputQuery<SettlementDataInterface> settlementQuery;
	private final  DseInputQuery<BigDecimal> atmQuery;
	private final  DseInputQuery<BigDecimal> rateQuery;
	private final  DseInputQuery<BigDecimal> divQuery;
	private final  DseInputQuery<BigDecimal> volQuery;
	private final  DseInputQuery<BigDecimal> volSurfaceQuery;
	private final  DseInputQuery<BigDecimal> corrFromSnSetCqrRetQuery;
	private final   DseInputQuery<BigDecimal> impliedCsoCorrSetQuery ;
	private final DteDseInputQuery dteDseInputQuery;
	
	
	
	private final  QueryInterface<String, List<SecDef>> baseUnderlyingSdQuery;
	private final QueryInterface<String, List<SecDef>> calSwapUnderSdQuery;
	private final  QueryInterface<String, List<SecDef>> csoUnderSdQuery;

	private final  DerivativeSetEngine derivativeSetEngine;

	
	@Override
	public QueryInterface<String, SecDef> getSecDefQuery() {
		return this.secDefQuery;
	}

	@Override
	public DseInputQuery<SettlementDataInterface> getSettlementQuery() {
		return this.settlementQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getAtmQuery() {
		return this.atmQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getRateQuery() {
		return this.rateQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getDivQuery() {
		return this.divQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getVolQuery() {
		return this.volQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getVolSurfaceQuery() {
		return this.volSurfaceQuery;
	}

	@Override
	public DseInputQuery<BigDecimal> getCorrFromSnSetCqrRetQuery() {
		return this.corrFromSnSetCqrRetQuery;
	}

	@Override
	public QueryInterface<String, List<SecDef>> getBaseUnderlyingSdQuery() {
		return this.baseUnderlyingSdQuery;
	}

	@Override
	public QueryManager getQueryManager() {
		return getDerivativeSetEngine().getQueryManager();
	}

	@Override
	protected void registerDiotTypes() {

		//************** register model DioTypes ******************
		getQueryManager().registerDioType(new AtmDiot(), getAtmQuery());
		getQueryManager().registerDioType(new VolSurfDiot(), getVolSurfaceQuery());
		getQueryManager().registerDioType(new VolSurfDiot(), getVolSurfaceQuery());
		getQueryManager().registerDioType(new VolDiot(), getVolQuery());
		getQueryManager().registerDioType(new VolForKdDiot(), getVolQuery());
		getQueryManager().registerDioType(new UnderlingVolsFromVsDiot(),getVolSurfaceQuery());
		getQueryManager().registerDioType(new RateDiot(), getRateQuery());
		getQueryManager().registerDioType(new DivDiot(), 
				getDivQuery()==null?getRateQuery():getDivQuery());
		getQueryManager().registerDioType(new CorrPairDiot(), getCorrFromSnSetCqrRetQuery());
		getQueryManager().registerDioType(new CorrDiot(), getCorrFromSnSetCqrRetQuery());
		getQueryManager().registerDioType(new ImpliedCorr(), getImpliedCsoCorrSetQuery());
		getQueryManager().registerDioType(new DteFromSettleDiot(), dteDseInputQuery);
		getQueryManager().registerDioType(new SettlePriceDiot(), getSettlementQuery());
		SettleToBigDecSetQuery bdSettle = new SettleToBigDecSetQuery(getSettlementQuery());
		getQueryManager().registerDioType(new PriceForImpliedCalcDiot(), bdSettle);

		//************** END register model DioTypes ******************
	}

	@Override
	protected void registerUnderlyingQueries() {
		getQueryManager().addUnderlyingSecDefQuery("((FOP)|(FUT)|(OPT)|(STK))",getBaseUnderlyingSdQuery());
		// add a query that gets underlyings for cal swaps
		getQueryManager().addUnderlyingSecDefQuery("((CSX)|(AAO)|(AOX))\\.((FOP)|(FUT))", getCalSwapUnderSdQuery());
		// add a query that gets underlyings for cso's
		getQueryManager().addUnderlyingSecDefQuery(
				ImpliedCsoCorrelationSetQuery.REGEX_GET_STRING, getCsoUnderSdQuery());

		DeBundleQueries.registerUnderlyingSerialMonthOptionQueries(
				getQueryManager(),secDefQuery);
		

		//******************* END load up underlying Queries **************
		
	}

	public static void registerUnderlyingSerialMonthOptionQueries(
			QueryManager qm,
			QueryInterface<String, SecDef> secDefQuery) {
		qm.addUnderlyingSecDefQuery(
				"^((ES)|(GE)|(NQ))\\.FOP\\.GLOBEX\\.USD\\.", 
				new MarJunSepDecSerialOptUnderQuery(secDefQuery));
		qm.addUnderlyingSecDefQuery(
				"^((DX)|(TF)|(RF))\\.FOP\\.NYBOT\\.USD\\.", 
				new MarJunSepDecSerialOptUnderQuery(secDefQuery));
		qm.addUnderlyingSecDefQuery(
				"^((OZN)|(OZB)|(OZF)|(OUB)|(OZD)|(OZT)|(YM))\\.FOP\\.ECBOT\\.USD\\.", 
				new MarJunSepDecSerialOptUnderQuery(secDefQuery));
		qm.addUnderlyingSecDefQuery(
				"^PAO\\.FOP\\.NYMEX\\.USD\\.", 
				new MarJunSepDecSerialOptUnderQuery(secDefQuery));
		qm.addUnderlyingSecDefQuery(
				"^((6A)|(6B)|(6C)|(6E)|(6J)|(6L)|(6M)|(6N)|(6S)|(6Z)|(ND)|(SP))\\.FOP\\.CME\\.USD\\.", 
				new MarJunSepDecSerialOptUnderQuery(secDefQuery));
		int[] hknuzMapper = {3,3,3,5,5,7,7,9,9,12,12,12};
		qm.addUnderlyingSecDefQuery(
				"((CC)|(KC)|(OZC)|(OZO)|(OZW))\\.FOP\\.((NYBOT)|(ECBOT))\\.USD", 
				new SerialOptUnderQuery(secDefQuery, hknuzMapper));
		int[] hknvMapper = {3,3,3,5,5,7,7,10,10,10,3,3};
		qm.addUnderlyingSecDefQuery(
				"^SB\\.FOP\\.NYBOT", 
				new SerialOptUnderQuery(secDefQuery, hknvMapper));
		int[] hknvzMapper = {3,3,3,5,5,7,7,10,10,12,12,12};
		qm.addUnderlyingSecDefQuery(
				"^CT\\.FOP\\.NYBOT", 
				new SerialOptUnderQuery(secDefQuery, hknvzMapper));
		int[] fhknuxMapper = {1,3,3,5,5,7,7,9,9,11,11,1};
		qm.addUnderlyingSecDefQuery(
				"((OJ)|(OZR))\\.FOP\\.((NYBOT)|(ECBOT))\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fhknuxMapper));
		int[] fhknquxMapper = {1,3,3,5,5,7,7,8,9,11,11,1};
		qm.addUnderlyingSecDefQuery(
				"^OZS\\.FOP\\.ECBOT\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fhknquxMapper));
		int[] fhknquvzMapper = {1,3,3,5,5,7,7,8,9,10,12,12};
		qm.addUnderlyingSecDefQuery(
				"((OZM)|(OZL))\\.FOP\\.ECBOT\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fhknquvzMapper));
		int[] gjmqvzMapper = {2,2,4,4,6,6,8,8,10,10,12,12};
		qm.addUnderlyingSecDefQuery(
				"^LE\\.FOP\\.GLOBEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, gjmqvzMapper));
		qm.addUnderlyingSecDefQuery(
				"^OG\\.FOP\\.COMEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, gjmqvzMapper));
		int[] gjkmnqvzMapper = {2,2,4,4,5,6,7,8,10,10,12,12};
		qm.addUnderlyingSecDefQuery(
				"^HE\\.FOP\\.GLOBEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, gjkmnqvzMapper));
		int[] fhjkquvxMapper = {1,3,3,4,5,8,8,8,9,10,11,1};
		qm.addUnderlyingSecDefQuery(
				"^GF\\.FOP\\.GLOBEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fhjkquvxMapper));
		int[] fhknuzMapper = {1,3,3,5,5,7,7,9,9,12,12,12};
		qm.addUnderlyingSecDefQuery(
				"^SO\\.FOP\\.COMEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fhknuzMapper));
		int[] fjnvMapper = {1,4,4,4,7,7,7,10,10,10,1,1};
		qm.addUnderlyingSecDefQuery(
				"^PO\\.FOP\\.NYMEX\\.USD", 
				new SerialOptUnderQuery(secDefQuery, fjnvMapper));
		

		//******************* END load up underlying Queries **************
		
	}

	@Override
	public DerivativeSetEngine getDerivativeSetEngine() {
		return this.derivativeSetEngine;
	}
	
	@Override
	public DseInputQuery<BigDecimal> getImpliedCsoCorrSetQuery() {
		return this.impliedCsoCorrSetQuery;
	}

	@Override
	public QueryInterface<String, List<SecDef>> getCalSwapUnderSdQuery() {
		return this.calSwapUnderSdQuery;
	}

	@Override
	public QueryInterface<String, List<SecDef>> getCsoUnderSdQuery() {
		return this.csoUnderSdQuery;
	}
	
	protected DerivativeSetEngine createDe(){
		if(this.derivativeSetEngine!=null)return this.derivativeSetEngine;
		Calendar todayInMorning = Calendar.getInstance();
		todayInMorning.set(Calendar.HOUR, 06);
		todayInMorning.set(Calendar.MINUTE, 00);
		todayInMorning.set(Calendar.SECOND, 00);
		QueryManager qm = new  QueryManager();
		DerivativeSetEngine de = new DerivativeSetEngine(qm,
				getSecDefQuery(), todayInMorning);

		return de;
	}

	@Override
	protected void registerModels() {
		Calendar todayInMorning = Calendar.getInstance();
		todayInMorning.set(Calendar.HOUR, 06);
		todayInMorning.set(Calendar.MINUTE, 00);
		todayInMorning.set(Calendar.SECOND, 00);
		// ---------------- add models ------------------------
		derivativeSetEngine.addModel(true, ImpliedCsoCorrelationSetQuery.REGEX_GET_STRING, 
				new CsoModel());
		derivativeSetEngine.addModel(true, "KD\\.FOP\\.", new VanOptBlackEuropean(todayInMorning, 
				new VolDiot()));
		
	}


}
