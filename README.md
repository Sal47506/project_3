# Large Scale Data Processing: Project 3
# Collaborators: Salamun Nuhin, Caden Parajuli, Tian-hao Zhang 
## Getting started
Head to [Project 1](https://github.com/CSCI3390Spring2025/project_1) if you're looking for information on Git, template repositories, or setting up your local/remote environments.

## Spark's GraphX API
This project will familiarize you with the [GraphX API](https://spark.apache.org/docs/latest/graphx-programming-guide.html) offered by Spark.  

You'll be implementing Luby's maximal independent set (MIS) algorithm and a program to verify MIS. In the program skeleton of `main.scala`, the corresponding functions are `LubyMIS` and `verifyMIS`, respectively.  

## Relevant data

You can find the TAR file containing Project 3's data (12 CSV files) [here](https://drive.google.com/file/d/1lBEztkL5mikmiLQI2-QrwJPwJP-R8g7v/view?usp=sharing). Download and expand the TAR file for local processing. For processing in the cloud, refer to the steps for creating a storage bucket in [Project 1](https://github.com/CSCI3390Spring2025/project_1) and upload `twitter_original_edges.csv`, the file you'll need to access in GCP.

`twitter_original_edges.csv` contains the social network graph of Twitter (~1.32 GB). You've also been provided truncated versions of the graph in files with the naming convention `twitter_x_edges.csv`, where the graph in that file consists of the first `x` vertices (e.g. `twitter_100_edges.csv` holds the first 100 vertices of the graph). In addition, you'll find some smaller graphs meant to expediate testing (`small_edges.csv` and `line_100_edges.csv`).  

## Calculating and reporting your findings
You'll be submitting a report along with your code that provides commentary on the tasks below.  

1. **(4 points)** Implement the `verifyMIS` function. The function accepts a Graph[Int, Int] object as its input. Each vertex of the graph is labeled with 1 or -1, indicating whether or not a vertex is in the MIS. `verifyMIS` should return `true` if the labeled vertices form an MIS and `false` otherwise. To execute the function, run the following:
```
// Linux
spark-submit --class project_3.main --master local[*] target/scala-2.12/project_3_2.12-1.0.jar verify [path_to_graph] [path_to_MIS]

// Unix
spark-submit --class "project_3.main" --master "local[*]" target/scala-2.12/project_3_2.12-1.0.jar verify [path_to_graph] [path_to_MIS]
```
Apply `verifyMIS` locally with the parameter combinations listed in the table below and **fill in all blanks**.
|        Graph file       |           MIS file           | Is an MIS? |
| ----------------------- | ---------------------------- | ---------- |
| small_edges.csv         | small_edges_MIS.csv          | Yes        |
| small_edges.csv         | small_edges_non_MIS.csv      | No         |
| line_100_edges.csv      | line_100_MIS_test_1.csv      | Yes        |
| line_100_edges.csv      | line_100_MIS_test_2.csv      | No         |
| twitter_10000_edges.csv | twitter_10000_MIS_test_1.csv | No         |
| twitter_10000_edges.csv | twitter_10000_MIS_test_2.csv | Yes        |

2. **(3 points)** Implement the `LubyMIS` function. The function accepts a Graph[Int, Int] object as its input. You can ignore the two integers associated with the vertex RDD and the edge RDD as they are dummy fields. `LubyMIS` should return a Graph[Int, Int] object such that the integer in a vertex's data field denotes whether or not the vertex is in the MIS, with 1 signifying membership and -1 signifying non-membership. The output will be written as a CSV file to the output path you provide. To execute the function, run the following:
```
// Linux
spark-submit --class project_3.main --master local[*] target/scala-2.12/project_3_2.12-1.0.jar compute [path_to_input_graph] [path_for_output_graph]

// Unix
spark-submit --class "project_3.main" --master "local[*]" target/scala-2.12/project_3_2.12-1.0.jar compute [path_to_input_graph] [path_for_output_graph]
```
Apply `LubyMIS` locally on the graph files listed below and report the number of iterations and running time that the MIS algorithm consumes for **each file**. You may need to include additional print statements in `LubyMIS` in order to acquire this information. Finally, verify your outputs with `verifyMIS`.
| Graph File               | # Iterations | Running Time (sec)  |
|--------------------------|--------------|---------------------|
| `small_edges.csv`        |  1           |    2                |
| `line_100_edges.csv`     |  3           |    2                |
| `twitter_100_edges.csv`  |  2           |    2                |
| `twitter_1000_edges.csv` |  3           |    2                |
| `twitter_10000_edges.csv`|  3           |    3                |

3. **(3 points)**  
a. Run `LubyMIS` on `twitter_original_edges.csv` in GCP with 3x4 cores (vCPUs). Report the number of iterations, running time, and remaining active vertices (i.e. vertices whose status has yet to be determined) at the end of **each iteration**. You may need to include additional print statements in `LubyMIS` in order to acquire this information. Finally, verify your outputs with `verifyMIS`.  

3x4 vCPUs:

```
1 iterations complete in 61 sec. 7278819 vertices remaining
2 iterations complete in 73 sec. 37222 vertices remaining
3 iterations complete in 83 sec. 397 vertices remaining
4 iterations complete in 95 sec. 3 vertices remaining
5 iterations complete in 112 sec. 0 vertices remaining
==================================
Luby's algorithm completed in 113s.
Number of iterations: 5
==================================
```

b. Run `LubyMIS` on `twitter_original_edges.csv` with 4x2 cores (vCPUs) and then 2x2 cores (vCPUs). Compare the running times between the 3 jobs with varying core specifications that you submitted in **3a** and **3b**.

4x2 vCPUs:

```
1 iterations complete in 111 sec. 6824685 vertices remaining
2 iterations complete in 140 sec. 31898 vertices remaining
3 iterations complete in 177 sec. 373 vertices remaining
4 iterations complete in 207 sec. 0 vertices remaining
==================================
Luby's algorithm completed in 208s.
Number of iterations: 4
==================================
```

2x2 vCPUs:

```
1 iterations complete in 154 sec. 6923135 vertices remaining
2 iterations complete in 199 sec. 34474 vertices remaining
3 iterations complete in 240 sec. 484 vertices remaining
4 iterations complete in 279 sec. 1 vertices remaining
5 iterations complete in 320 sec. 0 vertices remaining
==================================
Luby's algorithm completed in 321s.
Number of iterations: 5
==================================
```

## Run Time Comparison of LubyMIS for the three configurations

| Configuration | Number of Cores | Iterations | Total Time (s) | Notes                                      |
|---------------|------------------|------------|----------------|---------------------------------------------|
| 3x4 vCPUs     | 12 cores         | 5          | 113s           | Fastest overall                             |
| 4x2 vCPUs     | 8 cores          | 4          | 208s           | Fewer iterations, but slower per iteration  |
| 2x2 vCPUs     | 4 cores          | 5          | 321s           | Slowest configuration                       |

---

### Observations

- The 3x4 vCPUs (12 cores) configuration was fastest
- Despite only needing 4 iterations, the 4x2 vCPUs (8 cores) configuration was slower
- The 2x2 vCPUs (4 cores) configuration had the longest execution time at 321 seconds, showing a clear slow-down due to fewer cores.
- More cores enable better parallelism, reducing total runtime even if the number of iterations remains the same.

---

### Main take-away

Higher number of cores improve the performance of LubyMIS on large graphs like Twitter. The 3x4 configuration demonstrates the benefits of parallelism, making it the best configuration out of the three.


## Submission via GitHub
Delete your project's current **README.md** file (the one you're reading right now) and include your report as a new **README.md** file in the project root directory. Have no fear—the README with the project description is always available for reading in the template repository you created your repository from. For more information on READMEs, feel free to visit [this page](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/about-readmes) in the GitHub Docs. You'll be writing in [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown). Be sure that your repository is up to date and you have pushed all changes you've made to the project's code. When you're ready to submit, simply provide the link to your repository in the Canvas assignment's submission.

## You must do the following to receive full credit:
1. Create your report in the ``README.md`` and push it to your repo.
2. In the report, you must include your (and your group member's) full name in addition to any collaborators.
3. Submit a link to your repo in the Canvas assignment.

## Late submission penalties
Please refer to the course policy.
