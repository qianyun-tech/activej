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
import io.activej.dataflow.dataset.LocallySortedDataset;
import io.activej.dataflow.dataset.SortedDataset;
import io.activej.dataflow.graph.DataflowContext;
import io.activej.dataflow.graph.DataflowGraph;
import io.activej.dataflow.graph.Partition;
import io.activej.dataflow.graph.StreamId;
import io.activej.dataflow.node.NodeOffsetLimit;
import io.activej.datastream.processor.StreamLimiter;
import io.activej.datastream.processor.StreamSkip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.activej.dataflow.dataset.DatasetUtils.limitStream;
import static io.activej.datastream.processor.StreamReducers.mergeReducer;

public final class DatasetOffsetLimit<K, T> extends SortedDataset<K, T> {
	private final LocallySortedDataset<K, T> input;

	private final long offset;
	private final long limit;

	private final int sharderNonce = ThreadLocalRandom.current().nextInt();

	public DatasetOffsetLimit(LocallySortedDataset<K, T> input, long offset, long limit) {
		super(input.streamSchema(), input.keyComparator(), input.keyType(), input.keyFunction());
		this.input = input;
		this.offset = offset;
		this.limit = limit;
	}

	@Override
	public List<StreamId> channels(DataflowContext context) {
		DataflowContext next = context.withFixedNonce(sharderNonce);

		List<StreamId> streamIds = input.channels(next);

		if (offset == StreamSkip.NO_SKIP && limit == StreamLimiter.NO_LIMIT) return streamIds;

		DataflowGraph graph = next.getGraph();

		if (streamIds.isEmpty()) return streamIds;

		if (streamIds.size() == 1) {
			return toOutput(graph, next.generateNodeIndex(), streamIds.get(0));
		}

		if (limit != StreamLimiter.NO_LIMIT) {
			List<StreamId> newStreamIds = new ArrayList<>(streamIds.size());
			for (StreamId streamId : streamIds) {
				StreamId limitedStream = limitStream(graph, next.generateNodeIndex(), offset + limit, streamId);
				newStreamIds.add(limitedStream);
			}
			streamIds = newStreamIds;
		}

		StreamId randomStreamId = streamIds.get(Math.abs(sharderNonce) % streamIds.size());
		Partition randomPartition = graph.getPartition(randomStreamId);

		List<StreamId> newStreamIds = DatasetUtils.repartitionAndReduce(next, streamIds, streamSchema(), input.keyFunction(), input.keyComparator(), mergeReducer(), List.of(randomPartition));
		assert newStreamIds.size() == 1;

		return toOutput(graph, next.generateNodeIndex(), newStreamIds.get(0));
	}

	@Override
	public Collection<Dataset<?>> getBases() {
		return List.of(input);
	}

	private List<StreamId> toOutput(DataflowGraph graph, int index, StreamId streamId) {
		NodeOffsetLimit<T> node = new NodeOffsetLimit<>(index, offset, limit, streamId);
		graph.addNode(graph.getPartition(streamId), node);
		return List.of(node.getOutput());
	}
}
