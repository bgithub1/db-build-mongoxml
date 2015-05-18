package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;



import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.QueryManager;
import com.billybyte.dse.inputs.diotypes.AtmDiot;
import com.billybyte.dse.inputs.diotypes.PriceForImpliedCalcDiot;
import com.billybyte.dse.inputs.diotypes.VolDiot;
import com.billybyte.dse.outputs.DerivativeReturn;
import com.billybyte.dse.outputs.DerivativeReturnDisplay;
import com.billybyte.dse.outputs.ImpliedVolDerSen;
import com.billybyte.dse.queries.DseInputQuery;
import com.billybyte.dse.queries.SettleToBigDecSetQuery;
import com.billybyte.marketdata.MarketDataComLib;
import com.billybyte.marketdata.SecDef;
import com.billybyte.marketdata.ShortNameInfo;
import com.billybyte.marketdata.SecEnums.SecCurrency;
import com.billybyte.marketdata.SecEnums.SecExchange;
import com.billybyte.marketdata.SecEnums.SecSymbolType;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.marketdata.futures.FuturesProduct;
import com.billybyte.marketdata.futures.FuturesProductQuery;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.queries.ComplexQueryResult;
import com.billybyte.queries.SettleQueryFromMongo;

/**
 *
 * Create a Implied Vol maps for creation of the 
 *   MongoXml<BigDecimal> database of implied vols from settlement prices.
 * 
 * 
 * @author bperlman1
 *
 */
public class CreateMongoSettleImpliedVolDatabase {
	private static BigDecimal seed = new BigDecimal(".25");
	private static final BigDecimal two = new BigDecimal("2");
	private static final int YYYYMM_STRING_SIZE = 6;
	
	private final String shortNameSep = MarketDataComLib.DEFAULT_SHORTNAME_SEPARATOR;
	private final DerivativeSetEngine de ;
	private final QueryInterface<String,SecDef> sdQuery;
	private final MongoXml<SettlementDataImmute> mongoSettleDb;
	private final FuturesProductQuery fpq = 
			new FuturesProductQuery();
	private final Map<String,String> underSymToBestOptSym = new HashMap<String, String>();
	
	
	/**
	 * This class will register the mongoSettleDb that is passed to it
	 *   as both the SettlePriceDiot DioType query, and 
	 *   the PriceForImpliedCalcDiot DioType query.
	 *   
	 * @param de
	 * @param mongoSettleDb - used for SettleToBigDecSetQuery
	 * @param sdQuery
	 */
	public CreateMongoSettleImpliedVolDatabase(
			DerivativeSetEngine de,
			MongoXml<SettlementDataImmute> mongoSettleDb,
			QueryInterface<String, SecDef> sdQuery) {
		super();
		this.de = de;
		this.mongoSettleDb = mongoSettleDb;
		this.sdQuery = sdQuery;
		
		// register the mongo query to use as the SettlePriceDiot inputs query,
		//   and as the PriceForImpliedCalcDiot inputs query
		QueryInterface<Set<String>,Map<String,ComplexQueryResult<SettlementDataInterface>>> mongoSettleQuery = 
				new SettleQueryFromMongo(mongoSettleDb, null);

		SettleToBigDecSetQuery bdSettle = new SettleToBigDecSetQuery(mongoSettleQuery);
		de.getQueryManager().registerDioType(new PriceForImpliedCalcDiot(), bdSettle);
		de.getQueryManager().registerDioType(new AtmDiot(), bdSettle);

		
		DseInputQuery<BigDecimal> volSet =  new volSetDseInputQuery();

		de.getQueryManager().registerDioType(new VolDiot(), volSet);
		

	
	}


	private static class volSetDseInputQuery extends DseInputQuery<BigDecimal>{
		private final MockBigDecimalQuery volSingle = new MockBigDecimalQuery(seed);
		QueryOfSetFromSingleQuery<String, ComplexQueryResult<BigDecimal>> volSet = 
				new QueryOfSetFromSingleQuery<String, ComplexQueryResult<BigDecimal>>(volSingle);
		@Override
		public Map<String, ComplexQueryResult<BigDecimal>> get(Set<String> key,
				int timeoutValue, TimeUnit timeUnitType) {
			// 
			return volSet.get(key, timeoutValue, timeUnitType);
		}

	}
	
	
	
	/**
	 * From a List<SettlementDataInterface>, compute implied vols for the options
	 *   in this list of settlements
	 * @param optionSettles - List<SettlementDataInterface>
	 * @return List<DerivativeReturnDisplay>
	 */
	public List<DerivativeReturnDisplay> getOptionImpliedVols(
			List<SettlementDataInterface> optionSettles){
		List<DerivativeReturnDisplay> ret = new ArrayList<DerivativeReturnDisplay>();
		
		Set<String> optSnSet = new TreeSet<String>();
		for(SettlementDataInterface settle : optionSettles){
			if(settle == null)continue;
			optSnSet.add(settle.getShortName());
		}
		
		Map<String,DerivativeReturn[]> implVolCqrMap = 
				de.getSensitivity(new ImpliedVolDerSen(), optSnSet);
		
		for (Entry<String,DerivativeReturn[]> entry : implVolCqrMap.entrySet()) {
			DerivativeReturn[] drArr = entry.getValue();
			if(drArr == null || drArr.length<1)continue;
			for(DerivativeReturn dr : drArr){
				ret.add(new DerivativeReturnDisplay(dr.getWithRespectToShortName(), dr));
			}
		}
		
		return ret;

	}
	
	/**
	 * Get a list of settlements from a regexString
	 * @param regexStrings
	 * @param timeoutValue
	 * @param timeUnitType
	 * @return
	 */
	public List<SettlementDataInterface> getOptionSettlementsFromRegexStrings(
		String[] regexStrings){

		List<SettlementDataInterface> ret = 
				new ArrayList<SettlementDataInterface>();
		
		for(String regexString : regexStrings){
			Map<String, SettlementDataImmute> settleMap = 
					mongoSettleDb.getByRegex(regexString);
			for(SettlementDataInterface settle : settleMap.values()){
				if(settle != null){
					ret.add(settle);
				}
			}
		}
		return ret;
	}

	public Map<String,DerivativeReturnDisplay> getAtmImpliedVolsFromRegexStrings(
			String[] regexStrings,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		Map<String, SettlementDataInterface> atmSettles = 
				this.getAtmOptionSettlesForUnderlyings(
						regexStrings, timeoutValue, 
						timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow);
		return(this.getAtmImpliedVols(atmSettles));

	}
	
	public Map<String,DerivativeReturnDisplay> getAtmImpliedVolsFromSnSet(
			Set<String> snSet,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		Map<String, SettlementDataInterface> atmSettles = 
				this.getAtmOptionSettlesForUnderlyingsFromSnSet(
						snSet, timeoutValue, 
						timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow);
		
		Map<String,DerivativeReturnDisplay> ret = new HashMap<String, DerivativeReturnDisplay>();
		ImpliedVolDerSen sense = new ImpliedVolDerSen();
		if(atmSettles.size()!=snSet.size()){
			for(String sn:snSet){
				if(!atmSettles.containsKey(sn)){
					ret.put(sn, new DerivativeReturnDisplay(sn, sense, 
							sn, null, Utils.IllState(this.getClass(), sn+" : can not find any options for this security")));
				}
			}
		}
		ret.putAll(this.getAtmImpliedVols(atmSettles));
		return(ret);
		
	}
			
		
	/**
	 * Get implied vols for all options in the atmOptionSettles map.
	 * The key to each map entry is an underlying shortName, and the 
	 * entry value is the option settlement that whose strike is closest
	 * to the underlying's settle price.
	 * 
	 * @param atmOptionSettles - Map<String, SettlementDataInterface>
	 * @return Map<String,DerivativeReturnDisplay> where the key is the 
	 *    underlying shortName.
	 */
	public Map<String,DerivativeReturnDisplay> getAtmImpliedVols(
			Map<String, SettlementDataInterface> atmOptionSettles){

		Map<String,DerivativeReturnDisplay> ret = new TreeMap<String,DerivativeReturnDisplay>();
		
		Set<String> optSnSet = new TreeSet<String>();
		for(Entry<String,SettlementDataInterface> entry : atmOptionSettles.entrySet()){
			if(entry == null || entry.getValue() == null || entry.getKey() == null){
				Utils.prtObErrMess(this.getClass(),entry.getKey() +  "no atm settle for this key");
				continue;
			}
			optSnSet.add(entry.getValue().getShortName());
		}
					
		
		Map<String,DerivativeReturn[]> implVolCqrMap = 
				de.getSensitivity(new ImpliedVolDerSen(), optSnSet);
		
		
		for (String underlyingSn : atmOptionSettles.keySet()){
			if(!atmOptionSettles.containsKey(underlyingSn)){
				continue;
			}
			SettlementDataInterface settle = atmOptionSettles.get(underlyingSn);
			if(settle==null){
				continue;
			}
			String optSn = settle.getShortName();
			DerivativeReturn[] drArr = implVolCqrMap.get(optSn);
			if(drArr == null || drArr.length<1){
				continue;
			}
			DerivativeReturnDisplay drd = 
					new DerivativeReturnDisplay(optSn,drArr[0]);
			ret.put(underlyingSn, drd);
		}
		
		
		
		return ret;
		
	}
	
	public Map<String, SettlementDataInterface>  getAtmOptionSettlesForUnderlyingsFromSnSet(
			Set<String> snSet,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		Map<String, SettlementDataInterface> ret = new TreeMap<String, SettlementDataInterface>();
		
		Map<String, SettlementDataImmute> settleMap = 
				mongoSettleDb.findFromSet(snSet);
		List<SettlementDataInterface> futList = 
				new ArrayList<SettlementDataInterface>();
		List<SettlementDataInterface> stkList = 
				new ArrayList<SettlementDataInterface>();
		for(SettlementDataInterface settle : settleMap.values()){
			if(settle.getShortName().contains(".FUT.")){
				futList.add(settle);
				continue;
			}
			if(settle.getShortName().contains(".STK.")){
				stkList.add(settle);
				continue;
			}
		}
		ret.putAll(
				getAtmSettlesForFut(
						futList, timeoutValue, 
						timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow));
		// process STK
		ret.putAll(
				getAtmSettlesForStk(
						stkList, timeoutValue, 
						timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow));
		return ret;
		
	}

	
	
	/**
	 * Get ATM options for a list of underlyings
	 * @param regexStrings - regex string that describes underlyings
	 * @param timeoutValue
	 * @param timeUnitType
	 * @param maxPercDiffBtwSettleAndOpStrikeToAllow - max perc diff that you will
	 *   allow between the closest to the money option that you find and the underlying
	 *   settle price.
	 *   
	 * @return Map<String, SettlementDataInterface> 
	 */
	public Map<String, SettlementDataInterface>  getAtmOptionSettlesForUnderlyings(
			String[] regexStrings,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		
		Map<String, SettlementDataInterface> ret = new TreeMap<String, SettlementDataInterface>();
		
		for(String regexString : regexStrings){
			Map<String, SettlementDataImmute> settleMap = 
					mongoSettleDb.getByRegex(regexString);
			List<SettlementDataInterface> futList = 
					new ArrayList<SettlementDataInterface>();
			List<SettlementDataInterface> stkList = 
					new ArrayList<SettlementDataInterface>();
			for(SettlementDataInterface settle : settleMap.values()){
				if(settle.getShortName().contains(".FUT.")){
					futList.add(settle);
					continue;
				}
				if(settle.getShortName().contains(".STK.")){
					stkList.add(settle);
					continue;
				}
			}
			
			// process FUT
			ret.putAll(
					getAtmSettlesForFut(
							futList, timeoutValue, 
							timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow));
			// process STK
			ret.putAll(
					getAtmSettlesForStk(
							stkList, timeoutValue, 
							timeUnitType, maxPercDiffBtwSettleAndOpStrikeToAllow));
			
		}
		return ret;
	}

	/**
	 * 
	 * Get the option contract that is closest to the money for each
	 *   futures contract in arg list.
	 * 
	 * @param list - List<SettlementDataInterface> of futures settles
	 * @param timeoutValue
	 * @param timeUnitType
	 * @param maxPercDiffBtwSettleAndOpStrikeToAllow - see caller
	 * @return
	 */
	private final Map<String, SettlementDataInterface> getAtmSettlesForFut(
			List<SettlementDataInterface> list,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		
		Map<String, SettlementDataInterface> ret = new  TreeMap<String, SettlementDataInterface>();
		
		QueryInterface<String, Map<String, SettlementDataInterface>> regexSetQuery = 
				new RegexSettleQuery();
		
		for(SettlementDataInterface futSettle : list){
			if(futSettle == null) continue;
			SecDef futSd = sdQuery.get(futSettle.getShortName(), timeoutValue, timeUnitType);
			
			if(futSd == null) continue;
			String futSym =futSd.getSymbol();
			FuturesProduct fp = fpq.get(futSym, timeoutValue, timeUnitType);
			if(fp == null )continue;
			
			// this check will see if we have attempted to find the bestOptSym, and failed.
			//   if so, don't do this future.
			if(this.underSymToBestOptSym.containsKey(futSym) && this.underSymToBestOptSym.get(futSym)==null){
				continue;
			}
			String bestOptSymToUse = this.underSymToBestOptSym.containsKey(futSym) ? 
					underSymToBestOptSym.get(futSym) :
					fpq.findBestOptionSymbolForFuturesAtmVol(
							futSd, timeoutValue, timeUnitType, regexSetQuery);
			
			if(bestOptSymToUse == null ) continue;
			// save it for next time
			this.underSymToBestOptSym.put(futSym, bestOptSymToUse);
			
			// get all options for this future
			TreeMap<BigDecimal, SettlementDataInterface[]> treeMap = 
					createFopStrikeToSettleTreeMap(futSettle,bestOptSymToUse,
							timeoutValue, timeUnitType);
			if(treeMap == null || treeMap.size()<1)continue;
			
			// find strike closest to settle
			ret.put(futSettle.getShortName(), 
					getBestStrike(futSettle, treeMap, maxPercDiffBtwSettleAndOpStrikeToAllow));

		}
		
		return ret;

	}
	
	/**
	 * Create a treeMap of Futures options by strike.
	 * @param futSettle
	 * @param bestOptSymToUse
	 * @param timeoutValue
	 * @param timeUnitType
	 * @return
	 */
	private TreeMap<BigDecimal, SettlementDataInterface[]>  createFopStrikeToSettleTreeMap(
			SettlementDataInterface futSettle,
			String bestOptSymToUse,
			int timeoutValue,
			TimeUnit timeUnitType){

		
		TreeMap<BigDecimal, SettlementDataInterface[]> strikeToSettlesMap = 
				new TreeMap<BigDecimal, SettlementDataInterface[]>();
				
		if(futSettle == null)return null;
		
		String futSn = futSettle.getShortName();
		
		if(futSn == null)return null;
		String replaced = futSn.replace(
				SecSymbolType.FUT.toString(), SecSymbolType.FOP.toString());
		String sep = shortNameSep.replace(shortNameSep, "\\"+shortNameSep);
		String[] parts = replaced.split(sep);
		if(parts.length<5)return null;
		
		// re-assemble partialShortName with optSym
		String s = MarketDataComLib.DEFAULT_SHORTNAME_SEPARATOR;
		String partialFopRegex = bestOptSymToUse;
		for(int i = 1;i<parts.length;i++){
			partialFopRegex += shortNameSep + parts[i];
		}
		
		partialFopRegex = "^" + partialFopRegex.replace(s, "\\" +s);
		
		// here is the REGEX get from mongo
		Map<String, SettlementDataImmute> setMap = 
				mongoSettleDb.getByRegex(partialFopRegex);
		if(setMap == null || setMap.size()<1)return null;
		
		
		for(SettlementDataInterface opSettle : setMap.values()){
			SecDef opSd = sdQuery.get(opSettle.getShortName(),
					timeoutValue, timeUnitType);
			if(opSd == null) continue;
			BigDecimal strike = opSd.getStrike();
//			String opSn = opSd.getShortName();
			if(strike == null)continue;
			if(!strikeToSettlesMap.containsKey(strike)){
				strikeToSettlesMap.put(strike, new SettlementDataInterface[2]);
			}
			SettlementDataInterface[] arr = strikeToSettlesMap.get(strike);
			if(MarketDataComLib.isCall(opSd)){
				if(validateOption(futSettle, opSettle)){
					arr[0] = opSettle;
				}
			}else{
				if(validateOption(futSettle, opSettle)){
					arr[1] = opSettle;
				}
			}
			strikeToSettlesMap.put(strike,arr);
		}
		return strikeToSettlesMap;
	}
	
	private final boolean validateOption(
			SettlementDataInterface futSettle,
			SettlementDataInterface optSettle){
		if(futSettle == null || optSettle == null )return false;
		BigDecimal test  = futSettle.getPrice().multiply(two);
		if(optSettle.getPrice().compareTo(test)>=0) return false;
		return true;
	}
	
	class RegexSettleQuery implements QueryInterface<String, Map<String, SettlementDataInterface>>{

		@Override
		public Map<String, SettlementDataInterface> get(String key,
				int timeoutValue, TimeUnit timeUnitType) {
			Map<String, SettlementDataImmute> map = mongoSettleDb.getByRegex(key);
			Map<String, SettlementDataInterface> ret  = new HashMap<String, SettlementDataInterface>();
			for(Entry<String, SettlementDataImmute> entry : map.entrySet()){
				ret.put(entry.getKey(), entry.getValue());
			}
			return ret ;
		}
		
	}
	
	private SettlementDataInterface getBestStrike(
			SettlementDataInterface underSettle,
			TreeMap<BigDecimal,SettlementDataInterface[]> treeMap,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){

		// find strike closest to settle
		BigDecimal price = underSettle.getPrice();
		Entry<BigDecimal, SettlementDataInterface[]> ceilingEntry = treeMap.ceilingEntry(price);
		BigDecimal ceilingStrike = ceilingEntry == null ? 
				null : 
					ceilingEntry.getKey();
		Entry<BigDecimal, SettlementDataInterface[]> floorEntry = treeMap.floorEntry(price);
		BigDecimal floorStrike = floorEntry == null ? 
				null : 
					floorEntry.getKey();
		
		BigDecimal strikeToUse = null;
		
		if(ceilingStrike != null && floorStrike != null){
			BigDecimal csDiff = ceilingStrike.subtract(price);
			BigDecimal fsDiff = price.subtract(floorStrike);
			if(csDiff.compareTo(fsDiff)<0){
				strikeToUse = ceilingStrike;
			}else{
				strikeToUse = floorStrike;
			}
		}else if(ceilingStrike !=null && floorStrike ==null){
			strikeToUse = ceilingStrike;
		}else if(ceilingStrike ==null && floorStrike !=null){
			strikeToUse = floorStrike;
		}
		
		// see if strikeToUse is "reasonably" close to price
		// test here for zero price and for percDiffBtwStrikeToUseAndPrice not being to high
		SettlementDataInterface settleToUse = null;
		if(price.compareTo(BigDecimal.ZERO)==0){
			return settleToUse;
		}
		BigDecimal percDiffBtwStrikeToUseAndPrice = strikeToUse.subtract(price).abs().divide(price,6,RoundingMode.HALF_EVEN);;
		if(percDiffBtwStrikeToUseAndPrice.compareTo(
				maxPercDiffBtwSettleAndOpStrikeToAllow)<=0){
			// !!!!!!!!!!! IF YOU GET HERE, YOU HAVE FOUND A STRIKE
			//     THAT IS CLOSE ENOUGH TO USE FOR IMPLIED VOL OF A FUTURE
			//   YIPPIE !!!!!!
			SettlementDataInterface[] arr = treeMap.get(strikeToUse);
			SettlementDataInterface call = arr[0];
			SettlementDataInterface put = arr[1];
			
			if(call != null && strikeToUse.compareTo(price)>=0){
				settleToUse = call;
			}else if( put != null && strikeToUse.compareTo(price)<=0){
				settleToUse = put;
			}else if(call != null && put==null){
				settleToUse = call;
			}else if(put !=null && call==null){
				settleToUse = put;
			}
			
			if( settleToUse != null ){
				Utils.prt(underSettle.getShortName() + "," + settleToUse.getShortName() + "," + settleToUse.getPrice());
			}else{
				Utils.prt(underSettle.getShortName() + "," + " no option settle found");
			}
		}
		return settleToUse;
	}
	
	/**
	 * 
	 * @author bperlman1
	 *
	 */
	class ValueForImpliedCalcQuery implements QueryInterface<Set<String>,Map<String,ComplexQueryResult<BigDecimal>>> {
		private final 
			QueryInterface<Set<String>,Map<String,ComplexQueryResult<SettlementDataInterface>>> settleQuery;


		/**
		 * 
		 * @param atmOptionSettles - Map<String, SettlementDataInterface>
		 */
		ValueForImpliedCalcQuery(QueryInterface<Set<String>,Map<String,ComplexQueryResult<SettlementDataInterface>>> settleQuery){
			this.settleQuery = settleQuery;
		}
		
		@Override
		public Map<String, ComplexQueryResult<BigDecimal>> get(Set<String> keySet,
				int timeoutValue, TimeUnit timeUnitType) {
			
			
			Map<String,ComplexQueryResult<BigDecimal>> ret = 
					new HashMap<String, ComplexQueryResult<BigDecimal>>();
			
			Map<String,ComplexQueryResult<SettlementDataInterface>>  cqrMap =
					settleQuery.get(keySet, timeoutValue, timeUnitType);
			for(Entry<String,ComplexQueryResult<SettlementDataInterface>> entry : cqrMap.entrySet()){
				ret.put(entry.getKey(),
						new ComplexQueryResult<BigDecimal>(
								entry.getValue().getException(),
								entry.getValue().getResult().getPrice()));
			}
			return ret ;
		}
		
	}


	private final Map<String, SettlementDataInterface> getAtmSettlesForStk(
			List<SettlementDataInterface> list,
			int timeoutValue,
			TimeUnit timeUnitType,
			BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow){
		Map<String, SettlementDataInterface> ret = new TreeMap<String, SettlementDataInterface>();
		
		QueryInterface<String, Map<String, SettlementDataInterface>> regexSetQuery = 
				new RegexSettleQuery();
		for(SettlementDataInterface stkSettle : list){
			if(stkSettle == null) continue;
			SecDef stkSd = sdQuery.get(stkSettle.getShortName(), timeoutValue, timeUnitType);
			
			if(stkSd == null) continue;
			TreeMap<BigDecimal, SettlementDataInterface[]> treeMap = 
					createOptStrikeToSettleTreeMap(stkSettle,
							timeoutValue, timeUnitType,regexSetQuery,10,4);
			if(treeMap == null || treeMap.size()<1)continue;
			// find strike closest to settle
			ret.put(stkSettle.getShortName(), 
					getBestStrike(stkSettle, treeMap, maxPercDiffBtwSettleAndOpStrikeToAllow));

		}

		return ret;
	}

	private final TreeMap<BigDecimal, SettlementDataInterface[]> createOptStrikeToSettleTreeMap(
			SettlementDataInterface stkSettle,
			int timeoutValue,
			TimeUnit timeUnitType,
			QueryInterface<String,
				Map<String, SettlementDataInterface>> regexBasedSettleQuery,
			int minNumOfOptSettles,
			int numOfMonthsToSearch
			){
		if(stkSettle==null)return null;
		SecDef sd = sdQuery.get(stkSettle.getShortName(), timeoutValue, timeUnitType);
		// always go one month out
		TreeMap<BigDecimal, SettlementDataInterface[]> ret = 
				new TreeMap<BigDecimal, SettlementDataInterface[]>();
		for(int i = 1;i<=minNumOfOptSettles;i++){
			Calendar oneMonthMore = Dates.addToCalendar(
					de.getEvaluationDate(), i, Calendar.MONTH, true);
			String s = MarketDataComLib.DEFAULT_SHORTNAME_SEPARATOR;
			if(oneMonthMore == null)return null;
			Long yyyyMmDd =  Dates.getYyyyMmDdFromCalendar(oneMonthMore);
			if(yyyyMmDd == null)return null;
			String yyyyMm = yyyyMmDd.toString().trim().substring(0,YYYYMM_STRING_SIZE);
			String partialOptName = sd.getSymbol() + s + 
					SecSymbolType.OPT + s + 
					SecExchange.SMART + s + 
					SecCurrency.USD + s +
					yyyyMm;
			partialOptName = "^" + partialOptName.replace(s, "\\"+s);
			
			Map<String,SettlementDataInterface> setMap = 
					regexBasedSettleQuery.get(partialOptName, timeoutValue, timeUnitType);
			if(setMap.size()<minNumOfOptSettles){
				continue;
			}
			for(SettlementDataInterface opSettle : setMap.values()){
				SecDef opSd = sdQuery.get(opSettle.getShortName(),
						timeoutValue, timeUnitType);
				if(opSd == null) continue;
				BigDecimal strike = opSd.getStrike();
				if(strike == null)continue;
				if(!ret.containsKey(strike)){
					ret.put(strike, new SettlementDataInterface[2]);
				}
				SettlementDataInterface[] arr = ret.get(strike);
				if(MarketDataComLib.isCall(opSd)){
					if(validateOption(stkSettle, opSettle)){
						arr[0] = opSettle;
					}
				}else{
					if(validateOption(stkSettle, opSettle)){
						arr[1] = opSettle;
					}
				}
				ret.put(strike,arr);
			}
	
		}
		
		return ret;
		
	}


	/**
	 * Create In The Money (Itm) implied vols that are equal to their
	 *   Out of the Money (Otm) counterparts
	 *   
	 * @param ItmAndOtmOptImpliedVolMap - a map of implied vols that
	 *   includes both Itm and Otm options.
	 *   
	 * @return Map<String, BigDecimal>
	 * 
	 */
	public Map<String, BigDecimal> createItmIvFromOtm(
			Map<String, BigDecimal> itmAndOtmOptImpliedVolMap){
		QueryManager qm = de.getQueryManager();
		List<String> underlyingNames = new ArrayList<String>();
		Map<String,String> optToUnderMap = new HashMap<String, String>();
		for(String optSn : itmAndOtmOptImpliedVolMap.keySet()){
			List<SecDef> sdList = qm.getUnderlyingSecDefs(optSn, 1, TimeUnit.SECONDS);
			if(sdList==null || sdList.size()<1)continue;
			String underSn = sdList.get(0).getShortName();
			underlyingNames.add(underSn);
			optToUnderMap.put(optSn, underSn);
		}
		Map<String,SettlementDataImmute> settleMap =
				mongoSettleDb.findFromList(underlyingNames);
		
		Map<String, BigDecimal> ret = new HashMap<String, BigDecimal>();
		
		// *************** MAIN LOOP IS HERE **************************
		for(String optSn : itmAndOtmOptImpliedVolMap.keySet()){
			if(ret.containsKey(optSn))continue;
			String underSn = optToUnderMap.get(optSn);
			if(underSn==null)continue;
			if(!settleMap.containsKey(underSn))continue;
			SecDef optSd = this.sdQuery.get(optSn, 1, TimeUnit.SECONDS);
			if(optSd==null)continue;
			BigDecimal strike = optSd.getStrike();
			if(strike==null)continue;
			// get underSettle
			BigDecimal underSettle = settleMap.get(underSn).getPrice();
			
			//*********** SEE If underSettle is greater than strike ****************
			if(underSettle.compareTo(strike)>0){
				// ************ CASE 1  the settle IS greater than the strike
				// use put, b/c it is Out of the Money
				// First, see if the optSd is a call
				if(MarketDataComLib.isCall(optSd)){
					// if so, then we need to get the put vol from itmAndOtmOptImpliedVolMap 
					//new ShortNameInfo(symbol, symbolType, exchange, currency, contractYear, contractMonth, contractDay, right, strike)
					ShortNameInfo sni = new ShortNameInfo(
							optSd.getSymbol(),
							optSd.getSymbolType(), 
							optSd.getExchange(), 
							optSd.getCurrency(), 
							optSd.getContractYear(), 
							optSd.getContractMonth(),null, "P", strike);
					String putSn = sni.getShortName();
					if(!itmAndOtmOptImpliedVolMap.containsKey(putSn)){
						// just put the call and forget the put
						ret.put(optSn, itmAndOtmOptImpliedVolMap.get(optSn));
					}else{
						// get put vol
						BigDecimal vol = itmAndOtmOptImpliedVolMap.get(putSn);
						// and use it for both put and call
						//*********** !!!!!! here's where you insert both put and call vol
						//   when the settle is greater than the strike
						ret.put(optSn, vol);
						ret.put(putSn, vol);
					}
				
				}else{
					// if the option in optSd is a Put then we can use it and we will create
					//  a call shortName
					ShortNameInfo sni = new ShortNameInfo(
							optSd.getSymbol(),optSd.getSymbolType(), 
							optSd.getExchange(), 
							optSd.getCurrency(), 
							optSd.getContractYear(), optSd.getContractMonth(),null, "C", strike);
					String callSn = sni.getShortName();
					BigDecimal vol = itmAndOtmOptImpliedVolMap.get(optSn);
					ret.put(optSn, vol);
					ret.put(callSn, vol);
					
				}
			}else{
				// ************ CASE 2  the settle less than or equal to the strike
				// use call, b/c it is Out of the Money
				// First, see if the optSd is a put
				if(!MarketDataComLib.isCall(optSd)){
					// if so, then we need to get the call vol from itmAndOtmOptImpliedVolMap 
					ShortNameInfo sni = new ShortNameInfo(
							optSd.getSymbol(),optSd.getSymbolType(), 
							optSd.getExchange(), 
							optSd.getCurrency(), 
							optSd.getContractYear(), optSd.getContractMonth(),null, "C", strike);
					String callSn = sni.getShortName();
					if(!itmAndOtmOptImpliedVolMap.containsKey(callSn)){
						// just put the call and forget the put
						ret.put(optSn, itmAndOtmOptImpliedVolMap.get(optSn));
					}else{
						// get call vol
						BigDecimal vol = itmAndOtmOptImpliedVolMap.get(callSn);
						// and use it for both put and call
						//*********** !!!!!! here's where you insert both put and call vol
						//   when the settle is greater than the strike
						ret.put(optSn, vol);
						ret.put(callSn, vol);
					}
				
				}else{
					// if the option in optSd is a Call then we can use it and we will create
					//  a put shortName
					ShortNameInfo sni = new ShortNameInfo(
							optSd.getSymbol(),optSd.getSymbolType(), 
							optSd.getExchange(), 
							optSd.getCurrency(), 
							optSd.getContractYear(), optSd.getContractMonth(),null, "P", strike);
					String putSn = sni.getShortName();
					BigDecimal vol = itmAndOtmOptImpliedVolMap.get(optSn);
					ret.put(optSn, vol);
					ret.put(putSn, vol);
					
				}

			}
		}
		return ret;
	}
	
	public static List<String> updateVolsForUnderlyingFutWithNoOptions(
			MongoXml<SettlementDataImmute> mongoSettleDb,
			MongoXml<BigDecimal> impliedVolDb){
		List<String> rejects = new ArrayList<String>();
		String futRegex = "\\.FUT\\.";
		Map<String, SettlementDataImmute> settleMap = 
				mongoSettleDb.getByRegex(futRegex);
		Map<String, BigDecimal> impVolMap = 
				impliedVolDb.getByRegex(futRegex);
		if(settleMap==null)return null;
		if(impVolMap==null)return null;
		String sep = MarketDataComLib.DEFAULT_SHORTNAME_SEPARATOR;
		TreeSet<String> settleKeys = new TreeSet<String>(settleMap.keySet());
		List<String> orderedKeys = new ArrayList<String>(settleKeys);
		Map<String, BigDecimal> volsToUpdate = 
				new HashMap<String, BigDecimal>();
		for(int i = 0;i<orderedKeys.size();i++){
			String key = orderedKeys.get(i);
			// is this in impliedVolDb
			Map<String,BigDecimal> value = impliedVolDb.read(key);
			if(value.size()<1){
				// if you come here, you need to create an implied vol
				//   for this futures contract;
				// see if next several keys can fulfill the vol
				String[] partials = key.split("\\"+sep);
				String partial = partials[0]+sep+partials[1]+sep+partials[2];
				for(int j = i+1;
						j<(i+3>=orderedKeys.size()?orderedKeys.size():i+3);
						j++){
					String nextKey = orderedKeys.get(j);
					String[] partialsNext = nextKey.split("\\"+sep);
					String partialNext = partialsNext[0]+sep+partialsNext[1]+sep+partialsNext[2];
					if(partialNext.compareTo(partial)==0){
						// try getting this vol
						Map<String, BigDecimal> volMap = 
								impliedVolDb.read(nextKey);
						if(volMap.size()>0){
							// update ret
							volsToUpdate.put(key, volMap.get(nextKey));
						}
					}
				}
				if(!volsToUpdate.containsKey(key)){
					rejects.add(key);
				}
			}
		}
		// try and do updates
		impliedVolDb.multiUpsert(volsToUpdate);
		Utils.prtObMess(CreateMongoSettleImpliedVolDatabase.class,"Updating "+MongoDatabaseNames.IMPLIEDVOL_CL);
		CollectionsStaticMethods.prtMapItems(volsToUpdate);
		return rejects;
	}

}
