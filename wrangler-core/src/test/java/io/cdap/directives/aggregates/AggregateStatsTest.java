/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests {@link AggregateStats}
 */
public class AggregateStatsTest {

    private static final double DELTA = 0.001;

    @Test
    public void testBasicAggregation() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 100L).add("process_time", 5L),
                new Row("disk_size", 200L).add("process_time", 8L),
                new Row("disk_size", 150L).add("process_time", 6L));

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(450L, ((Long) rows.get(0).getValue("total_size")).longValue());
        Assert.assertEquals(19L, ((Long) rows.get(0).getValue("total_time")).longValue());
    }

    @Test
    public void testSpecificationExample() throws Exception {
        // Test case exactly matching specification example
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 'seconds'"
        };

        // Create sample log/transaction data
        // Using 1 MB = 1024 * 1024 bytes for consistency
        List<Row> rows = Arrays.asList(
                // 2.5 MB data, 1.5 seconds response time
                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.5 * 1000),
                // 1.8 MB data, 0.8 seconds response time
                new Row("data_transfer_size", 1.8 * 1024 * 1024).add("response_time", 0.8 * 1000),
                // 3.2 MB data, 2.1 seconds response time
                new Row("data_transfer_size", 3.2 * 1024 * 1024).add("response_time", 2.1 * 1000));

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify results
        Assert.assertEquals(1, results.size());
        // Expected: 2.5 + 1.8 + 3.2 = 7.5 MB
        Assert.assertEquals(7.5, ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
        // Expected: 1.5 + 0.8 + 2.1 = 4.4 seconds
        Assert.assertEquals(4.4, ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
    }

    @Test
    public void testAggregationWithUnits() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_mb :total_time_min 'MB' 'minutes'"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L).add("process_time", 120L), // 1MB, 2min
                new Row("disk_size", 2048L * 1024L).add("process_time", 180L) // 2MB, 3min
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
        Assert.assertEquals(5.0, ((Double) rows.get(0).getValue("total_time_min")).doubleValue(), DELTA);
    }

    @Test
    public void testAggregationWithAverage() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :avg_size :avg_time 'MB' 'minutes' true"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L).add("process_time", 120L), // 1MB, 2min
                new Row("disk_size", 2048L * 1024L).add("process_time", 180L), // 2MB, 3min
                new Row("disk_size", 3072L * 1024L).add("process_time", 240L) // 3MB, 4min
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(2.0, ((Double) rows.get(0).getValue("avg_size")).doubleValue(), DELTA);
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("avg_time")).doubleValue(), DELTA);
    }

    @Test
    public void testSmallValuesWithPrecision() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_kb :total_time_ms 'KB' 'milliseconds'"
        };

        // Test with small values to verify precision
        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L).add("process_time", 100L), // 1KB, 100ms
                new Row("disk_size", 2048L).add("process_time", 150L) // 2KB, 150ms
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_kb")).doubleValue(), DELTA);
        Assert.assertEquals(250.0, ((Double) rows.get(0).getValue("total_time_ms")).doubleValue(), DELTA);
    }

    @Test
    public void testEmptyInput() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.emptyList();
        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(0L, ((Long) rows.get(0).getValue("total_size")).longValue());
        Assert.assertEquals(0L, ((Long) rows.get(0).getValue("total_time")).longValue());
    }

    @Test(expected = RecipeException.class)
    public void testMissingColumn() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :missing_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", 100L).add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test(expected = RecipeException.class)
    public void testInvalidInputType() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", "not a number").add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test(expected = RecipeException.class)
    public void testInvalidTimeUnit() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time 'MB' 'invalid_unit'"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", 100L).add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test
    public void testLargeNumbers() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_tb :total_time_hr 'TB' 'hours'"
        };

        // Test with large numbers to verify no overflow
        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L * 1024L * 1024L).add("process_time", 3600L), // 1TB, 1hr
                new Row("disk_size", 2048L * 1024L * 1024L * 1024L).add("process_time", 7200L) // 2TB, 2hr
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_tb")).doubleValue(), DELTA);
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_time_hr")).doubleValue(), DELTA);
    }

    @Test
    public void testMedianCalculation() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :median_size :median_time 'MB' 'seconds' true median"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L).add("process_time", 1000L), // 1MB, 1sec
                new Row("disk_size", 2048L * 1024L).add("process_time", 2000L), // 2MB, 2sec
                new Row("disk_size", 3072L * 1024L).add("process_time", 3000L), // 3MB, 3sec
                new Row("disk_size", 4096L * 1024L).add("process_time", 4000L), // 4MB, 4sec
                new Row("disk_size", 5120L * 1024L).add("process_time", 5000L) // 5MB, 5sec
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("median_size")).doubleValue(), DELTA);
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("median_time")).doubleValue(), DELTA);
    }

    @Test
    public void testPercentileCalculations() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :p95_size :p95_time 'MB' 'seconds' true p95",
                "aggregate-size-time :disk_size :process_time :p99_size :p99_time 'MB' 'seconds' true p99"
        };

        // Create 100 rows with increasing values to test percentiles
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            rows.add(new Row("disk_size", (i + 1) * 1024L * 1024L) // 1MB to 100MB
                    .add("process_time", (i + 1) * 1000L)); // 1sec to 100sec
        }

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());

        // For 100 values, 95th percentile should be 95th value (95MB, 95sec)
        Assert.assertEquals(95.0, ((Double) rows.get(0).getValue("p95_size")).doubleValue(), DELTA);
        Assert.assertEquals(95.0, ((Double) rows.get(0).getValue("p95_time")).doubleValue(), DELTA);

        // For 100 values, 99th percentile should be 99th value (99MB, 99sec)
        Assert.assertEquals(99.0, ((Double) rows.get(0).getValue("p99_size")).doubleValue(), DELTA);
        Assert.assertEquals(99.0, ((Double) rows.get(0).getValue("p99_time")).doubleValue(), DELTA);
    }

    @Test
    public void testStatisticalMeasuresWithUnits() throws Exception {
        String[] directives = new String[] {
                // Test all statistical measures with different units
                "aggregate-size-time :disk_size :process_time :avg_size :avg_time 'GB' 'minutes' true average",
                "aggregate-size-time :disk_size :process_time :median_size :median_time 'GB' 'minutes' true median",
                "aggregate-size-time :disk_size :process_time :p95_size :p95_time 'GB' 'minutes' true p95",
                "aggregate-size-time :disk_size :process_time :p99_size :p99_time 'GB' 'minutes' true p99"
        };

        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // Values from 1GB to 100GB and 1min to 100min
            rows.add(new Row("disk_size", (i + 1) * 1024L * 1024L * 1024L)
                    .add("process_time", (i + 1) * 60L * 1000L));
        }

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());

        // Average should be 50.5 (mean of 1 to 100)
        Assert.assertEquals(50.5, ((Double) rows.get(0).getValue("avg_size")).doubleValue(), DELTA);
        Assert.assertEquals(50.5, ((Double) rows.get(0).getValue("avg_time")).doubleValue(), DELTA);

        // Median should be 50.0 (middle value)
        Assert.assertEquals(50.0, ((Double) rows.get(0).getValue("median_size")).doubleValue(), DELTA);
        Assert.assertEquals(50.0, ((Double) rows.get(0).getValue("median_time")).doubleValue(), DELTA);

        // Verify p95 and p99 with GB and minute units
        Assert.assertEquals(95.0, ((Double) rows.get(0).getValue("p95_size")).doubleValue(), DELTA);
        Assert.assertEquals(95.0, ((Double) rows.get(0).getValue("p95_time")).doubleValue(), DELTA);
        Assert.assertEquals(99.0, ((Double) rows.get(0).getValue("p99_size")).doubleValue(), DELTA);
        Assert.assertEquals(99.0, ((Double) rows.get(0).getValue("p99_time")).doubleValue(), DELTA);
    }

    @Test
    public void testComprehensiveAggregation() throws Exception {
        // Base recipe for total aggregation
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 'seconds'",
                // Additional recipes for statistics
                "aggregate-size-time :data_transfer_size :response_time :avg_size_mb :avg_time_sec 'MB' 'seconds' true average",
                "aggregate-size-time :data_transfer_size :response_time :p95_size_mb :p95_time_sec 'MB' 'seconds' true p95"
        };

        // Sample data with consistent unit conversions
        // Using 1 MB = 1024 * 1024 bytes (binary conversion)
        List<Row> rows = Arrays.asList(
                // 1.5 MB, 2.5 seconds
                new Row("data_transfer_size", 1.5 * 1024 * 1024).add("response_time", 2.5 * 1000),
                // 2.5 MB, 1.8 seconds
                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.8 * 1000),
                // 3.0 MB, 3.2 seconds
                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.2 * 1000),
                // 4.2 MB, 2.1 seconds
                new Row("data_transfer_size", 4.2 * 1024 * 1024).add("response_time", 2.1 * 1000),
                // 5.8 MB, 4.4 seconds
                new Row("data_transfer_size", 5.8 * 1024 * 1024).add("response_time", 4.4 * 1000));

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify the results
        Assert.assertEquals(1, results.size());

        // Total calculations
        // Expected total size: 1.5 + 2.5 + 3.0 + 4.2 + 5.8 = 17.0 MB
        Assert.assertEquals(17.0, ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
        // Expected total time: 2.5 + 1.8 + 3.2 + 2.1 + 4.4 = 14.0 seconds
        Assert.assertEquals(14.0, ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);

        // Average calculations
        // Expected average size: 17.0 / 5 = 3.4 MB
        Assert.assertEquals(3.4, ((Double) results.get(0).getValue("avg_size_mb")).doubleValue(), DELTA);
        // Expected average time: 14.0 / 5 = 2.8 seconds
        Assert.assertEquals(2.8, ((Double) results.get(0).getValue("avg_time_sec")).doubleValue(), DELTA);

        // 95th percentile calculations (with 5 values, should be the second-highest
        // value)
        Assert.assertEquals(5.8, ((Double) results.get(0).getValue("p95_size_mb")).doubleValue(), DELTA);
        Assert.assertEquals(4.4, ((Double) results.get(0).getValue("p95_time_sec")).doubleValue(), DELTA);

        // total_size_mb column (using 1 MB = 1024 * 1024 bytes)
        // Time Calculation: Sum response_time values (in nanoseconds/ms) then convert
        // to total_time_sec column

    }

}