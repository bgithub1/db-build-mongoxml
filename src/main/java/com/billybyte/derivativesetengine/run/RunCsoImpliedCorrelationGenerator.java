package com.billybyte.derivativesetengine.run;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.outputs.DerivativeReturn;
import com.billybyte.dse.outputs.ImpliedVolDerSen;
import com.billybyte.dse.queries.SettleDseInputQueryFromMongoRegex;
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
public class RunCsoImpliedCorrelationGenerator {
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
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, " args examples:");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "dseXmlName=myDseBeans ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "ipOfMongo=127.0.0.1 ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "portOfMongo=27017 ");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "showMsgBox=true");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "remove=true");
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, "csoRegexKey=((G[234567])|G((3B)|(4X)|(6B))).FOP");
		//showMsgBox
		Map<String, String> argPairs = 
				Utils.getArgPairsSeparatedByChar(args, "=");
		String dseXmlName = argPairs.get("dseXmlName");
		
		
		// Step 1
		// build dse from Spring beans
		String beanName = "dse";
		Class<DerivativeSetEngine> classOfReturn = DerivativeSetEngine.class;
		DerivativeSetEngine de = Utils.springGetBean(
				classOfReturn, dseXmlName, beanName);
		if(de==null){
			throw Utils.IllState(RunCsoImpliedCorrelationGenerator.class, "can't build dse");
		}
		
		
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
		
		Set<String> keySet = CollectionsStaticMethods.setFromArray(new String[]{csoRegexKey});
		Map<String,ComplexQueryResult<SettlementDataInterface>> allSettles = 
			mongoRegex.get(keySet, 10,TimeUnit.SECONDS );	
		// use model to get implied correlations and then compare them to the old algo below
		Map<String, DerivativeReturn[]> impliedCorrs = 
				de.getSensitivity(new ImpliedVolDerSen(), allSettles.keySet());
		
		String header = 
				"shortName" + "," +	"correlation";
		Utils.prt(header);

		Map<String,BigDecimal> corrMatrixMap = new TreeMap<String, BigDecimal>();
		for(Entry<String,ComplexQueryResult<SettlementDataInterface>> entry:allSettles.entrySet()){
			String derivativeShortName = entry.getKey();
			DerivativeReturn[] drlist = impliedCorrs.get(derivativeShortName);
			if(drlist==null){
				Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class,
						derivativeShortName + ", No correlation directly from model: no drlist");
			}else{
				DerivativeReturn dr = drlist[0];
				if(dr==null){
					Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class,
							derivativeShortName + ", No implied correlation directly from model: drlist[0] is null");					
				}else{
					if(!dr.isValidReturn()){
						Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class,
								derivativeShortName + "," + dr.getException().getMessage());					
					}else{
						BigDecimal  value = new BigDecimal(dr.getValue().doubleValue()).setScale( 8,RoundingMode.HALF_EVEN);
						Utils.prt(derivativeShortName + "," + value.toString());	
						corrMatrixMap.put(derivativeShortName, value);
					}
				}
			}
			
		}
		
		List<String[]> csvData = new ArrayList<String[]>();
		for(Entry<String, BigDecimal> entry:corrMatrixMap.entrySet()){
			String[] line = new String[]{entry.getKey(),entry.getValue().toString()};
			csvData.add(line);
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
		}
		
		System.exit(0);
	}


}
