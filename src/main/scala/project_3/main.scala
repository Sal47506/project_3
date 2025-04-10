package project_3

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import org.apache.spark.storage.StorageLevel
import org.apache.log4j.{Level, Logger}

object main {
  // Set root logger to ERROR
  val rootLogger = Logger.getRootLogger()
  rootLogger.setLevel(Level.ERROR)

  // Set Spark loggers to WARN
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.spark-project").setLevel(Level.WARN)
  Logger.getLogger("project_3").setLevel(Level.INFO)

  def LubyMIS(g_in: Graph[Int, Int]): (Graph[Int, Int], Int) = {
    var g = g_in.mapVertices((id, attr) => 0) // 0: undecided, 1: in MIS, -1: not in MIS
    var remaining_vertices = g.vertices.count()
    var iterations = 0
    
    while (remaining_vertices > 0) {
      iterations += 1
      println(s"Iteration $iterations: $remaining_vertices vertices remaining")
      // Generate random numbers for undecided vertices
      val random_g = g.mapVertices((id, attr) => 
        if (attr == 0) scala.util.Random.nextDouble() else -1.0
      )
      
      // Compare random values with neighbors and send messages
      val messages = random_g.aggregateMessages[Boolean](
        triplet => {
          if (triplet.srcAttr >= 0 && triplet.dstAttr >= 0) {
            if (triplet.srcAttr > triplet.dstAttr) {
              triplet.sendToDst(false)
            } else if (triplet.srcAttr < triplet.dstAttr) {
              triplet.sendToSrc(false)
            }
          }
        },
        (a, b) => a || b
      )
      
      // Update vertex states
      g = g.outerJoinVertices(messages) {
        case (id, oldAttr, Some(msg)) => 
          if (oldAttr == 0 && !msg) 1 // Add to MIS if not blocked by neighbor
          else oldAttr
        case (id, oldAttr, None) => 
          if (oldAttr == 0) 1 // Isolated vertices join MIS
          else oldAttr
      }
      
      // Mark neighbors of MIS vertices as not in MIS
      val neighbors = g.aggregateMessages[Int](
        triplet => {
          if (triplet.srcAttr == 1) triplet.sendToDst(-1)
          if (triplet.dstAttr == 1) triplet.sendToSrc(-1)
        },
        (a, b) => -1
      )
      
      g = g.outerJoinVertices(neighbors) {
        case (id, oldAttr, Some(-1)) => -1 
        case (id, oldAttr, _) => oldAttr 
      }
      
      remaining_vertices = g.vertices.filter(_._2 == 0).count()
    }
    (g, iterations)
  }

  def verifyMIS(g_in: Graph[Int, Int]): Boolean = {
    // Check independence (no two adjacent vertices in MIS)
    val notAdjacent = g_in.triplets.map(triplet => 
      !(triplet.srcAttr == 1 && triplet.dstAttr == 1)
    ).reduce(_ && _)

    // Check maximality (every vertex is either in MIS or adjacent to MIS)
    val allCovered = g_in.aggregateMessages[Boolean](
      triplet => {
        if (triplet.srcAttr == 1) {
          triplet.sendToDst(true)
        }
        if (triplet.dstAttr == 1) {
          triplet.sendToSrc(true)
        }
      },
      _ || _
    )

    val verticesCovered = g_in.vertices.leftJoin(allCovered) {
      case (id, attr, Some(covered)) => attr == 1 || covered
      case (id, attr, None) => attr == 1
    }.map(_._2).reduce(_ && _)

    notAdjacent && verticesCovered
  }

  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("project_3")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()
    if(args.length == 0) {
      println("Usage: project_3 option = {compute, verify}")
      sys.exit(1)
    }
    if(args(0)=="compute") {
      if(args.length != 3) {
        println("Usage: project_3 compute graph_path output_path")
        sys.exit(1)
      }
      val startTimeMillis = System.currentTimeMillis()
      val edges = sc.textFile(args(1)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
      val g = Graph.fromEdges[Int, Int](edges, 0, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)
      val (g2, iterations) = LubyMIS(g)

      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
      println("==================================")
      println("Luby's algorithm completed in " + durationSeconds + "s.")
      println("==================================")
      val g2df = spark.createDataFrame(g2.vertices)
      g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(2))
    }
    else if(args(0)=="verify") {
      if(args.length != 3) {
        println("Usage: project_3 verify graph_path MIS_path")
        sys.exit(1)
      }

      val edges = sc.textFile(args(1)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
      val vertices = sc.textFile(args(2)).map(line => {val x = line.split(","); (x(0).toLong, x(1).toInt) })
      val g = Graph[Int, Int](vertices, edges, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)

      val ans = verifyMIS(g)
      if(ans)
        println("Yes")
      else
        println("No")
    }
    else
    {
        println("Usage: project_3 option = {compute, verify}")
        sys.exit(1)
    }
  }
}
