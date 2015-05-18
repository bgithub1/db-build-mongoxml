package com.billybyte.derivativesetengine.run;

import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.mongo.MongoDatabaseNames;


public class RunCopySettleAndVolForAssociatedProducts {

	public static void main(String[] args) {
		int i = -1;
		
		i+=1;
		String sourceHostIp = args!=null && args.length>i ? args[i] : MongoDatabaseNames.DEFAULT_HOST;
		i+=1;
		Integer sourcePort = args!=null && args.length>i ? new Integer( args[i]) : MongoDatabaseNames.DEFAULT_PORT;
		i+=1;
		String destinationHostIp = args!=null && args.length>i ? args[i] : MongoDatabaseNames.DEFAULT_HOST;
		i+=1;
		Integer destinationPort = args!=null && args.length>i ? new Integer( args[i]) : MongoDatabaseNames.DEFAULT_PORT;

		String settleDbName = MongoDatabaseNames.SETTLEMENT_DB;
		String settleCollName = MongoDatabaseNames.SETTLEMENT_CL;
		String volDbName = MongoDatabaseNames.IMPLIEDVOL_DB;
		String volCollName = MongoDatabaseNames.IMPLIEDVOL_CL;
		

		// do futures stuff
		String regexKey = "^CL\\.FUT\\.NYMEX";
		String originalString = "CL.FUT.NYMEX";
		String replaceString = "WTI.FUT.ICE";
		Utils.prtObMess(RunCopySettleAndVolForAssociatedProducts.class,"copying from: " + sourceHostIp + ":" + sourcePort + " - " + settleDbName + " : " + settleCollName);
		Utils.prtObMess(RunCopySettleAndVolForAssociatedProducts.class,"copying from: " + sourceHostIp + ":" + sourcePort + " - " + volDbName + " : " + volCollName);


		DseRunUtils.copyAndReplaceKey(settleDbName, settleCollName, 
				sourceHostIp, sourcePort, 
				destinationHostIp, destinationPort, 
				regexKey, originalString, replaceString);
		DseRunUtils.copyAndReplaceKey(volDbName, volCollName, 
				sourceHostIp, sourcePort, 
				destinationHostIp, destinationPort, 
				regexKey, originalString, replaceString);

		

		String optRegexKey = "^LO\\.FOP\\.NYMEX";
		String optOriginalString = "LO.FOP.NYMEX";
		String optReplaceString = "WTI.FOP.ICE";
		
		DseRunUtils.copyAndReplaceKey(settleDbName, settleCollName, 
				sourceHostIp, sourcePort, 
				destinationHostIp, destinationPort, 
				optRegexKey, optOriginalString, replaceString);
		DseRunUtils.copyAndReplaceKey(volDbName, volCollName, 
				sourceHostIp, sourcePort, 
				destinationHostIp, destinationPort, 
				optRegexKey, optOriginalString, optReplaceString);

		Utils.prtObMess(RunCopySettleAndVolForAssociatedProducts.class,"complete");
		System.exit(0);
		
	}
}
