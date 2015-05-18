package com.billybyte.derivativesetengine.run.copy;

import com.billybyte.marketdata.CorrPair;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;

public class RunBatchCopyMongoCorrelation {
	public static void main(String[] args) {
		String sourceIp = args[0];
		Integer sourcePort = new Integer(args[1]);
		String destIp = args[2];
		Integer destPort = new Integer(args[3]);
		boolean remove = args.length>4 ? new Boolean(args[4]) :
			new Boolean(MessageBox.MessageBoxNoChoices("Remove All??", "false"));
		

		MongoXml<CorrPair> source = 
				new MongoXml<CorrPair>(sourceIp, sourcePort,
						MongoDatabaseNames.CORRELATIONS_DB,MongoDatabaseNames.CORRELATIONS_CL );
		MongoXml<CorrPair> dest = 
				new MongoXml<CorrPair>(destIp, destPort,
						MongoDatabaseNames.CORRELATIONS_DB,MongoDatabaseNames.CORRELATIONS_CL );

		if(remove){
			dest.deleteAll();
		}
		source.batchCopy(dest);
		System.exit(0);
	}
}
