/*
 * Copyright (C) 2020 ActiveJ LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.activej.dataflow.dataset.impl;

import io.activej.dataflow.dataset.Dataset;
import io.activej.dataflow.dataset.DatasetUtils;
import io.activej.dataflow.graph.DataflowContext;
import io.activej.dataflow.graph.StreamId;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class DatasetOffsetLimit<T, K> extends Dataset<T> {
	private final Dataset<T> input;
	private final Function<T, K> keyFunction;

	private final long offset;
	private final long limit;

	private final int sharderNonce = ThreadLocalRandom.current().nextInt();

	public DatasetOffsetLimit(Dataset<T> input, Function<T, K> keyFunction, long offset, long limit) {
		super(input.streamSchema());
		this.input = input;
		this.keyFunction = keyFunction;
		this.offset = offset;
		this.limit = limit;
	}

	@Override
	public List<StreamId> channels(DataflowContext context) {
		DataflowContext next = context.withFixedNonce(sharderNonce);

		List<StreamId> streamIds = input.channels(next);

		return DatasetUtils.offsetLimit(next, streamIds, offset, limit,
				(inputs, partition) -> {
					List<StreamId> repartitioned = DatasetUtils.repartition(next, inputs, input.streamSchema(), keyFunction, List.of(partition));
					assert repartitioned.size() == 1;
					return repartitioned.get(0);
				});
	}

	@Override
	public Collection<Dataset<?>> getBases() {
		return List.of(input);
	}
}
