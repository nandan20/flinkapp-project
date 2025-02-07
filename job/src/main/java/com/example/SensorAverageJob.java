package com.example;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class SensorAverageJob {

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Force the entire job to use parallelism of 1
        env.setParallelism(1);

        // Connect to the sensor data generator using the Docker Compose service name "generator"
        DataStream<String> sensorData = env.socketTextStream("generator", 9999);

        // Parse each line (expected format: sensorId,temperature) into a Tuple3: (sensorId, sum, count)
        DataStream<Tuple3<String, Double, Integer>> parsedData = sensorData.flatMap(new FlatMapFunction<String, Tuple3<String, Double, Integer>>() {
            @Override
            public void flatMap(String value, Collector<Tuple3<String, Double, Integer>> out) {
                try {
                    String[] parts = value.split(",");
                    if (parts.length == 2) {
                        String sensorId = parts[0].trim();
                        double temperature = Double.parseDouble(parts[1].trim());
                        out.collect(new Tuple3<>(sensorId, temperature, 1));
                    }
                } catch (Exception e) {
                    // Ignore malformed lines
                }
            }
        });

        // Aggregate per sensor using a reduce function within a tumbling window of 5 seconds
        DataStream<Tuple3<String, Double, Integer>> aggregated = parsedData
            .keyBy(tuple -> tuple.f0)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .reduce(new ReduceFunction<Tuple3<String, Double, Integer>>() {
                @Override
                public Tuple3<String, Double, Integer> reduce(Tuple3<String, Double, Integer> value1, Tuple3<String, Double, Integer> value2) {
                    return new Tuple3<>(value1.f0, value1.f1 + value2.f1, value1.f2 + value2.f2);
                }
            });

        // Compute and print the average temperature per sensor
        aggregated.map(tuple -> {
            String sensorId = tuple.f0;
            double average = tuple.f1 / tuple.f2;
            return sensorId + " average temperature: " + average;
        }).print();

        // Execute the Flink streaming job
        env.execute("Sensor Average Temperature Calculation");
    }
}
