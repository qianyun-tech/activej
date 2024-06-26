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

package io.activej.eventloop.inspector;

import io.activej.common.inspector.BaseInspector;
import io.activej.common.time.Stopwatch;
import org.jetbrains.annotations.Nullable;

public interface EventloopInspector extends BaseInspector<EventloopInspector> {
	void onUpdateBusinessLogicTime(boolean taskOrKeyPresent, boolean externalTaskPresent, long businessLogicTime);

	void onUpdateSelectorSelectTime(long selectorSelectTime);

	void onUpdateSelectorSelectTimeout(long selectorSelectTimeout);

	void onUpdateSelectedKeyDuration(Stopwatch sw);

	void onUpdateSelectedKeysStats(int lastSelectedKeys, int invalidKeys, int acceptKeys, int connectKeys, int readKeys, int writeKeys, long loopTime);

	void onUpdateLocalTaskDuration(Runnable runnable, @Nullable Stopwatch sw);

	void onUpdateLocalTasksStats(int localTasks, long loopTime);

	void onUpdateConcurrentTaskDuration(Runnable runnable, @Nullable Stopwatch sw);

	void onUpdateConcurrentTasksStats(int newConcurrentTasks, long loopTime);

	void onUpdateScheduledTaskDuration(Runnable runnable, @Nullable Stopwatch sw, boolean background);

	void onUpdateScheduledTasksStats(int scheduledTasks, long loopTime, boolean background);

	void onFatalError(Throwable e, @Nullable Object context);

	void onScheduledTaskOverdue(long overdue, boolean background);
}
