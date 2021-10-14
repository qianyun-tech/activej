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

package io.activej.serializer.impl;

import io.activej.codegen.expression.Expression;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.SerializerDef;
import org.jetbrains.annotations.NotNull;

import static io.activej.codegen.expression.Expressions.call;
import static io.activej.codegen.expression.Expressions.constructor;

public final class SerializerDefRegularMap extends AbstractSerializerDefMap {
	public SerializerDefRegularMap(SerializerDef keySerializer, SerializerDef valueSerializer, Class<?> encodeType, Class<?> decodeType) {
		super(keySerializer, valueSerializer, encodeType, decodeType, Object.class, Object.class, false);
	}

	private SerializerDefRegularMap(SerializerDef keySerializer, SerializerDef valueSerializer, Class<?> encodeType, Class<?> decodeType, boolean nullable) {
		super(keySerializer, valueSerializer, encodeType, decodeType, Object.class, Object.class, nullable);
	}

	@Override
	protected SerializerDef doEnsureNullable(CompatibilityLevel compatibilityLevel) {
		return new SerializerDefRegularMap(keySerializer, valueSerializer, encodeType, decodeType, true);
	}

	@Override
	protected Expression createBuilder(Expression length) {
		return constructor(decodeType, initialSize(length));
	}

	@Override
	protected @NotNull Expression putToBuilder(Expression builder, Expression index, Expression key, Expression value) {
		return call(builder, "put", key, value);
	}
}
