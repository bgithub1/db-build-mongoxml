package com.billybyte.derivativesetengine.run.copy;

import java.math.BigDecimal;

import com.billybyte.derivativesetengine.run.DseRunUtils;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.ui.messagerboxes.MessageBox;

public class RunCopyMongoImpliedVolsDb {
	
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
				MongoDatabaseNames.IMPLIEDVOL_DB, 
				MongoDatabaseNames.IMPLIEDVOL_CL, 
				BigDecimal.class, sourceIp, 
				27017, destinationIp, 
				27017, regexExpressionForSourceData,
				false);
		
		
	}
}
