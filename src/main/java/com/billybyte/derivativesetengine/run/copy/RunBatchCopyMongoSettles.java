package com.billybyte.derivativesetengine.run.copy;

import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;

public class RunBatchCopyMongoSettles {
	/**
	 * @param args
	 * 0 sourceIp
	 * 1 sourcePort
	 * 2 destIp
	 * 3 destPort
	 * 4 (optional) remove - boolean: true = remove all data from dest before copy
	 * 
	 */
	public static void main(String[] args) {
		
		String sourceIp = args[0];
		Integer sourcePort = new Integer(args[1]);
		String destIp = args[2];
		Integer destPort = new Integer(args[3]);
		boolean remove = args.length>4 ? new Boolean(args[4]) :
			new Boolean(MessageBox.MessageBoxNoChoices("Remove All??", "false"));
		MongoXml<SettlementDataInterface> source = 
				new MongoXml<SettlementDataInterface>(sourceIp, sourcePort,
						MongoDatabaseNames.SETTLEMENT_DB,MongoDatabaseNames.SETTLEMENT_CL );
		MongoXml<SettlementDataInterface> dest = 
				new MongoXml<SettlementDataInterface>(destIp, destPort,
						MongoDatabaseNames.SETTLEMENT_DB,MongoDatabaseNames.SETTLEMENT_CL );
		if(remove){
			dest.deleteAll();
		}
		source.batchCopy(dest);
		System.exit(0);
	}
}
