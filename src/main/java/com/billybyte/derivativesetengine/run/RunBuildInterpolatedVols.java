package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.mongo.MongoDatabaseNames;

public class RunBuildInterpolatedVols {
	private static final class InterpVolBuilder extends AbstractBuildInterpolatedValues<BigDecimal>{
		public InterpVolBuilder(String mongoIp, Integer mongoPort,
				String snXmlListDataPath) {
			super(mongoIp, mongoPort, MongoDatabaseNames.IMPLIEDVOL_DB, MongoDatabaseNames.IMPLIEDVOL_CL, snXmlListDataPath);
		}

		@Override
		public BigDecimal getBigDecimalValueFromT(BigDecimal t) {
			return t;
		}

		@Override
		public BigDecimal newT(String shortName,
				BigDecimal newValue) {
			
			return newValue;
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
		InterpVolBuilder builder = 
				new InterpVolBuilder(mongoIp, mongoPort, snXmlListDataPath);
		builder.process();
	}	
}
