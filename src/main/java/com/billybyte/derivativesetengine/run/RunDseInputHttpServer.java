package com.billybyte.derivativesetengine.run;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.billybyte.clientserver.httpserver.HttpCsvQueryServer;
import com.billybyte.commoninterfaces.QueryInterface;

import com.billybyte.commonstaticmethods.RegexMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeModelInterface;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.InBlk;

import com.billybyte.dse.inputs.diotypes.DioType;
import com.billybyte.marketdata.SecDef;
import com.billybyte.queries.ComplexQueryResult;

public class RunDseInputHttpServer {
	private static final int DEFAULT_PRECISION = 5;
	/**
	 * 
	 * @param args  "dseXmlName=beans_DseFromMongoBasedQm_EvalToday.xml" httpPort=8989 httpPath=dseinblk
	 */
	public static void main(String[] args) {
		
		Map<String, String> argPairs = 
				Utils.getArgPairsSeparatedByChar(args, "=");
		Map<String,Object> beansMap = 
				DseRunUtils.getBeans(argPairs);
		// create dse from spring beans file
		DerivativeSetEngine dse =  DseRunUtils.getDse(beansMap);
		 
		
		int httpPort = new Integer(argPairs.get("httpPort"));
		String httpPath = "/"+argPairs.get("httpPath");
		String precisionString = argPairs.get("precision");
		int precision = precisionString==null ? DEFAULT_PRECISION : new Integer(precisionString);
		
		// create csvQuery
		DseCsvQuery csvQuery = new DseCsvQuery(dse,precision);
		HttpCsvQueryServer httpserver = null;
		try {
			httpserver = 
					new HttpCsvQueryServer(httpPort, httpPath, csvQuery, 10, TimeUnit.SECONDS);
		} catch (IOException e) {
			throw Utils.IllState(e);
		}
		httpserver.start();
		Utils.prtObMess(RunDseInputHttpServer.class, "server started on port: "+httpPort);
		Utils.prtObMess(RunDseInputHttpServer.class, "Enter in address bar of browser: ");
		Utils.prtObMess(RunDseInputHttpServer.class,"http://127.0.0.1:"+httpPort+ httpPath+"?p1=CL.FUT.NYMEX.USD.201512&p2=LO.FOP.NYMEX.USD.201512.C.60.00&p3=LO.FOP.NYMEX.USD.201512.P.60.00");
		Utils.prtObMess(RunDseInputHttpServer.class,"or use another url with same port");
	}
	
	private static class DseCsvQuery implements QueryInterface<String , List<String[]>>{
		private final DerivativeSetEngine dse;
		private final int precision;
		private DseCsvQuery(DerivativeSetEngine dse, int precision){
			this.dse = dse;
			this.precision = precision;
		}
		@Override
		public List<String[]> get(String key, int timeoutValue,
				TimeUnit timeUnitType) {
			String[] tokenPairs = key.split("&");
			Set<String> keyset = new HashSet<String>();
			for(String tokenPair:tokenPairs){
				String[] sns = tokenPair.split("=");	
				keyset.add(sns[1]);  // right side of = is the shortName
			}
			
			// get inputs from dse
			Map<String, ComplexQueryResult<InBlk>> cqrResults = 
					dse.getInputs(keyset);
			
			// create the return object
			List<String[]> ret = new ArrayList<String[]>();
			
			// create a map that maps shortName to the values per that shortName
			Map<String,Map<String, String>> snVsTypeVsValue = new HashMap<String, Map<String,String>>();
			// create a TreeSet of all possible DioTypes used as inputs.  You'll use to create csv columns later on
			Set<String> allPossibleTypes = new TreeSet<String>();
			
			// iterate through shortNames
			for(String sn : keyset){
				if(!cqrResults.containsKey(sn)){
					String[] line = {sn,"no cqr returned from dse"};
					ret.add(line);
					continue;
				}
				
				// get the InBlk for this shortName
				ComplexQueryResult<InBlk> cqr = 
						cqrResults.get(sn);
				if(!cqr.isValidResult()){
					String[] line = {sn,cqr.getException().getMessage()};
					ret.add(line);
					continue;
				}
				InBlk inblk = cqr.getResult();

				// get the model per the shortName
				DerivativeModelInterface optionsModel = dse.getModel(sn);
				
				// create a map of DioTypes vs values per that type.  This map will be put into the snVsTypeVsValue map for each shortName
				Map<String, String> typeVsValue = new HashMap<String, String>();
				
				// get main DioTypes and their values
				List<DioType<?>> mainTypes = optionsModel.getMainInputTypes();
				for(DioType<?> diot: mainTypes){
					String diotName = diot.name();

					Object o = diot.getMainInputs(inblk);
					// some DioTypes have multiple field outputs
					String[] multiPart = o.toString().split(",");
					if(multiPart.length>1){
						for(int j = 0;j<multiPart.length;j++){
							String typeKey = diotName + "_" + j;
							String value = multiPart[j];
							typeVsValue.put(typeKey,value);
							allPossibleTypes.add(typeKey);
						}
					}else{
						typeVsValue.put(diotName, multiPart[0]);
						allPossibleTypes.add(diotName);
					}

				}
				
				// get underlying DioTypes and their values
				List<DioType<?>> underTypes = optionsModel.getUnderlyingInputTypes();
				for(DioType<?> diot: underTypes){
					// you can have multiple underlying shortNames for each optionable security (e.g spread options)
					List<?> l = diot.getUnderlyingInputs(inblk);
					String diotName = diot.name();
					for(int i = 0;i<l.size();i++){
						String typeKey = diotName + "_" + i;
						String value = l.get(i).toString();
						typeVsValue.put(typeKey,value);
						allPossibleTypes.add(typeKey);
					}
				}
				
				SecDef[] underlyingSecDefs = inblk.getUnderlyingSds();
				for(int i = 0;i < underlyingSecDefs.length; i++){
					String typeKey = "Underlying_" + i;
					String value = underlyingSecDefs[i].getShortName();
					// create columns for each underlying shortName
					typeVsValue.put(typeKey, value);
					allPossibleTypes.add(typeKey);
				}
				
				// save all inputs for this shortName
				snVsTypeVsValue.put(sn,typeVsValue);
			}

			List<String> colNames = new ArrayList<String>();
			colNames.add("shortName");
			for(String diotName : allPossibleTypes ){
				colNames.add(diotName);
			}
			ret.add(colNames.toArray(new String[]{}));
			
			// now add lines for each shortname
			for(String sn : keyset){
				Map<String,String> valuesForThisShortName = 
						snVsTypeVsValue.get(sn);
				String[] line = new String[allPossibleTypes.size()+1]; // plus 1 for the shortname
				line[0] = sn;
				for(int j = 1;j<colNames.size();j++){
					String colName = colNames.get(j);
					String value = valuesForThisShortName.get(colName);
					// if value is a number, then round it
					if(RegexMethods.isNumber(value)){
						value = new BigDecimal(value,
								new MathContext(precision, RoundingMode.HALF_EVEN)).toString();
					}
					line[j] = value;
				}
				ret.add(line);
			}
			return ret;
		}
		
	}
}
