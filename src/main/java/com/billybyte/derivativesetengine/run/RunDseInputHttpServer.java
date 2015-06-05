package com.billybyte.derivativesetengine.run;

import java.io.IOException;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.billybyte.clientserver.httpserver.HttpCsvQueryServer;
import com.billybyte.commoninterfaces.QueryInterface;

import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.InBlk;

import com.billybyte.queries.ComplexQueryResult;
import com.thoughtworks.xstream.XStream;

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
		String returnXmlString = argPairs.get("returnXml");
		boolean returnXml = false;
		if(returnXmlString!=null){
			returnXml = new Boolean(returnXmlString);
		}
		
		// create csvQuery
		DseCsvQuery csvQuery = new DseCsvQuery(dse,precision,returnXml);
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
		private final boolean returnXml;
		private DseCsvQuery(
				DerivativeSetEngine dse, int precision,
				boolean returnXml){
			this.dse = dse;
			this.precision = precision;
			this.returnXml = returnXml;
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
			
			// should we just return xml?
			if(returnXml){
				XStream xs = new XStream();
				String xmldata = xs.toXML(cqrResults);
				List<String[]> ret = new ArrayList<String[]>();
				String[] s = {xmldata};
				ret.add(new String[]{"xml"});  // add header
				ret.add(s);
				return(ret);
			}else{
				return InBlk.getCsvListFromInBlkMap(dse, cqrResults, precision);
			}
			
			
		}
		
	}
}
