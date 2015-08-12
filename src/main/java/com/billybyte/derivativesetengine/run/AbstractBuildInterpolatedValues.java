package com.billybyte.derivativesetengine.run;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.mongo.MongoXml;
import com.billybyte.mongo.QueryFromMongoXml;

public abstract class AbstractBuildInterpolatedValues<T> {
	public abstract BigDecimal getBigDecimalValueFromT(T t);
	public abstract T newT(String shortName,BigDecimal newValue);
	
	private final String mongoIp;
	private final Integer mongoPort;
	private final String mongoDbName;
	private final String mongoCollectionName;
	private final MongoXml<T> mongoXml;
	private final QueryFromMongoXml<T> mongoQuery;
	private final String snXmlListDataPath;
	private final List<List<String>> futuresSeries;
	
	
	
	@SuppressWarnings("unchecked")
	public AbstractBuildInterpolatedValues(String mongoIp, Integer mongoPort,
			String mongoDbName, String mongoCollectionName,
			String snXmlListDataPath) {
		super();
		this.mongoIp = mongoIp;
		this.mongoPort = mongoPort;
		this.mongoDbName = mongoDbName;
		this.mongoCollectionName = mongoCollectionName;
		this.snXmlListDataPath = snXmlListDataPath;
		this.futuresSeries = (List<List<String>>)Utils.getXmlData(List.class, null, snXmlListDataPath) ;
		this.mongoXml = new MongoXml<T>(mongoIp, mongoPort, mongoDbName, mongoCollectionName);
		this.mongoQuery = new QueryFromMongoXml<T>(mongoIp, mongoPort, mongoDbName, 
				mongoCollectionName);
	}
	
	
	public String getMongoIp() {
		return mongoIp;
	}
	public Integer getMongoPort() {
		return mongoPort;
	}
	public String getMongoDbName() {
		return mongoDbName;
	}
	public String getMongoCollectionName() {
		return mongoCollectionName;
	}
	public String getSnXmlListDataPath() {
		return snXmlListDataPath;
	}
	public List<List<String>> getFuturesSeries() {
		return futuresSeries;
	}
	
	
	public void process(){
		
				
		// The outer loop is for each set of instruments that need to be interpolated
		for(int i = 0;i< futuresSeries.size();i++){
			// get the inner series so that you can interpolate settles
			List<String> innerSeries = futuresSeries.get(i);
			// get all the settles that exist for this series
			
			Map<String,T> mapOfSettleCqrs = new HashMap<String, T>();
			for(String sn : innerSeries){
				mapOfSettleCqrs.putAll(mongoQuery.get(
						sn, 
						10, TimeUnit.SECONDS));				
			}
			// create an array of settlements that will initially have either a settle or null
			BigDecimal[] settleList = new BigDecimal[innerSeries.size()];
			// assemble prices in a list sequentially
			for(int j = 0;j<innerSeries.size();j++){
				// get a name
				String sn = innerSeries.get(j);
				// see if there is a settle
				T t = 
						mapOfSettleCqrs.get(sn);
				if(t!=null){
					// good, there is a settle
					settleList[j] = getBigDecimalValueFromT(t);
				}else{
					// bad, you need to interpolate this value
					settleList[j] = null;
				}
			}
			// do interpolations here
			// first initialize the current unit spread 
			BigDecimal currentSingleUnitSpread = settleList[1].subtract(settleList[0]);
			int currentHeadIndex = 0;
			for(int j = 1;j<settleList.length;j++){  // interate j up to the second to last value of the settleList
				if(settleList[j]==null){
					continue;
				}
				BigDecimal value2 = settleList[j];
				BigDecimal value1 = settleList[currentHeadIndex];
				BigDecimal divisor = new BigDecimal(j).subtract(new BigDecimal(currentHeadIndex));
				currentSingleUnitSpread = (value2.subtract(value1)).divide(divisor,RoundingMode.HALF_EVEN);
				for(int k = currentHeadIndex+1;k<=j;k++){
					settleList[k] = settleList[k-1].add(currentSingleUnitSpread);
				}
				currentHeadIndex = j;
			}
			Map<String, T> settleMap = 
					new HashMap<String, T>();
			// print out interpolated values
			for(int j = 0;j<settleList.length;j++){
				String sn = innerSeries.get(j);
				BigDecimal price = settleList[j];
				Utils.prt(innerSeries.get(j)+ " = "+settleList[j].toString());
				settleMap.put(sn, newT(sn,price));
			}
			// write them to mongo
			mongoXml.multiUpsert(settleMap);
		}

	}
	
	
	
	public List<List<String>> createTestValues(){
		String[][] futuresSeriesArray = 
		{
			{
				"CL.FUT.NYMEX.USD.201611", // the first 2 should be in the db for this to work
				"CL.FUT.NYMEX.USD.201612", // because the you always need at least one spread
				"CL.FUT.NYMEX.USD.201701",
				"CL.FUT.NYMEX.USD.201702",
				"CL.FUT.NYMEX.USD.201703",
				"CL.FUT.NYMEX.USD.201704",
				"CL.FUT.NYMEX.USD.201705",
				"CL.FUT.NYMEX.USD.201706",
				"CL.FUT.NYMEX.USD.201707",
				"CL.FUT.NYMEX.USD.201708",
				"CL.FUT.NYMEX.USD.201709",
				"CL.FUT.NYMEX.USD.201710",
				"CL.FUT.NYMEX.USD.201711",
				"CL.FUT.NYMEX.USD.201712",
				"CL.FUT.NYMEX.USD.201801",
				"CL.FUT.NYMEX.USD.201802",
				"CL.FUT.NYMEX.USD.201803",
				"CL.FUT.NYMEX.USD.201804",
				"CL.FUT.NYMEX.USD.201805",
				"CL.FUT.NYMEX.USD.201806",
				"CL.FUT.NYMEX.USD.201807",
				"CL.FUT.NYMEX.USD.201808",
				"CL.FUT.NYMEX.USD.201809",
				"CL.FUT.NYMEX.USD.201810",
				"CL.FUT.NYMEX.USD.201811",
				"CL.FUT.NYMEX.USD.201812",
			}
			
		};
		List<List<String>> futuresSeries = new ArrayList<List<String>>();
		for(int i = 0;i<futuresSeriesArray.length;i++){
			String[] innerArray = futuresSeriesArray[i];
			List<String> innerList = Arrays.asList(innerArray);
			futuresSeries.add(innerList);
		}
		
		return futuresSeries;
	}
	
	public void writeTestSeries(String pathOfXmlToWrite){
		String path = pathOfXmlToWrite;
		if(path==null){
			path = "snListForSettleInterpolation.xml";
		}
		List<List<String>> futuresSeries = createTestValues();
		try {
			Utils.writeToXml(futuresSeries, path);
		} catch (IOException e) {
			throw Utils.IllState(e);
		}


	}

	
}
