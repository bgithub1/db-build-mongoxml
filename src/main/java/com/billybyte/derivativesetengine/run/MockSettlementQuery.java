package com.billybyte.derivativesetengine.run;



//import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.billybyte.clientserver.ServiceBlock;
import com.billybyte.clientserver.webserver.WebServiceComLib;
import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
//import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.queries.ComplexQueryResult;
import com.thoughtworks.xstream.XStream;

public class MockSettlementQuery implements QueryInterface<String , ComplexQueryResult<SettlementDataInterface>>{
//	private final Calendar currTime = Calendar.getInstance();
//	private final long yyyyMmDd  = Dates.getYyyyMmDdFromCalendar(currTime);
	private final static String settlePort = "9500";
	private final QueryInterface<String, SettlementDataInterface> settleQuery ;
	private final Map<String,SettlementDataInterface> cacheMap;
	public MockSettlementQuery(){
		ServiceBlock sb = new ServiceBlock(","+settlePort+",http://127.0.0.1,settleDirectQuery");
		settleQuery = WebServiceComLib.getQueryService(sb, new XStream());
		this.cacheMap = new HashMap<String,SettlementDataInterface>();

	}
	
	@Override
	public ComplexQueryResult<SettlementDataInterface> get(String key,
			int timeoutValue, TimeUnit timeUnitType) {
		if(cacheMap.containsKey(key))return new ComplexQueryResult<SettlementDataInterface>(null, cacheMap.get(key));
		SettlementDataInterface settle = settleQuery.get(key, timeoutValue, timeUnitType);
		if(settle==null){
			return errRet(key+" settle not found");
		}
		cacheMap.put(key, settle);
		return new ComplexQueryResult<SettlementDataInterface>(null, settle);
	}

	private ComplexQueryResult<SettlementDataInterface> errRet(String s){
		Exception e = Utils.IllState(this.getClass(),s);
		return new ComplexQueryResult<SettlementDataInterface>(e, null);
	}

}
