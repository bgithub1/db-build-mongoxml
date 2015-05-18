package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;

public class DseRunUtils {
	static String getStringParam(Map<String, String> argPairs,String param){
		String ret = argPairs.get(param);
		Utils.prtObMess(RunCsoImpliedCorrelationGenerator.class, param+"="+ret);
		return ret;
	}
	
	static Boolean getBooleanParam(Map<String, String> argPairs,String param,Boolean defaultValue){
		String stringParam = getStringParam(argPairs,param);
		return stringParam==null ? defaultValue : new Boolean(stringParam);
	}
	
	static Integer getIntegerParam(Map<String, String> argPairs,String param,Integer defaultValue){
		String stringParam = getStringParam(argPairs,param);
		return stringParam==null ? defaultValue : new Integer(stringParam);
	}

	static BigDecimal getBigDecimalParam(Map<String, String> argPairs,String param,BigDecimal defaultValue){
		String stringParam = getStringParam(argPairs,param);
		return stringParam==null ? defaultValue : new BigDecimal(stringParam);
	}

	static TimeUnit getTimeUnitParam(Map<String, String> argPairs,String param,TimeUnit defaultValue){
		String stringParam = getStringParam(argPairs,param);
		return stringParam==null ? defaultValue : TimeUnit.valueOf(stringParam);
	}
	
	static Class<?> getClassParam(Map<String, String> argPairs,String param){
		String stringParam = getStringParam(argPairs,param);
		Class<?> classInPackageWhereFileIsLocated = null;
		try {
			if(stringParam!=null){
				classInPackageWhereFileIsLocated =
						Class.forName(stringParam) ;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return classInPackageWhereFileIsLocated;
	}
	
	static Map<String, Object> getBeans(Map<String, String> argPairs){
		String dseXmlName = getStringParam(argPairs,"dseXmlName");
		return Utils.springGetAllBeans(dseXmlName);
	}
	
	static DerivativeSetEngine getDse(Map<String, Object> beansMap ){
		// get stuff for building a derivativesetengine from spring beans
		String beanName = "dse";
		DerivativeSetEngine de = (DerivativeSetEngine)beansMap.get(beanName);
		if(de==null){
			throw Utils.IllState(RunCsoImpliedCorrelationGenerator.class, "can't build dse");
		}
		return de;
	}
	

	
	static MongoXml<BigDecimal> getMongoImpliedVolDb(Map<String, String> argPairs){
		
		String mongoHost = getStringParam(argPairs,"mongoHost");
		if(mongoHost==null){
			mongoHost=MongoDatabaseNames.DEFAULT_HOST ;
		}
		String mongoPortString  = getStringParam(argPairs,"mongoPort");
		Integer mongoPort =null;
		if(mongoPortString==null){
			mongoPort = MongoDatabaseNames.DEFAULT_PORT;
		}else{
			mongoPort = new Integer(mongoPortString);
		}
		MongoXml<BigDecimal> mongoImpliedVolDb = 
				new MongoXml<BigDecimal>(
						mongoHost, 
						mongoPort, 
						MongoDatabaseNames.IMPLIEDVOL_DB, 
						MongoDatabaseNames.IMPLIEDVOL_CL);

		return mongoImpliedVolDb;
	}

	
	
	/**
	 * Copy database collection elements that you obtain from
	 *   the sourceHostIp:sourcePort using a regex lookup on the regex key
	 *   regexMatch, to the destinationHostIp:destinationPort.
	 *   Also, replace the originalString with the replaceString in
	 *    the key.
	 *    
	 * @param databaseName
	 * @param collectionName
	 * @param sourceHostIp
	 * @param sourcePort
	 * @param destinationHostIp
	 * @param destinationPort
	 * @param regexMatch
	 * @param originalString - a string found in the MongoXml database key
	 * @param replaceString - a string that will replace originalString
	 * 
	 */
	public static <T> void copyAndReplaceKey(
			String databaseName,
			String collectionName,
			String sourceHostIp, 
			Integer sourcePort,
			String destinationHostIp, 
			Integer destinationPort,
			String regexMatch, 
			String originalString, 
			String replaceString){
		MongoXml<T> sourceMongo = 
				new MongoXml<T>(sourceHostIp, sourcePort, databaseName, collectionName);
		MongoXml<T> destinationMongo = 
				new MongoXml<T>(destinationHostIp, destinationPort, databaseName, collectionName);
		copyAndReplaceKey(sourceMongo, destinationMongo, regexMatch, originalString, replaceString);
	}

	
	public static <T> void copyAndReplaceKey(
			MongoXml<T> sourceMongo, MongoXml<T> destMongo, 
			String regexMatch, String originalString, String replaceString) {
		Map<String,T> sourceMap = sourceMongo.getByRegex(regexMatch);
		for(Entry<String,T> entry:sourceMap.entrySet()){
			String key = entry.getKey();
			T value = entry.getValue();
			String newKey = key.replace(originalString, replaceString);
			destMongo.upsert(newKey, value);
		}
		Utils.prt(sourceMap.size()+" db items copied and replaced - "+Calendar.getInstance().getTime().toString());

	}

	/**
	 * 
	 * @param source MongoXml<T>
	 * @param regexExpressionForSourceData - regex expression to get data
	 * @param destination MongoXml<T>
	 */
	public static <T> void copyMongo(
			MongoXml<T> source, 
			String regexExpressionForSourceData,
			MongoXml<T> destination){
		destination.multiUpsert(source.getByRegex(regexExpressionForSourceData));
	}
	
	

	
	public static <T> void copyMongo(
			String mongoDbName,
			String mongoCollectionName,
			Class<T> classOfData,
			String sourceHostIp,
			Integer sourcePort,
			String destinationHostIp,
			Integer destinationPort,
			String regexExpressionForSourceData,
			boolean skipWarningPrint){

		if(sourceHostIp==null || sourceHostIp.trim().compareTo("    ")<=0){
			throw Utils.IllState(DseRunUtils.class," sourceHostIp: " +   sourceHostIp + " is invalid");
		}
		if(sourcePort==null ){
			throw Utils.IllState(DseRunUtils.class," sourcePort: " +   " is null");
		}
		if(destinationHostIp==null || destinationHostIp.trim().compareTo("    ")<=0){
			throw Utils.IllState(DseRunUtils.class," destinationHostIp: " +   destinationHostIp + " is invalid");
		}
		if(destinationPort==null ){
			throw Utils.IllState(DseRunUtils.class," destinationPort: " +   " is null");
		}
		
		if(regexExpressionForSourceData==null || regexExpressionForSourceData.trim().compareTo("    ")<=0){
			throw Utils.IllState(DseRunUtils.class," regex string: " +  regexExpressionForSourceData + " is invalid");
		}

		MongoXml<T> source = 
				new MongoXml<T>(sourceHostIp,
						sourcePort, mongoDbName, 
						mongoCollectionName);
		MongoXml<T> destination = 
				new MongoXml<T>(destinationHostIp,
						destinationPort, mongoDbName, 
						mongoCollectionName);
		CollectionsStaticMethods.prtMapItems(source.getByRegex(regexExpressionForSourceData));
		Utils.prt("Copying from : " + sourceHostIp + ":" + sourcePort);
		Utils.prt("          to : " + destinationHostIp + ":" + destinationPort);
		boolean doCopy = true;
		if(!skipWarningPrint){
			String tf = MessageBox.MessageBoxNoChoices("Enter true to continue copy","false");
			if(tf.compareTo("true")!=0){
				doCopy=false;
			}
		}
		if(doCopy){
			copyMongo(source, regexExpressionForSourceData, destination);
		}
		
		System.exit(0);

	}
	
		
}
