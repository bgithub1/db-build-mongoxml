package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.util.Map;

import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.mongo.MongoDatabaseNames;

public class RunModifyVolsWithJanino {
	private static class JaninoVolModifier extends AbstractJaninoModifyValues<BigDecimal>{

		public JaninoVolModifier(String mongoIp, Integer mongoPort,
				String regexSnAndExpressionCsvPath) {
			super(mongoIp, mongoPort, MongoDatabaseNames.IMPLIEDVOL_DB, MongoDatabaseNames.IMPLIEDVOL_CL,
					regexSnAndExpressionCsvPath);
		}

		@Override
		public BigDecimal getBigDecimalValueFromT(
				BigDecimal t) {
			return t;
		}

		@Override
		public BigDecimal newT(
				String shortName, BigDecimal newValue) {
			return newValue;
		}
		
	}
	
	
	public static void main(String[] args) {
		Map<String, String> ap = Utils.getArgPairsSeparatedByChar(args, "=");
		String mongoIp = ap.get("mongoIp");
		Integer mongoPort = new Integer(ap.get("mongoPort"));
		String regexSnAndExpressionCsvPath = ap.get("regexSnAndExpressionCsvPath");
		JaninoVolModifier janinoVolModifier =
				new JaninoVolModifier(mongoIp, mongoPort, regexSnAndExpressionCsvPath);
		janinoVolModifier.process();
	}
}
