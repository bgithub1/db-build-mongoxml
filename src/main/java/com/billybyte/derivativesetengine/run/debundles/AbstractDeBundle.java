package com.billybyte.derivativesetengine.run.debundles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;

import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.inputs.QueryManager;
import com.billybyte.dse.queries.DseInputQuery;
import com.billybyte.marketdata.SecDef;

public abstract class AbstractDeBundle {
	
	public abstract QueryInterface<String, SecDef> getSecDefQuery();
	public abstract DseInputQuery<SettlementDataInterface> getSettlementQuery();
	public abstract DseInputQuery<BigDecimal> getAtmQuery();
	public abstract DseInputQuery<BigDecimal> getRateQuery();
	public abstract DseInputQuery<BigDecimal> getDivQuery();
	public abstract DseInputQuery<BigDecimal> getVolQuery();
	public abstract DseInputQuery<BigDecimal> getVolSurfaceQuery();
	public abstract DseInputQuery<BigDecimal> getCorrFromSnSetCqrRetQuery();
	public abstract  DseInputQuery<BigDecimal> getImpliedCsoCorrSetQuery() ;
	public abstract QueryInterface<String, List<SecDef>> getBaseUnderlyingSdQuery();
	public abstract QueryInterface<String, List<SecDef>> getCalSwapUnderSdQuery();
	public abstract  QueryInterface<String, List<SecDef>> getCsoUnderSdQuery() ;

	
	
	public abstract QueryManager getQueryManager();
	protected abstract void registerDiotTypes();
	protected abstract void registerUnderlyingQueries();
	protected abstract void registerModels();
	public abstract DerivativeSetEngine getDerivativeSetEngine();
	
}
