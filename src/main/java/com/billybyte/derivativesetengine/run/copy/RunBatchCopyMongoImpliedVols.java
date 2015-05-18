package com.billybyte.derivativesetengine.run.copy;

import java.math.BigDecimal;


import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;

public class RunBatchCopyMongoImpliedVols {
	public static void main(String[] args) {
		String sourceIp = args[0];
		Integer sourcePort = new Integer(args[1]);
		String destIp = args[2];
		Integer destPort = new Integer(args[3]);
		boolean remove = args.length>4 ? new Boolean(args[4]) :
			new Boolean(MessageBox.MessageBoxNoChoices("Remove All??", "false"));
		
		
		MongoXml<BigDecimal> source = 
				new MongoXml<BigDecimal>(sourceIp, sourcePort,
						MongoDatabaseNames.IMPLIEDVOL_DB,MongoDatabaseNames.IMPLIEDVOL_CL );
		MongoXml<BigDecimal> dest = 
				new MongoXml<BigDecimal>(destIp, destPort,
						MongoDatabaseNames.IMPLIEDVOL_DB,MongoDatabaseNames.IMPLIEDVOL_CL );
		if(remove){
			dest.deleteAll();
		}
		source.batchCopy(dest);
		System.exit(0);
	}
}
