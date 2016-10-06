package jp.riken.clustering.misc;

import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DefaultSuppliers {

	private DefaultSuppliers() {
		
	};

	public static <T> Supplier<List<T>> list() {
		return new Supplier<List<T>>() {
			public List<T> get() {
				return Lists.newArrayList();
			}
		};
	}

	public static <T> Supplier<Set<T>> set() {
		return new Supplier<Set<T>>() {
			public Set<T> get() {
				return Sets.newHashSet();
			}
		};
	}
}
