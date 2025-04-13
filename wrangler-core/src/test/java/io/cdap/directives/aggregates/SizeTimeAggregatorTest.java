/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.DirectiveLoadException;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link SizeTimeAggregator} functionality for aggregating data transfer
 * sizes and response times
 */
public class SizeTimeAggregatorTest {
    private static final double DELTA = 0.001; // Delta for floating point comparisons

    @Test
    public void testBasicSizeTimeAggregation() throws DirectiveParseException, DirectiveLoadException, RecipeException {
        // Base recipe for simple aggregation
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 'seconds'"
        };

        // Sample log/transaction data
        // Using 1 MB = 1024 * 1024 bytes (binary conversion)
        List<Row> rows = Arrays.asList(
                // 1.5 MB data, 2.5 seconds response time
                new Row("data_transfer_size", 1.5 * 1024 * 1024).add("response_time", 2.5 * 1000),
                // 2.5 MB data, 1.8 seconds response time
                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.8 * 1000),
                // 3.0 MB data, 3.2 seconds response time
                new Row("data_transfer_size", 3.0 * 1024 * 1024).add("response_time", 3.2 * 1000));

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify results
        Assert.assertEquals("Should produce exactly one row with results", 1, results.size());

        // Size Calculation: Sum all data_transfer_size values (1.5 + 2.5 + 3.0 = 7.0
        // MB)
        double expectedTotalSizeInMB = 7.0;
        Assert.assertEquals(expectedTotalSizeInMB,
                ((Number) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);

        // Time Calculation: Sum all response_time values (2.5 + 1.8 + 3.2 = 7.5
        // seconds)
        double expectedTotalTimeInSeconds = 7.5;
        Assert.assertEquals(expectedTotalTimeInSeconds,
                ((Number) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
    }

    @Test
    public void testDifferentOutputUnits() throws DirectiveParseException, DirectiveLoadException, RecipeException {
        String[] recipe = new String[] {
                // Output in KB and milliseconds
                "aggregate-size-time :data_transfer_size :response_time :total_size_kb :total_time_ms 'KB' 'milliseconds'"
        };

        // Sample data - consistent units for easy verification
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 1024 * 1024).add("response_time", 1000), // 1MB, 1sec
                new Row("data_transfer_size", 1024 * 1024).add("response_time", 1000) // 1MB, 1sec
        );

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());

        // 2MB = 2048 KB
        double expectedTotalSizeInKB = 2048.0;
        Assert.assertEquals(expectedTotalSizeInKB,
                ((Number) results.get(0).getValue("total_size_kb")).doubleValue(), DELTA);

        // 2 seconds = 2000 milliseconds
        double expectedTotalTimeInMs = 2000.0;
        Assert.assertEquals(expectedTotalTimeInMs,
                ((Number) results.get(0).getValue("total_time_ms")).doubleValue(), DELTA);
    }

    @Test
    public void testAverageCalculation() throws DirectiveParseException, DirectiveLoadException, RecipeException {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :avg_size_mb :avg_time_sec 'MB' 'seconds' true"
        };

        // Sample data with 3 entries
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 1 * 1024 * 1024).add("response_time", 1000), // 1MB, 1sec
                new Row("data_transfer_size", 2 * 1024 * 1024).add("response_time", 2000), // 2MB, 2sec
                new Row("data_transfer_size", 3 * 1024 * 1024).add("response_time", 3000) // 3MB, 3sec
        );

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());

        // Average: (1 + 2 + 3) / 3 = 2 MB
        double expectedAvgSizeInMB = 2.0;
        Assert.assertEquals(expectedAvgSizeInMB,
                ((Number) results.get(0).getValue("avg_size_mb")).doubleValue(), DELTA);

        // Average: (1 + 2 + 3) / 3 = 2 seconds
        double expectedAvgTimeInSeconds = 2.0;
        Assert.assertEquals(expectedAvgTimeInSeconds,
                ((Number) results.get(0).getValue("avg_time_sec")).doubleValue(), DELTA);
    }

    @Test
    public void testMixedInputUnits() throws DirectiveParseException, DirectiveLoadException, RecipeException {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 'seconds'"
        };

        // Sample data with mixed units
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "1.5MB").add("response_time", "2500ms"),
                new Row("data_transfer_size", "500KB").add("response_time", "1.8s"),
                new Row("data_transfer_size", "2MB").add("response_time", "3.2s"));

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());

        // 1.5MB + 500KB + 2MB = 4MB (500KB = 0.5MB)
        double expectedTotalSizeInMB = 4.0;
        Assert.assertEquals(expectedTotalSizeInMB,
                ((Number) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);

        // 2.5s + 1.8s + 3.2s = 7.5s (2500ms = 2.5s)
        double expectedTotalTimeInSeconds = 7.5;
        Assert.assertEquals(expectedTotalTimeInSeconds,
                ((Number) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
    }
}
