package com.billybyte.derivativesetengine.run.copy;

import com.billybyte.derivativesetengine.run.DseRunUtils;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.ui.messagerboxes.MessageBox;

public class RunCopyMongoSettles {
	public static void main(String[] args) {
		String defaultDestinationHostIp = args!=null && args.length>0 ? args[0] : "";
		String exampleRegex = args!=null && args.length>1 ? args[1] : "";
		String sourceIp = MessageBox.MessageBoxNoChoices("Enter source Host IP", MongoDatabaseNames.DEFAULT_HOST);
		String destinationIp = MessageBox.MessageBoxNoChoices("Enter destination Host IP", defaultDestinationHostIp);
		
		String regexExpressionForSourceData = 
				MessageBox.MessageBoxNoChoices(
						"Enter Regex String to get source records to copy", 
						exampleRegex);
		DseRunUtils.copyMongo(
				MongoDatabaseNames.SETTLEMENT_DB, 
				MongoDatabaseNames.SETTLEMENT_CL, 
				SettlementDataImmute.class, sourceIp, 
				27017, destinationIp, 
				27017, regexExpressionForSourceData,
				false);

		
//		int i = -1;
//		i+=1;
//		String mongoIp = args.length>i?args[i]:MongoDatabaseNames.DEFAULT_HOST;
//		i+=1;
//		Integer mongoPort = args.length>i?new Integer(args[i]):MongoDatabaseNames.DEFAULT_PORT;
//		i+=1;
//		String mongoIpDest = args.length>i?args[i]:MongoDatabaseNames.DEFAULT_HOST;
//		i+=1;
//		Integer mongoPortDest = args.length>i?new Integer(args[i]):MongoDatabaseNames.DEFAULT_PORT;
//
//		
//		MongoXml<SettlementDataImmute> source = 
//				new MongoXml<SettlementDataImmute>(
//						mongoIp,
//						mongoPort, 
//						MongoDatabaseNames.SETTLEMENT_DB, 
//						MongoDatabaseNames.SETTLEMENT_CL);
//		MongoXml<SettlementDataImmute> destination = 
//				new MongoXml<SettlementDataImmute>(
//						mongoIpDest,
//						mongoPortDest, 
//						MongoDatabaseNames.SETTLEMENT_DB, 
//						MongoDatabaseNames.SETTLEMENT_CL);
//		while(true){
//			String regexExpressionForSourceData = MessageBox.MessageBoxNoChoices("enter regex string",	"^KC\\.FUT");
//			if(regexExpressionForSourceData==null || regexExpressionForSourceData.length()<1)break;
//			MongoUtils.copyMongo(source, regexExpressionForSourceData, destination);
//		
//		
//		}
	}
}
