package com.billybyte.derivativesetengine.run;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.billybyte.commoninterfaces.QueryInterface;

public class QueryOfSetFromSingleQuery <K,V> implements QueryInterface<Set<K>, Map<K,V>>{
	private final QueryInterface<K, V> singleQuery;
	public QueryOfSetFromSingleQuery(QueryInterface<K, V> singleQuery) {
		super();
		this.singleQuery = singleQuery;
	}

	@Override
	public Map<K, V> get(Set<K> keySet, int timeoutValue, TimeUnit timeUnitType) {
		Map<K,V> ret = new HashMap<K, V>();
		for(K key:keySet){
			V v = singleQuery.get(key, timeoutValue, timeUnitType);
			ret.put(key, v);
		}
		return ret;
	}
}
