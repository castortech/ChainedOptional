package com.castortech.util.functional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Taken from:
 * https://stackoverflow.com/questions/46802484/how-to-chain-lambdas-with-all-optional-values-available-at-the-innermost-scope-w%3E
 * and modified to use a map so that we can assign name to the various intermediate values. Also added use of
 * intermediates for map and orElseGet
 * 
 * @author 	Malte Hartwig (original author)
 * 					Alain Picard
 *
 * @param <T>
 */
public class ChainedOptional<T> {
	private final Map<String, Object> intermediates;
	private final Optional<T> delegate;

	private ChainedOptional(Map<String, Object> previousValues, Optional<T> delegate) {
		intermediates = new LinkedHashMap<>(previousValues);
		String name = "Value" + (intermediates.size() + 1); //$NON-NLS-1$
		intermediates.put(name, delegate.orElse(null));
		this.delegate = delegate;
	}

	private ChainedOptional(Map<String, Object> previousValues, String name, Optional<T> delegate) {
		intermediates = new LinkedHashMap<>(previousValues);
		intermediates.put(name, delegate.orElse(null));
		this.delegate = delegate;
	}

	public static <T> ChainedOptional<T> of(T value) {
		return of(Optional.ofNullable(value));
	}

	public static <T> ChainedOptional<T> of(String name, T value) {
		return of(name, Optional.ofNullable(value));
	}

	public static <T> ChainedOptional<T> of(Optional<T> delegate) {
		return new ChainedOptional<>(new LinkedHashMap<>(), delegate);
	}

	public static <T> ChainedOptional<T> of(String name, Optional<T> delegate) {
		return new ChainedOptional<>(new LinkedHashMap<>(), name, delegate);
	}
	
	public ChainedOptional<T> filter(Predicate<? super T> predicate) {
		return new ChainedOptional<>(intermediates, delegate.filter(predicate));
	}

	public ChainedOptional<T> filter(BiPredicate<Map<String, Object>, ? super T> predicate) {
		return new ChainedOptional<>(intermediates, delegate.filter(value -> predicate.test(intermediates, value)));
	}

	public <R> ChainedOptional<R> map(Function<T, R> mapper) {
		return new ChainedOptional<>(intermediates, delegate.map(mapper));
	}

	public <R> ChainedOptional<R> map(String name, Function<T, R> mapper) {
		return new ChainedOptional<>(intermediates, name, delegate.map(mapper));
	}

	public <R> ChainedOptional<R> map(BiFunction<Map<String, Object>, T, R> mapper) {
		return new ChainedOptional<>(intermediates, delegate.map(value -> mapper.apply(intermediates, value)));
	}

	public <R> ChainedOptional<R> map(String name, BiFunction<Map<String, Object>, T, R> mapper) {
		return new ChainedOptional<>(intermediates, name, delegate.map(value -> mapper.apply(intermediates, 
				value)));
	}
	
	public <U> ChainedOptional<U> flatMap(Function<? super T, Optional<U>> mapper) {
		return new ChainedOptional<>(intermediates, delegate.flatMap(mapper));
	}

	public <U> ChainedOptional<U> flatMap(String name, Function<? super T, Optional<U>> mapper) {
		return new ChainedOptional<>(intermediates, name, delegate.flatMap(mapper));
	}

	public <U> ChainedOptional<U> flatMap(BiFunction<Map<String, Object>, ? super T, Optional<U>> mapper) {
		return new ChainedOptional<>(intermediates, delegate.flatMap(value -> mapper.apply(intermediates, 
				value)));
	}

	public <U> ChainedOptional<U> flatMap(String name, 
			BiFunction<Map<String, Object>, ? super T, Optional<U>> mapper) {
		return new ChainedOptional<>(intermediates, name, delegate.flatMap(value -> mapper.apply(intermediates, 
				value)));
	}

	public ChainedOptional<T> ifPresent(Consumer<T> consumer) {
		delegate.ifPresent(consumer);
		return this;
	}

	public ChainedOptional<T> ifPresent(BiConsumer<Map<String, Object>, T> consumer) {
		delegate.ifPresent(value -> consumer.accept(intermediates, value));
		return this;
	}
	
	public T orElseGet(Supplier<? extends T> supplier) {
		return delegate.orElseGet(supplier);
	}

	public T orElseGet(Function<Map<String, Object>, T> supplier) {
		return orElseGet(() -> supplier.apply(intermediates));
	}
	
	public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		return delegate.orElseThrow(exceptionSupplier);
	}

	public <X extends Throwable> T orElseThrow(Function<Map<String, Object>, X> exceptionSupplier) throws X {
		return orElseThrow(() -> exceptionSupplier.apply(intermediates));
	}
	
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
	
	@SuppressWarnings("nls")
	public static void main(String[] args) {
		ChainedOptional.of(1)
		.map(s -> s + 1)
		.map("hw", s -> "hello world")
		// .map(s -> (String) null)
		.map("lgth", String::length)
		.filter((intermediates, val) -> (int)intermediates.get("lgth") == 11)
		.map((intermediates, val) -> val + 1 + (int)intermediates.get("lgth"))
		.ifPresent((intermediates, result) -> {
				System.out.println(intermediates);
				System.out.println("Result: " + intermediates.get("hw") + " is of length:" + intermediates.get("lgth"));
		})
		.orElseThrow(intermediates -> {
				System.err.println(intermediates);
				return new NoSuchElementException();
		});
		
		String str = ChainedOptional.of(1)
		.map(s -> s + 1)
		.map("hw", s -> "hello world")
		.map(s -> (String) null)
//		.map("lgth", String::length)
		.map((intermediates, val) -> val + 1 + (int)intermediates.get("lgth"))
		.ifPresent((intermediates, result) -> {
				System.out.println(intermediates);
				System.out.println("Result: " + intermediates.get("hw") + " is of length:" + intermediates.get("lgth"));
		})
		.orElseGet(intermediates -> {
				System.err.println(intermediates);
				return (String)intermediates.get("hw");
		});
		System.out.println("str =" + str);
	}
}
