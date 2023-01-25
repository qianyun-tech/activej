package io.activej.dataflow.codec.module;

import io.activej.dataflow.codec.Subtype;
import io.activej.datastream.processor.StreamReducers.Reducer_Deduplicate;
import io.activej.datastream.processor.StreamReducers.Reducer_Merge;
import io.activej.datastream.processor.StreamReducers.ReducerToResult;
import io.activej.datastream.processor.StreamReducers.ReducerToResult.AccumulatorToAccumulator;
import io.activej.datastream.processor.StreamReducers.ReducerToResult.InputToAccumulator;
import io.activej.datastream.processor.StreamReducers.ReducerToResult.InputToOutput;
import io.activej.inject.annotation.Provides;
import io.activej.inject.binding.OptionalDependency;
import io.activej.inject.module.AbstractModule;
import io.activej.serializer.stream.StreamCodec;
import io.activej.serializer.stream.StreamCodecs;
import io.activej.serializer.stream.StreamInput;
import io.activej.serializer.stream.StreamOutput;

import java.io.IOException;

final class ReducerCodecModule extends AbstractModule {
	@Provides
	@Subtype(0)
	StreamCodec<Reducer_Merge<?, ?>> mergeReducer() {
		return StreamCodecs.singleton(new Reducer_Merge<>());
	}

	@Provides
	@Subtype(1)
	StreamCodec<Reducer_Deduplicate<?, ?>> deduplicateReducer() {
		return StreamCodecs.singleton(new Reducer_Deduplicate<>());
	}

	@Provides
	@Subtype(2)
	StreamCodec<InputToAccumulator<?, ?, ?, ?>> inputToAccumulator(
			OptionalDependency<StreamCodec<ReducerToResult<?, ?, ?, ?>>> optionalReducerToResultSerializer
	) {
		StreamCodec<ReducerToResult<?, ?, ?, ?>> reducerToResultSerializer = optionalReducerToResultSerializer.get();
		return new StreamCodec<>() {
			@Override
			public void encode(StreamOutput output, InputToAccumulator<?, ?, ?, ?> item) throws IOException {
				reducerToResultSerializer.encode(output, item.getReducerToResult());
			}

			@Override
			public InputToAccumulator<?, ?, ?, ?> decode(StreamInput input) throws IOException {
				return new InputToAccumulator<>(reducerToResultSerializer.decode(input));
			}
		};
	}

	@Provides
	@Subtype(3)
	StreamCodec<InputToOutput<?, ?, ?, ?>> inputToOutput(
			OptionalDependency<StreamCodec<ReducerToResult<?, ?, ?, ?>>> optionalReducerToResultSerializer
	) {
		StreamCodec<ReducerToResult<?, ?, ?, ?>> reducerToResultSerializer = optionalReducerToResultSerializer.get();
		return new StreamCodec<>() {
			@Override
			public void encode(StreamOutput out, InputToOutput<?, ?, ?, ?> item) throws IOException {
				reducerToResultSerializer.encode(out, item.getReducerToResult());
			}

			@Override
			public InputToOutput<?, ?, ?, ?> decode(StreamInput in) throws IOException {
				return new InputToOutput<>(reducerToResultSerializer.decode(in));
			}
		};
	}

	@Provides
	@Subtype(4)
	StreamCodec<AccumulatorToAccumulator<?, ?, ?, ?>> accumulatorToAccumulator(
			OptionalDependency<StreamCodec<ReducerToResult<?, ?, ?, ?>>> optionalReducerToResultSerializer
	) {
		StreamCodec<ReducerToResult<?, ?, ?, ?>> reducerToResultSerializer = optionalReducerToResultSerializer.get();
		return new StreamCodec<>() {
			@Override
			public void encode(StreamOutput out, AccumulatorToAccumulator<?, ?, ?, ?> item) throws IOException {
				reducerToResultSerializer.encode(out, item.getReducerToResult());
			}

			@Override
			public AccumulatorToAccumulator<?, ?, ?, ?> decode(StreamInput in) throws IOException {
				return new AccumulatorToAccumulator<>(reducerToResultSerializer.decode(in));
			}
		};
	}

	@Provides
	@Subtype(5)
	StreamCodec<ReducerToResult.AccumulatorToOutput<?, ?, ?, ?>> accumulatorToOutput(
			OptionalDependency<StreamCodec<ReducerToResult<?, ?, ?, ?>>> optionalReducerToResultSerializer
	) {
		StreamCodec<ReducerToResult<?, ?, ?, ?>> reducerToResultSerializer = optionalReducerToResultSerializer.get();
		return new StreamCodec<>() {
			@Override
			public void encode(StreamOutput out, ReducerToResult.AccumulatorToOutput<?, ?, ?, ?> item) throws IOException {
				reducerToResultSerializer.encode(out, item.getReducerToResult());
			}

			@Override
			public ReducerToResult.AccumulatorToOutput<?, ?, ?, ?> decode(StreamInput in) throws IOException {
				return new ReducerToResult.AccumulatorToOutput<>(reducerToResultSerializer.decode(in));
			}
		};
	}
}
