package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.billybyte.commonstaticmethods.JaninoStuff;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.mongo.MongoXml;
import com.billybyte.mongo.QueryFromMongoXml;
/**
 * From a list of csv lines, in which the first column is a regex expression of
 *   shortNames to get, and the next column is an expression of the form:
 *   		value * 100
 *   where value will be replaced by the BigDecimal value (e.g the price field of the
 *    MongoXml records in settleDb, or the big-decimal field in the MongoXml records in 
 *      ImpliedVolDb.
 *    The word "value" will be replace by a double value from a the MongoXml record, 
 *      and then Janino will evalute the expression.  The new value will replace the
 *      old value in the MongoXml records for those shortNames that match the regex expression.
 *      
 *    Each line has a different regex/expression pair of coummns.
 *         
 * @author bperlman1
 *
 * @param <T>
 */
public abstract class AbstractJaninoModifyValues<T> {
	public abstract BigDecimal getBigDecimalValueFromT(T t);
	public abstract T newT(String shortName,BigDecimal newValue);
	
	private final String mongoIp;
	private final Integer mongoPort;
	private final String mongoDbName;
	private final String mongoCollectionName;
	private final MongoXml<T> mongoXml;
	private final List<String[]> regexSnAndExpressionCsvLines;
	
	
	
	public AbstractJaninoModifyValues(String mongoIp, Integer mongoPort,
			String mongoDbName, String mongoCollectionName,
			String regexSnAndExpressionCsvPath) {
		super();
		this.mongoIp = mongoIp;
		this.mongoPort = mongoPort;
		this.mongoDbName = mongoDbName;
		this.mongoCollectionName = mongoCollectionName;
		this.mongoXml = new MongoXml<T>(mongoIp, mongoPort, mongoDbName, mongoCollectionName);
		this.regexSnAndExpressionCsvLines = Utils.getCSVData(regexSnAndExpressionCsvPath);
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
	
	
	public void process(){
		for(String[] line : regexSnAndExpressionCsvLines){
			String regexSnExpression = line[0];
			
			Map<String,T> mapOfSettleCqrs  = mongoXml.getByRegex(regexSnExpression);
			Map<String, T> replacementMap = new HashMap<String, T>();
			String expression = line[1];
			for(String sn : mapOfSettleCqrs.keySet()){
				BigDecimal value = getBigDecimalValueFromT(mapOfSettleCqrs.get(sn));
				int precision = value.toString().split("\\.")[1].length();
				String replaceExpression = expression.replace("value", value.toString());
				Double newValue = JaninoStuff.getDoubleFromExpression(replaceExpression);
				BigDecimal newBdValue = new BigDecimal(newValue).setScale(precision, RoundingMode.HALF_EVEN);
				T t = newT(sn,newBdValue);
				replacementMap.put(sn,t);
			}
			
			mongoXml.multiUpsert(replacementMap);
		}

	}
	
	
}
