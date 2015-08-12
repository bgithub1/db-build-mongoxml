package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.mongo.MongoDatabaseNames;

public class RunBuildInterpolatedSettles {
	private static final class InterpSettleBuilder extends AbstractBuildInterpolatedValues<SettlementDataInterface>{
		private final Long yyyyMmDd;
		public InterpSettleBuilder(String mongoIp, Integer mongoPort,
				String snXmlListDataPath,
				Long yyyyMmDd) {
			super(mongoIp, mongoPort, MongoDatabaseNames.SETTLEMENT_DB, MongoDatabaseNames.SETTLEMENT_CL, snXmlListDataPath);
			this.yyyyMmDd = yyyyMmDd;
		}

		@Override
		public BigDecimal getBigDecimalValueFromT(SettlementDataInterface t) {
			return t.getPrice();
		}

		@Override
		public SettlementDataInterface newT(String shortName,
				BigDecimal newValue) {
			
			return new SettlementDataImmute(shortName, newValue, 1, yyyyMmDd);
		}
		
	}
	public static void main(String[] args) {
		Map<String, String> ap = Utils.getArgPairsSeparatedByChar(args, "=");
		String mongoIp = ap.get("mongoIp");
		Integer mongoPort = new Integer(ap.get("mongoPort"));
		//list of lists of futures prices to interpolate
		// do interpolation and write them back
		//
		String snXmlListDataPath = ap.get("shortNameListsPath");
		Long yyyyMmDd = Dates.getYyyyMmDdFromCalendar(Calendar.getInstance());
		InterpSettleBuilder builder = 
				new InterpSettleBuilder(mongoIp, mongoPort, snXmlListDataPath, yyyyMmDd);
		builder.process();
	}	
}
