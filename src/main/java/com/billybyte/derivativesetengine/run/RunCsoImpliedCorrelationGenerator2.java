package com.billybyte.derivativesetengine.run;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.InBlk;
import com.billybyte.dse.inputs.diotypes.AtmDiot;
import com.billybyte.dse.inputs.diotypes.DioType;
import com.billybyte.dse.inputs.diotypes.DteFromSettleDiot;
import com.billybyte.dse.inputs.diotypes.RateDiot;
import com.billybyte.dse.inputs.diotypes.SettlePriceDiot;
import com.billybyte.dse.inputs.diotypes.VolDiot;
import com.billybyte.dse.models.spread.BinarySearchImpliedCorrelation;
import com.billybyte.dse.models.spread.CsoModel;
import com.billybyte.dse.outputs.DerivativeReturn;
import com.billybyte.dse.outputs.ImpliedVolDerSen;
import com.billybyte.dse.queries.SettleDseInputQueryFromMongoRegex;
import com.billybyte.marketdata.SecDef;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.queries.ComplexQueryResult;
import com.billybyte.ui.messagerboxes.MessageBox;


/**
 * Create Implied correlations for CSOS.
 * 
 * @author bperlman1
 *
 */
public class RunCsoImpliedCorrelationGenerator2 {
//	private static final String OUTPUTCSV_PATH =  "../PortfolioData/CorrelationData/CommoditiesCorrelations/impliedCSOCorrelationsMap.csv";

	/**
	 * 
	 * @param args
		"dseXmlName=beans_DseFromMongoBasedQm_EvalToday.xmlbeans_DseFromMongoBasedQm_EvalToday.xmlbeans_DseFromMongoBasedQm_EvalToday.xml"  
		"ipOfMongo=127.0.0.1" 
		"portOfMongo=27017" 
		"showMsgBox=true" 
		"remove=true" 
		"csoRegexKey=((G[234567])|G((3B)|(4X)|(6B))).FOP"
		
		Pre load settlement query with CSO settles,
		and iterate thru them.
		Use a REGEX based settlement query to get all CSO settles.

	 *
	 */
	public static void main(String[] args) {
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, " args examples:");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "dseXmlName=myDseBeans ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "ipOfMongo=127.0.0.1 ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "portOfMongo=27017 ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "showMsgBox=true");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "remove=true");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class, "csoRegexKey=((G[234567])|G((3B)|(4X)|(6B))).FOP");
		//showMsgBox
		Map<String, String> argPairs = 
				Utils.getArgPairsSeparatedByChar(args, "=");
		String dseXmlName = argPairs.get("dseXmlName");
		
		
		// Step 1
		// build dse from Spring beans
		String beanName = "dse";
		Class<DerivativeSetEngine> classOfReturn = DerivativeSetEngine.class;
		DerivativeSetEngine dse = Utils.springGetBean(
				classOfReturn, dseXmlName, beanName);
		if(dse==null){
			throw Utils.IllState(RunCsoImpliedCorrelationGenerator2.class, "can't build dse");
		}
		
		// get de
		DerivativeSetEngine de = dse;//getDe(args[3]);
		
		// get display args
		String showMsgBoxString = argPairs.get("showMsgBox");
		Boolean showMsgBox = showMsgBoxString!=null ?  new Boolean(showMsgBoxString) : true;
		String removeString = argPairs.get("remove");
		Boolean remove = removeString!=null ? new Boolean(removeString) : false;
		//get mongo settlel from regex
		String ipOfMongo = argPairs.get("ipOfMongo");
		Integer portOfMongo  = new Integer(argPairs.get("portOfMongo"));
		String csoRegexKey =  argPairs.get("csoRegexKey");
		
		SettleDseInputQueryFromMongoRegex mongoRegex = 
				new  SettleDseInputQueryFromMongoRegex(ipOfMongo, portOfMongo, csoRegexKey);
		de.getQueryManager().registerDioType(
				new SettlePriceDiot(), mongoRegex);

		// get sdQuery
		QueryInterface<String, SecDef> sdQuery = 
				de.getSdQuery();
		// get initial regex for initial mongo fetch
		Set<String> keySet = CollectionsStaticMethods.setFromArray(new String[]{csoRegexKey});
		Map<String,ComplexQueryResult<SettlementDataInterface>> allSettles = 
			mongoRegex.get(keySet, 10,TimeUnit.SECONDS );	
		
		int timeoutValue=10;
		TimeUnit timeUnitType=TimeUnit.SECONDS;
		
		// create cso model
		CsoModel spreadModel = new CsoModel();
		Set<DioType<?>> types = 
				new HashSet<DioType<?>>(spreadModel.getMainInputTypes());
		types.addAll(new HashSet<DioType<?>>(spreadModel.getUnderlyingInputTypes()));

		// use these types to call the queries in queryManager
		AtmDiot atmDiot = new AtmDiot();
		RateDiot rateDiot = new RateDiot();
		// this type uses volSurf requests, so make sure that a volSurfaceQuery has
		//   been registered in De
//		UnderlingVolsFromVsDiot volDiot = new UnderlingVolsFromVsDiot();
		VolDiot volDiot = new VolDiot();
		DteFromSettleDiot dteFromSet = new DteFromSettleDiot();
		
		Map<String,ComplexQueryResult<InBlk>> cqrMap = de.getInputs(allSettles.keySet());
		// use model to get implied correlations and then compare them to the old algo below
		Map<String, DerivativeReturn[]> impliedCorrs = 
				de.getSensitivity(new ImpliedVolDerSen(), allSettles.keySet());
		double initialCorrelation = .0;
		// print out all underlyings for diagnostics
		
//		QueryInterface<Set<String>,
//			Map<String,ComplexQueryResult<BigDecimal>>> corrQuery = 
//			de.getQueryManager().getQuery(new CorrPairDiot());
		
		String header = 
				"spreadPriceToAchieve" + "," +
						"precision" + "," + 
						"maxIterations " + "," +
						"spreadModel" + "," +
						"callPut" + "," + 
						"atmLeg0" + "," +
						"atmLeg1" + "," +
						"strike" + "," +
						"dte" + "," +
						"volLeg0" + "," +
						"volLeg1" + "," + 
						"rate0" + "," +
						"divLeg0" + "," +
						"divLeg1" + "," +
						"initialCorrelation" + "," + 
						"other0" + "," +
						"other1";

		Map<String,BigDecimal> corrMatrixMap = new TreeMap<String, BigDecimal>();
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
				header);
		for(Entry<String,ComplexQueryResult<SettlementDataInterface>> entry:allSettles.entrySet()){
			String derivativeShortName = entry.getKey();
			SecDef optionSd = sdQuery.get(derivativeShortName, timeoutValue, timeUnitType);

			if(optionSd==null || optionSd.getStrike()==null){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
						derivativeShortName+" Invalid option without strike");
				continue;
			}
			List<SecDef> underlyingSdList = de.getQueryManager().getUnderlyingSecDefs(derivativeShortName, timeoutValue, timeUnitType);
			Set<String> corrKeySet = new TreeSet<String>();
			for(SecDef sd:underlyingSdList){
				corrKeySet.add(sd.getShortName());
			}
//			Map<String,ComplexQueryResult<BigDecimal>> correlations = 
//					corrQuery.get(corrKeySet, 10, TimeUnit.SECONDS);
//			String pairName = underlyingSdList.get(0).getShortName()+"__"+underlyingSdList.get(1).getShortName();
//			ComplexQueryResult<BigDecimal> cqr = correlations.get(pairName);
			double corr = initialCorrelation;
//			if(cqr.isValidResult()){
//				corr = cqr.getResult().doubleValue();
//			}
			BigDecimal settle = entry.getValue().getResult().getPrice();
			double spreadPriceToAchieve = settle.doubleValue();
			double precision = .00001;
			int maxIterations = 1000;
			double callPut = optionSd.getRight().toUpperCase().compareTo("P")==0?1:0;
			ComplexQueryResult<InBlk> cqrInblk = cqrMap.get(derivativeShortName);
			if(!cqrInblk.isValidResult()){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,derivativeShortName+" No Inblk returned from getInputs");
				continue;
			}
			InBlk inblk = cqrInblk.getResult();

			double[] atms = getFromInBlk(inblk,atmDiot);
			if(atms.length<2 || Double.isNaN(atms[0]) || Double.isNaN(atms[1]) ){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
						derivativeShortName+" No Inblk returned from getInputs");
				continue;
			}
			
			double atmLeg0 = atms[0];
			double atmLeg1 = atms[1];
			
			double strike = optionSd.getStrike().doubleValue();

			double dte = dteFromSet.getMainInputs(inblk).doubleValue();

			double[] vols = getFromInBlk(inblk,volDiot);
			if(vols.length<2 || Double.isNaN(vols[0]) || Double.isNaN(vols[1]) ){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
						derivativeShortName+" No underlying vols returned from getInputs");
				continue;
			}
			double volLeg0 = vols[0];
			double volLeg1 = vols[1];
//			double rates[] = rateDiot.getFromInBlk(inblk);
			double rates[] = getFromInBlk(inblk,rateDiot);
			if(rates.length<2 || Double.isNaN(rates[0]) || Double.isNaN(rates[1]) ){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
						derivativeShortName+" No underlying rates returned from getInputs");
				continue;
			}
			double rate0 = rates[0];
			double rate1 = rates[1];
			double divLeg0 = rates[0];
			double divLeg1 = rates[1];
			
			Object other0 = null;
			Object other1 = null;
			
			BinarySearchImpliedCorrelation bsCorr= new BinarySearchImpliedCorrelation(spreadPriceToAchieve, precision, maxIterations, spreadModel, callPut, atmLeg0, atmLeg1, strike, dte, volLeg0, volLeg1, rate0, divLeg0, divLeg1, other0, other1);
			double impliedCorr =  bsCorr.findByInteration();
//			double impliedCorr = Double.NaN;
//			int tries = 10;
//			for(int k = 1;k<tries;k++){
//				try {
//					impliedCorr = SpreadMultiAbstract.impliedCorrelation(
//							spreadPriceToAchieve, 
//							precision, 
//							maxIterations, 
//							spreadModel,
//							callPut, 
//							atmLeg0, 
//							atmLeg1,
//							strike, 
//							dte,
//							volLeg0,
//							volLeg1, 
//							rate0, 
//							divLeg0, 
//							divLeg1,
//							(corr+k/10>1.0) ? 1.0 : corr+k/10, 
//							other0, 
//							other1);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if(Double.isNaN(impliedCorr) || Double.isInfinite(impliedCorr)){
//					Utils.prt(derivativeShortName+" NaN - trying "+(tries-1-k)+" times");
//				}else{
//					break;
//				}
//				
//			}
				
//			String s = spreadPriceToAchieve +"," +
//					precision +"," +
//					maxIterations +"," +
//					spreadModel +"," +
//					callPut +"," +
//					atmLeg0 +"," +
//					atmLeg1 +"," +
//					strike +"," +
//					dte +"," +
//					volLeg0 +"," +
//					volLeg1 +"," +
//					rate0 +"," +
//					divLeg0 +"," +
//					divLeg1 +"," +
//					initialCorrelation +"," + 
//					other0 +"," +
//					other1	;

			
			if(new Double(impliedCorr).compareTo(Double.NaN)==0){
				continue;
			}

			corrMatrixMap.put(derivativeShortName,new BigDecimal(impliedCorr));
			double price = spreadModel.getSpreadPrice(
					callPut, atmLeg0, atmLeg1, strike, dte, 
					volLeg0, volLeg1, rate0, rate1, divLeg0, divLeg1, 
					impliedCorr, other0, other1);
			Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
					derivativeShortName + "," + impliedCorr + ",UnderlyingCorr," + 
					corr + ",spreadPrice," + spreadPriceToAchieve + ",priceWithImpliedCorr," + price);
			DerivativeReturn[] drlist = impliedCorrs.get(derivativeShortName);
			if(drlist==null){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
						derivativeShortName + ", No correlation directly from model: no drlist");
			}else{
				DerivativeReturn dr = drlist[0];
				if(dr==null){
					Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
							derivativeShortName + ", No implied correlation directly from model: drlist[0] is null");					
				}else{
					if(!dr.isValidReturn()){
						Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
								derivativeShortName + "," + dr.getException().getMessage());					
					}else{
						Utils.prtObMess(RunCsoImpliedCorrelationGenerator2.class,
								derivativeShortName + "," + dr.getValue().toString());					
					}
				}
			}
			
		}
		
		List<String[]> csvData = new ArrayList<String[]>();
		for(Entry<String, BigDecimal> entry:corrMatrixMap.entrySet()){
			String[] line = new String[]{entry.getKey(),entry.getValue().toString()};
			csvData.add(line);
		}

		for(Entry<String, BigDecimal> entry:corrMatrixMap.entrySet()){
			String sn = entry.getKey();
			DerivativeReturn[] drlist = impliedCorrs.get(sn);
			if(drlist==null){
				
			}
		}

		String mbResponse = "true";
		if(showMsgBox){
			mbResponse = MessageBox.MessageBoxNoChoices(new JFrame(),"To save Cso Implied Correlations, enter true", "IMPLIED CSO VOL SAVE", true);	
		}
		
		
		if(mbResponse.trim().toLowerCase().compareTo("true")==0){
			MongoXml<BigDecimal> impliedCsoDb = 
					new MongoXml<BigDecimal>(ipOfMongo, portOfMongo, MongoDatabaseNames.IMPLIED_CORREL_DB, MongoDatabaseNames.IMPLIED_CORREL_CL);
			if(remove){
				impliedCsoDb.deleteAll();
			}
			impliedCsoDb.removeMultipleEntries(corrMatrixMap.keySet());
			impliedCsoDb.multiUpsert(corrMatrixMap);
//			try {
//				Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class,
//						"Writing CSV Data to Path: "+OUTPUTCSV_PATH);
//				Utils.writeCleanCSVData(csvData, OUTPUTCSV_PATH);
//				MongoXml<BigDecimal> impliedCsoDb = 
//						new MongoXml<BigDecimal>(ipOfMongo, portOfMongo, MongoDatabaseNames.IMPLIED_CORREL_DB, MongoDatabaseNames.IMPLIED_CORREL_CL);
//				impliedCsoDb.removeMultipleEntries(corrMatrixMap.keySet());
//				impliedCsoDb.multiUpsert(corrMatrixMap);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
		System.exit(0);
	}

//	/**
//	 * 
//	 * @param deBundleMapXmlPath path of xml file with urls to all WsPorts of 
//	 *    services for DeBundle queries.
//	 * @return
//	 */
//	static DerivativeSetEngine getDe(String deBundleMapXmlPath){
//		DeBundleFromSpring deb = new DeBundleFromSpring("deBundleQueries");
//		DerivativeSetEngine de = deb.getDerivativeSetEngine();
//
//		return de;
//	}
	
	static <T> double[] getFromInBlk(InBlk inblk,DioType<T> diot){
		List<T> tlist = diot.getUnderlyingInputs(inblk);
		double[] ret = new double[tlist.size()];
		for(int j=0;j<tlist.size();j++){
			ret[j] = new Double(tlist.get(j).toString());
		}
		return ret;
	}


}
