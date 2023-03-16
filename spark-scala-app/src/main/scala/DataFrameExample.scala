import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.avro.functions.{from_avro}
import java.nio.file.{Files, Paths}

object DataFrameExample extends Serializable {
  def main(args: Array[String]) = {
    val spark = SparkSession
      .builder()
      .appName("Spark Example")
      .config("spark.sql.warehouse.dir", "/user/hive/warehouse")
      .getOrCreate()

    import spark.implicits._

    spark.sparkContext.setLogLevel("WARN");

    val KAFKA_BOOTSTRAP_SERVERS = "localhost:9091,localhost:9093,localhost:9094"
    val TOPIC = "test-person-activity-partitions-replication-qwe"

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
      .option("startingOffsets", "earliest")
      .option("subscribe", TOPIC)
      .load()

    val jsonFormatSchema = new String(
      Files.readAllBytes(
        Paths.get("./src/main/resources/avro/person-activity.avsc")
      )
    )

    val df2 = df
      .selectExpr("substring(value, 6) as avro_value", "key")
      .select(
        df.col("key").cast("string"),
        from_avro($"avro_value", jsonFormatSchema).as("value")
      )
      .select(
        $"value.uid",
        $"value.timestamp".cast("timestamp").alias("timestamp"),
        $"value.activity",
        $"key"
      )

    val windowedCounts = df2
      .withWatermark("timestamp", "1 minute")
      .groupBy(
        window($"timestamp", "30 seconds", "10 seconds"),
        $"key",
        $"activity"
      )
      .count()

    windowedCounts.writeStream
      .format("console")
      .outputMode("append")
      .option("truncate", "false")
      .start()
      .awaitTermination()
  }
}
