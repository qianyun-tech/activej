package io.activej.crdt.util;

import io.activej.common.ref.RefInt;
import io.activej.crdt.storage.cluster.RendezvousHashSharder;
import org.junit.Test;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.activej.common.collection.CollectorUtils.toLinkedHashMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RendezvousHashSharderTest {

	@Test
	public void testSharder() {
		RefInt index = new RefInt(0);
		Map<String, Integer> partitionsWithIndexes = Stream.of("one", "two", "three", "four", "five")
			.collect(toLinkedHashMap(s -> index.value++));
		Set<String> alive = new HashSet<>(partitionsWithIndexes.keySet());
		RendezvousHashSharder<Integer> sharder1 = RendezvousHashSharder.create(
			Object::hashCode, Object::hashCode,
			partitionsWithIndexes.keySet(), new ArrayList<>(alive), 3, false);

		Map<Integer, Set<Integer>> sharded1 = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			for (int id : sharder1.shard(i)) {
				sharded1.computeIfAbsent(i, $ -> new HashSet<>()).add(id);
			}
		}
		Set<Integer> allSharded = sharded1.values().stream()
			.flatMap(Collection::stream)
			.collect(toSet());

		assertEquals(Set.copyOf(partitionsWithIndexes.values()), allSharded);

		alive.remove("five");
		int fiveId = partitionsWithIndexes.remove("five");

		RendezvousHashSharder<Integer> sharder2 = RendezvousHashSharder.create(
			Object::hashCode, Object::hashCode,
			partitionsWithIndexes.keySet(), new ArrayList<>(alive), 3, false);

		Map<Integer, Set<Integer>> sharded2 = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			for (int id : sharder2.shard(i)) {
				sharded2.computeIfAbsent(i, $ -> new HashSet<>()).add(id);
			}
		}
		for (Map.Entry<Integer, Set<Integer>> entry : sharded1.entrySet()) {
			Set<Integer> first = entry.getValue();
			Set<Integer> second = sharded2.get(entry.getKey());
			for (int id : first) {
				if (id != fiveId) {
					assertTrue(second.contains(id));
				}
			}
		}

		allSharded = sharded2.values().stream()
			.flatMap(Collection::stream)
			.collect(toSet());

		assertEquals(Set.copyOf(partitionsWithIndexes.values()), allSharded);

		alive.remove("four");

		RendezvousHashSharder<Integer> sharder3 = RendezvousHashSharder.create(
			Object::hashCode, Object::hashCode,
			partitionsWithIndexes.keySet(), new ArrayList<>(alive), 3, true);

		List<int[]> shards3 = IntStream.range(0, 100)
			.mapToObj(sharder3::shard).toList();
		int[] first = shards3.get(0);
		assertTrue(shards3.stream().allMatch(ints -> Arrays.equals(first, ints)));
	}
}
