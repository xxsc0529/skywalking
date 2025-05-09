/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.analysis.meter.function.min;

import org.apache.skywalking.oap.server.core.analysis.Layer;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.meter.MeterEntity;
import org.apache.skywalking.oap.server.core.analysis.meter.function.AcceptableValue;
import org.apache.skywalking.oap.server.core.config.NamingControl;
import org.apache.skywalking.oap.server.core.config.group.EndpointNameGrouping;
import org.apache.skywalking.oap.server.core.storage.type.HashMapConverter;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class MinFunctionTest {

    private static final Long LARGE_VALUE = 1597113447737L;

    private static final Long SMALL_VALUE = 1597113318673L;

    private MinFunction function;

    @BeforeAll
    public static void setup() {
        MeterEntity.setNamingControl(new NamingControl(512, 512, 512, new EndpointNameGrouping()));
    }

    @BeforeEach
    public void before() {
        function = new MinFunctionInst();
        function.setTimeBucket(TimeBucket.getMinuteTimeBucket(System.currentTimeMillis()));
    }

    @AfterAll
    public static void tearDown() {
        MeterEntity.setNamingControl(null);
    }

    @Test
    public void testAccept() {
        function.accept(MeterEntity.newService("service-test", Layer.GENERAL), SMALL_VALUE);
        assertThat(function.getValue()).isEqualTo(SMALL_VALUE);

        function.accept(MeterEntity.newService("service-test", Layer.GENERAL), LARGE_VALUE);
        assertThat(function.getValue()).isEqualTo(SMALL_VALUE);
    }

    @Test
    public void testCalculate() {
        function.accept(MeterEntity.newService("service-test", Layer.GENERAL), SMALL_VALUE);
        function.accept(MeterEntity.newService("service-test", Layer.GENERAL), LARGE_VALUE);
        function.calculate();

        assertThat(function.getValue()).isEqualTo(SMALL_VALUE);
    }

    @Test
    public void testToHour() {
        function.setTimeBucket(TimeBucket.getMinuteTimeBucket(System.currentTimeMillis()));
        MeterEntity meterEntity = MeterEntity.newService("service-test", Layer.GENERAL);
        meterEntity.setAttr0("testAttr");
        function.accept(meterEntity, LARGE_VALUE);
        function.accept(meterEntity, SMALL_VALUE);
        function.calculate();

        final MinFunction hourFunction = (MinFunction) function.toHour();
        hourFunction.calculate();

        assertThat(hourFunction.getValue()).isEqualTo(SMALL_VALUE);
        assertThat(hourFunction.getAttr0()).isEqualTo(function.getAttr0());
    }

    @Test
    public void testToDay() {
        function.setTimeBucket(TimeBucket.getMinuteTimeBucket(System.currentTimeMillis()));
        MeterEntity meterEntity = MeterEntity.newService("service-test", Layer.GENERAL);
        meterEntity.setAttr0("testAttr");
        function.accept(meterEntity, LARGE_VALUE);
        function.accept(meterEntity, SMALL_VALUE);
        function.calculate();

        final MinFunction dayFunction = (MinFunction) function.toDay();
        dayFunction.calculate();
        assertThat(dayFunction.getValue()).isEqualTo(SMALL_VALUE);
        assertThat(dayFunction.getAttr0()).isEqualTo(function.getAttr0());
    }

    @Test
    public void testSerialize() {
        MeterEntity meterEntity = MeterEntity.newService("service-test", Layer.GENERAL);
        meterEntity.setAttr0("testAttr");
        function.accept(meterEntity, SMALL_VALUE);
        MinFunction function2 = Mockito.spy(MinFunction.class);
        function2.deserialize(function.serialize().build());

        assertThat(function2.getEntityId()).isEqualTo(function.getEntityId());
        assertThat(function2.getTimeBucket()).isEqualTo(function.getTimeBucket());
        assertThat(function2.getServiceId()).isEqualTo(function.getServiceId());
        assertThat(function2.getValue()).isEqualTo(function.getValue());
        assertThat(function2.getAttr0()).isEqualTo(function.getAttr0());

    }

    @Test
    public void testBuilder() throws IllegalAccessException, InstantiationException {
        MeterEntity meterEntity = MeterEntity.newService("service-test", Layer.GENERAL);
        meterEntity.setAttr0("testAttr");
        function.accept(meterEntity, SMALL_VALUE);
        function.calculate();
        StorageBuilder<MinFunction> storageBuilder = function.builder().newInstance();

        final HashMapConverter.ToStorage toStorage = new HashMapConverter.ToStorage();
        storageBuilder.entity2Storage(function, toStorage);
        final Map<String, Object> map = toStorage.obtain();
        map.put(MinFunction.VALUE, map.get(MinFunction.VALUE));

        MinFunction function2 = storageBuilder.storage2Entity(new HashMapConverter.ToEntity(map));

        assertThat(function2.getValue()).isEqualTo(function.getValue());
    }

    private static class MinFunctionInst extends MinFunction {
        @Override
        public AcceptableValue<Long> createNew() {
            return new MinFunctionInst();
        }
    }
}
