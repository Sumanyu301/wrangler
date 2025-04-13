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
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test cases for the AggregateStats directive following the specifications.
 */
public class AggregateStatsSpecificationTest {

        // Small delta for floating-point comparisons
        private static final double DELTA = 0.001;

        @Test
        public void testSumAggregation() throws Exception {
                // Input Data: Create sample log/transaction data
                List<Row> rows = Arrays.asList(
                                // 1.5 MB data, 2.5 seconds response time
                                new Row("data_transfer_size", 1.5 * 1024 * 1024).add("response_time", 2.5 * 1000),
                                // 2.5 MB data, 1.8 seconds response time
                                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.8 * 1000),
                                // 3.0 MB data, 3.2 seconds response time
                                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.2 * 1000));

                // Recipe for total aggregation
                String[] recipe = new String[] {
                                // Aggregate size (output MB), total time (output seconds)
                                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
                };

                // Execution: Use TestingRig.execute
                List<Row> results = TestingRig.execute(recipe, rows);

                // Expected Output: Assert single row with correctly calculated values
                Assert.assertEquals(1, results.size());

                // Size Calculation: Sum all data_transfer_size values (converted to bytes)
                // and convert to MB (1 MB = 1024 * 1024 bytes)
                // Expected: 1.5 + 2.5 + 3.0 = 7.0 MB
                double expectedTotalSizeInMB = 7.0;
                Assert.assertEquals(expectedTotalSizeInMB,
                                ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);

                // Time Calculation: Sum all response_time values (in milliseconds)
                // and convert to seconds
                // Expected: 2.5 + 1.8 + 3.2 = 7.5 seconds
                double expectedTotalTimeInSeconds = 7.5;
                Assert.assertEquals(expectedTotalTimeInSeconds,
                                ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
        }

        @Test
        public void testAverageAggregation() throws Exception {
                // Input Data with more diverse values
                List<Row> rows = Arrays.asList(
                                new Row("data_transfer_size", 1.0 * 1024 * 1024).add("response_time", 1.0 * 1000),
                                new Row("data_transfer_size", 2.0 * 1024 * 1024).add("response_time", 2.0 * 1000),
                                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.0 * 1000),
                                new Row("data_transfer_size", 4.0 * 1024 * 1024).add("response_time", 4.0 * 1000));

                // Recipe for average aggregation
                String[] recipe = new String[] {
                                "aggregate-stats :data_transfer_size :response_time avg_size_mb avg_time_sec average"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // Average calculation: (1.0 + 2.0 + 3.0 + 4.0) / 4 = 2.5 MB
                double expectedAvgSizeInMB = 2.5;
                Assert.assertEquals(expectedAvgSizeInMB,
                                ((Double) results.get(0).getValue("avg_size_mb")).doubleValue(), DELTA);

                // Average calculation: (1.0 + 2.0 + 3.0 + 4.0) / 4 = 2.5 seconds
                double expectedAvgTimeInSeconds = 2.5;
                Assert.assertEquals(expectedAvgTimeInSeconds,
                                ((Double) results.get(0).getValue("avg_time_sec")).doubleValue(), DELTA);
        }

        @Test
        public void testMedianAggregation() throws Exception {
                // Input Data with values suitable for median testing
                List<Row> rows = Arrays.asList(
                                new Row("data_transfer_size", 1.0 * 1024 * 1024).add("response_time", 1.0 * 1000),
                                new Row("data_transfer_size", 2.0 * 1024 * 1024).add("response_time", 2.0 * 1000),
                                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.0 * 1000),
                                new Row("data_transfer_size", 4.0 * 1024 * 1024).add("response_time", 4.0 * 1000),
                                new Row("data_transfer_size", 5.0 * 1024 * 1024).add("response_time", 5.0 * 1000));

                // Recipe for median aggregation
                String[] recipe = new String[] {
                                "aggregate-stats :data_transfer_size :response_time median_size_mb median_time_sec median"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // Median of [1,2,3,4,5] is 3
                double expectedMedianSizeInMB = 3.0;
                Assert.assertEquals(expectedMedianSizeInMB,
                                ((Double) results.get(0).getValue("median_size_mb")).doubleValue(), DELTA);

                double expectedMedianTimeInSeconds = 3.0;
                Assert.assertEquals(expectedMedianTimeInSeconds,
                                ((Double) results.get(0).getValue("median_time_sec")).doubleValue(), DELTA);
        }

        @Test
        public void testPercentileAggregation() throws Exception {
                // Input Data with 100 rows for accurate percentile calculation
                List<Row> rows = new java.util.ArrayList<>();
                for (int i = 1; i <= 100; i++) {
                        rows.add(new Row("data_transfer_size", i * 1024 * 1024)
                                        .add("response_time", i * 1000));
                }

                // Recipe for p95 and p99 aggregation
                String[] recipe = new String[] {
                                "aggregate-stats :data_transfer_size :response_time p95_size_mb p95_time_sec p95",
                                "aggregate-stats :data_transfer_size :response_time p99_size_mb p99_time_sec p99"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // 95th percentile of 1-100 is 95
                double expectedP95SizeInMB = 95.0;
                Assert.assertEquals(expectedP95SizeInMB,
                                ((Double) results.get(0).getValue("p95_size_mb")).doubleValue(), DELTA);

                double expectedP95TimeInSeconds = 95.0;
                Assert.assertEquals(expectedP95TimeInSeconds,
                                ((Double) results.get(0).getValue("p95_time_sec")).doubleValue(), DELTA);

                // 99th percentile of 1-100 is 99
                double expectedP99SizeInMB = 99.0;
                Assert.assertEquals(expectedP99SizeInMB,
                                ((Double) results.get(0).getValue("p99_size_mb")).doubleValue(), DELTA);

                double expectedP99TimeInSeconds = 99.0;
                Assert.assertEquals(expectedP99TimeInSeconds,
                                ((Double) results.get(0).getValue("p99_time_sec")).doubleValue(), DELTA);
        }

        @Test
        public void testDifferentOutputUnits() throws Exception {
                // Input Data
                List<Row> rows = Arrays.asList(
                                new Row("data_transfer_size", 1.0 * 1024 * 1024).add("response_time", 60.0 * 1000),
                                new Row("data_transfer_size", 1.0 * 1024 * 1024).add("response_time", 60.0 * 1000));

                // Recipe for different output units
                String[] recipe = new String[] {
                                // Output in KB and milliseconds
                                "aggregate-stats :data_transfer_size :response_time total_size_kb total_time_ms KB ms",
                                // Output in GB and minutes
                                "aggregate-stats :data_transfer_size :response_time total_size_gb total_time_min GB min"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // 2 MB = 2048 KB
                double expectedTotalSizeInKB = 2048.0;
                Assert.assertEquals(expectedTotalSizeInKB,
                                ((Double) results.get(0).getValue("total_size_kb")).doubleValue(), DELTA);

                // 120,000 ms
                double expectedTotalTimeInMs = 120000.0;
                Assert.assertEquals(expectedTotalTimeInMs,
                                ((Double) results.get(0).getValue("total_time_ms")).doubleValue(), DELTA);

                // 2 MB = 0.001953125 GB
                double expectedTotalSizeInGB = 0.001953125;
                Assert.assertEquals(expectedTotalSizeInGB,
                                ((Double) results.get(0).getValue("total_size_gb")).doubleValue(), DELTA);

                // 120 seconds = 2 minutes
                double expectedTotalTimeInMin = 2.0;
                Assert.assertEquals(expectedTotalTimeInMin,
                                ((Double) results.get(0).getValue("total_time_min")).doubleValue(), DELTA);
        }

        @Test
        public void testMixedInputUnits() throws Exception {
                // Input Data with mixed units (as strings)
                List<Row> rows = Arrays.asList(
                                // Using string representation to simulate real-world log data
                                new Row("data_transfer_size", "1.5MB").add("response_time", "2.5s"),
                                new Row("data_transfer_size", "500KB").add("response_time", "800ms"),
                                new Row("data_transfer_size", "2MB").add("response_time", "1.2s"));

                // Recipe
                String[] recipe = new String[] {
                                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // 1.5MB + 500KB + 2MB = 4MB (500KB = 0.5MB)
                double expectedTotalSizeInMB = 4.0;
                Assert.assertEquals(expectedTotalSizeInMB,
                                ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);

                // 2.5s + 800ms + 1.2s = 4.5s (800ms = 0.8s)
                double expectedTotalTimeInSeconds = 4.5;
                Assert.assertEquals(expectedTotalTimeInSeconds,
                                ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
        }

        @Test
        public void testCompleteAggregations() throws Exception {
                // Input Data
                List<Row> rows = Arrays.asList(
                                new Row("data_transfer_size", 1.5 * 1024 * 1024).add("response_time", 2.5 * 1000),
                                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.8 * 1000),
                                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.2 * 1000),
                                new Row("data_transfer_size", 4.2 * 1024 * 1024).add("response_time", 2.1 * 1000),
                                new Row("data_transfer_size", 5.8 * 1024 * 1024).add("response_time", 4.4 * 1000));

                // Recipe with all statistics
                String[] recipe = new String[] {
                                // Sum
                                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec",
                                // Average
                                "aggregate-stats :data_transfer_size :response_time avg_size_mb avg_time_sec average",
                                // Median
                                "aggregate-stats :data_transfer_size :response_time median_size_mb median_time_sec median",
                                // Percentiles
                                "aggregate-stats :data_transfer_size :response_time p95_size_mb p95_time_sec p95",
                                "aggregate-stats :data_transfer_size :response_time p99_size_mb p99_time_sec p99"
                };

                // Execution
                List<Row> results = TestingRig.execute(recipe, rows);

                // Verify results
                Assert.assertEquals(1, results.size());

                // Total: 1.5 + 2.5 + 3.0 + 4.2 + 5.8 = 17.0 MB
                Assert.assertEquals(17.0,
                                ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
                // Total: 2.5 + 1.8 + 3.2 + 2.1 + 4.4 = 14.0 seconds
                Assert.assertEquals(14.0,
                                ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);

                // Average: 17.0 / 5 = 3.4 MB
                Assert.assertEquals(3.4,
                                ((Double) results.get(0).getValue("avg_size_mb")).doubleValue(), DELTA);
                // Average: 14.0 / 5 = 2.8 seconds
                Assert.assertEquals(2.8,
                                ((Double) results.get(0).getValue("avg_time_sec")).doubleValue(), DELTA);

                // Median: middle value from [1.5, 2.5, 3.0, 4.2, 5.8] = 3.0 MB
                Assert.assertEquals(3.0,
                                ((Double) results.get(0).getValue("median_size_mb")).doubleValue(), DELTA);
                // Median: middle value from [1.8, 2.1, 2.5, 3.2, 4.4] = 2.5 seconds
                Assert.assertEquals(2.5,
                                ((Double) results.get(0).getValue("median_time_sec")).doubleValue(), DELTA);

                // 95th percentile with 5 values is the 5th value: 5.8 MB
                Assert.assertEquals(5.8,
                                ((Double) results.get(0).getValue("p95_size_mb")).doubleValue(), DELTA);
                // 95th percentile with 5 values is the 5th value: 4.4 seconds
                Assert.assertEquals(4.4,
                                ((Double) results.get(0).getValue("p95_time_sec")).doubleValue(), DELTA);

                // 99th percentile with 5 values is the 5th value: 5.8 MB
                Assert.assertEquals(5.8,
                                ((Double) results.get(0).getValue("p99_size_mb")).doubleValue(), DELTA);
                // 99th percentile with 5 values is the 5th value: 4.4 seconds
                Assert.assertEquals(4.4,
                                ((Double) results.get(0).getValue("p99_time_sec")).doubleValue(), DELTA);
        }
}
