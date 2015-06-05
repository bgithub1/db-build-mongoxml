package com.billybyte.derivativesetengine.run;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.billybyte.clientserver.ServiceBlock;
import com.billybyte.clientserver.webserver.WebServiceComLib;
import com.billybyte.clientserver.webserver.WebServiceServer;
import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.InBlk;
import com.billybyte.queries.ComplexQueryResult;

public class RunDseInBlkWebService  {
	
	private static final class LocalQuery implements QueryInterface<Set<String>, Map<String, ComplexQueryResult<InBlk>>>{
		private final DerivativeSetEngine dse;
		private LocalQuery(DerivativeSetEngine dse){
			this.dse=dse;
		}
		@Override
		public Map<String, ComplexQueryResult<InBlk>> get(Set<String> key,
				int timeoutValue, TimeUnit timeUnitType) {
			return dse.getInputs(key);
		}
		
	}
	
	
	public static void main(String[] args) {
		
		Map<String, String> argPairs = 
				Utils.getArgPairsSeparatedByChar(args, "=");
		Map<String,Object> beansMap = 
				DseRunUtils.getBeans(argPairs);
		// create dse from spring beans file
		DerivativeSetEngine dse =  DseRunUtils.getDse(beansMap);
		LocalQuery inblkQuery = new LocalQuery(dse);
		// create a ServiceBlock with ip,port, service name info
		String urlOfApplicationServer = argPairs.get("urlOfApplicationServer");
		int portOfService = new Integer(argPairs.get("portOfService"));
		String urlOfService = argPairs.get("urlOfService");
		String nameOfService = argPairs.get("nameOfService");
		
		// create service block for service
		ServiceBlock sb = new ServiceBlock(
				urlOfApplicationServer, portOfService, 
				urlOfService, nameOfService);
		
		// start up service
		WebServiceServer<Set<String>,  Map<String, ComplexQueryResult<InBlk>>> wss = 
				WebServiceComLib.startServer(sb, inblkQuery);
		wss.publishService();
	}
	
	
}
