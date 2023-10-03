package io.activej.cube.etcd;

import io.activej.cube.aggregation.AggregationChunk;
import io.activej.cube.aggregation.ot.AggregationDiff;
import io.activej.cube.ot.CubeDiff;
import io.activej.etcd.TxnOps;
import io.activej.etcd.codec.key.EtcdKeyCodecs;
import io.activej.etcd.codec.kv.EtcdKVCodec;
import io.activej.etcd.codec.kv.EtcdKVCodecs;
import io.activej.etcd.codec.prefix.EtcdPrefixCodec;
import io.activej.etcd.codec.prefix.Prefix;
import io.activej.etl.LogDiff;
import io.activej.etl.LogPositionDiff;
import io.etcd.jetcd.ByteSequence;

import java.util.Map;
import java.util.function.Function;

import static io.activej.common.Utils.entriesToLinkedHashMap;
import static io.activej.cube.etcd.CubeEtcdOTUplink.logPositionEtcdCodec;
import static io.activej.etcd.EtcdUtils.*;

final class EtcdUtils {
	static void saveCubeLogDiff(ByteSequence prefixPos, ByteSequence prefixCube, EtcdPrefixCodec<String> aggregationIdCodec, Function<String, EtcdKVCodec<Long, AggregationChunk>> chunkCodecsFactory, TxnOps txn, LogDiff<CubeDiff> logDiff) {
		savePositions(txn.child(prefixPos), logDiff.getPositions());
		for (CubeDiff diff : logDiff.getDiffs()) {
			saveCubeDiff(aggregationIdCodec, chunkCodecsFactory, txn.child(prefixCube), diff);
		}
	}

	private static void savePositions(TxnOps txn, Map<String, LogPositionDiff> positions) {
		checkAndInsert(txn,
			EtcdKVCodecs.ofMapEntry(EtcdKeyCodecs.ofString(), logPositionEtcdCodec()),
			positions.entrySet().stream().filter(diff -> diff.getValue().from().isInitial()).collect(entriesToLinkedHashMap(LogPositionDiff::to)));
		checkAndUpdate(txn,
			EtcdKVCodecs.ofMapEntry(EtcdKeyCodecs.ofString(), logPositionEtcdCodec()),
			positions.entrySet().stream().filter(diff -> !diff.getValue().from().isInitial()).collect(entriesToLinkedHashMap(LogPositionDiff::from)),
			positions.entrySet().stream().filter(diff -> !diff.getValue().from().isInitial()).collect(entriesToLinkedHashMap(LogPositionDiff::to)));
	}

	private static void saveCubeDiff(
		EtcdPrefixCodec<String> aggregationIdCodec,
		Function<String, EtcdKVCodec<Long, AggregationChunk>> chunkCodecsFactory, TxnOps txn,
		CubeDiff cubeDiff
	) {
		for (var entry : cubeDiff.getDiffs().entrySet().stream().collect(entriesToLinkedHashMap(AggregationDiff::getRemovedChunks)).entrySet()) {
			String aggregationId = entry.getKey();
			checkAndDelete(
				txn.child(aggregationIdCodec.encodePrefix(new Prefix<>(aggregationId, ByteSequence.EMPTY))),
				chunkCodecsFactory.apply(aggregationId),
				entry.getValue().stream().map(chunk -> (long) chunk.getChunkId()).toList());
		}
		for (var entry : cubeDiff.getDiffs().entrySet().stream().collect(entriesToLinkedHashMap(AggregationDiff::getAddedChunks)).entrySet()) {
			String aggregationId = entry.getKey();
			checkAndInsert(
				txn.child(aggregationIdCodec.encodePrefix(new Prefix<>(aggregationId, ByteSequence.EMPTY))),
				chunkCodecsFactory.apply(aggregationId),
				entry.getValue());
		}
	}

}