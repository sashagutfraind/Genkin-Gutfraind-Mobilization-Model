# Setup

Two ways to get a working environment:

- **Docker** — one image with Java 8 and the Python environment. Nothing else to install.
  This is the tested route.
- **Native** — a Java 8 JDK plus [uv](https://docs.astral.sh/uv/) for Python.

Commands assume you are in the repository root.

## Installing

### Docker

```bash
docker build -t mobilization-model .
```

The image puts the repository at `/model` and starts in `/runs`, where simulations write
their output. Mount a host directory there to keep it:

```bash
mkdir -p runs
docker run --rm -it -v "$PWD/runs:/runs" mobilization-model
```

Two build options:

| Option | Effect |
|---|---|
| `--build-arg WITH_LATEX=1` | Adds LaTeX, dvipng and ImageMagick, which the plotting scripts in `src/3_analysis` need. About 500 MB larger. |
| `--platform linux/amd64 --build-arg JAVA_IMAGE=azul/zulu-openjdk:6` | Uses Java 6, the version the jar was built for, instead of Java 8. On Apple Silicon this runs under emulation. |

### Native

1. **Java.** Install a Java 8 JDK and check that `java -version` reports 1.8. A JRE is
   enough unless you want to compile.
2. **Python.** Install uv, then create the environment from the lock file:

   ```bash
   uv sync
   ```

   This installs Python 3.12 with NumPy, SciPy, matplotlib and NetworkX into `.venv/`.
   Prefix Python commands with `uv run`.
3. **LaTeX** (analysis figures only). The plotting scripts set `text.usetex`, so they
   need `latex`, `dvipng` and the `cm-super` fonts; `count_avg_cell.py` and
   `scenario_analysis.py` also call ImageMagick's `convert` to make PNG copies.

In a native setup, replace `/model/` in the commands below with the path to the
repository.

## Running

### One simulation

```bash
java -cp "/model/releases/radicalization-3.24.3.jar:/model/lib/*" \
  edu.cornell.rad64.NewModel -b /model/src/2_driver/sample1.pf -NS
```

- `-b <file>` runs in batch mode from a Repast parameter file.
- `-NS` means the file describes a single simulation. Without it the file is treated as
  a scan over one parameter, as in `src/2_driver/batchfiles/*.pf`.
- With no arguments the model opens the Repast GUI. That needs a display and was not
  tested in this reconstruction.

Output goes to the directory named by `outputDirectory` in the parameter file, relative
to the working directory:

| File | Contents |
|---|---|
| `<timestamp>.raw.csv` | Header with the parameter values, then one row of metrics per tick. |
| `<timestamp>.csv` | Summary of the run. |
| `full_network_<timestamp>.DL` | Snapshot of the social network. |

`src/1_simulation/edu/cornell/rad64/README.txt` defines every parameter and metric.

`java -jar releases/radicalization-3.24.3.jar` only works if the libraries sit in a
`jars/` directory next to the jar, as its manifest expects. The `-cp` form above avoids
that.

### A Latin Hypercube batch

A batch is described by an `.ini` file: an `[LHSconfig]` section, then one section per
simulation parameter giving its type and distribution. `src/2_driver/manual.txt` explains
the design.

```bash
python /model/src/2_driver/lhsDriver.py -m b -b /model/src/2_driver/lhs_smoke.ini
```

`lhs_smoke.ini` is a four-run check that finishes in seconds. The configurations used for
the research are alongside it:

| File | Samples | Notes |
|---|---|---|
| `lhs_big_boy12_transitivity.ini` | 6,000 | Latest (February 2011). |
| `lhs_big_boy12.ini`, `lhs_big_boy.ini` | 6,000 | December and August 2010. |
| `lhs_thin_man.ini` | 3,000 | December 2010. |
| `lhs.ini`, `lhs_sd1.ini`, `lhs_sd2.ini` | 6,000 / 6,000 / 3,000 | Earlier 2010 batches. |

The full configurations simulate populations of up to 50,000 agents for 1,000 ticks,
6,000 times. Copy one into your run directory and lower `numSamples` before trying it.

Modes (`-m`), which can be combined:

| Mode | Does |
|---|---|
| `b` | Everything: read the `.ini`, generate samples, run them, analyse. |
| `g` | Generate the job files (`.pf`) and a `jobList_<timestamp>` only. |
| `j` | Run the jobs listed in `-f <jobList>`; needs `-d <dataDir>`. |
| `a` | Collect finished runs in `-d <dataDir>` into a `batchDossier_t=<timestamp>.csv`. |

Other options:

| Option | Does |
|---|---|
| `-w <n>` | Number of simulations to run at once. Default: `numWorkers` in the `.ini`, else one per CPU. Large populations need memory; lower this if runs fail. |
| `-c '<command>'` | Command used to start one simulation, with `%s` where the job file goes. Use it to add JVM flags such as `-Xmx2G`. |

Failed jobs are listed in `<dataDir>/failed_<timestamp>.txt`, one job file per line, so
the file can be passed back with `-m j -f`. Each job's console output is in
`<job>.pf_out.txt`.

If `dataDir` already exists the driver asks before continuing, so run it with `-it`
under Docker.

### Analysis

The scripts are run from `src/3_analysis` and write into `output/` there:

```bash
cd /model/src/3_analysis && mkdir -p output
python scenario_analysis.py
```

| Script | Produces | Reads |
|---|---|---|
| `scenario_analysis.py` | Which parameters drive each mobilization scenario: `radicalizationReport_*.csv` and the effect-size bar chart. | `../batchDossier_t=2011_02_16__transitivity.csv` |
| `count_avg_cell.py` | Simulated against empirical cell-size distributions, with Kolmogorov–Smirnov and Mann–Whitney tests. | The same dossier and `terroristcelllist4.19.csv` |
| `plot_magnets.py` | Cell size against the number of magnets. | The same dossier |
| `plot_elasticities2.py` | Elasticity bar chart. | `../rankIsolation.csv`, `../rankClustering.csv`, `../rankAvgCell.csv` |
| `drop.py` | Drop-line chart of three metrics. | `data.csv` |
| `plot_elasticities.py` | Older elasticity chart from `abmdriver.py` pickles (`-d <dir>`). | Not runnable today; see `PORTING.md`. |

None of these input files is included in the repository. To analyse a table from
`results/` or one you generated, edit the file name passed to `loadData` or
`loadCellCounts` at the bottom of the script.

## Building from source

Compile against the libraries in `lib/`. The test classes need JUnit:

```bash
curl -fsSL -o junit-4.8.2.jar https://repo1.maven.org/maven2/junit/junit/4.8.2/junit-4.8.2.jar
mkdir -p build
javac -nowarn -source 1.6 -target 1.6 -encoding ISO-8859-1 \
  -cp "/model/lib/*:junit-4.8.2.jar" -d build /model/src/1_simulation/edu/cornell/rad64/*.java

java -cp "build:/model/lib/*" edu.cornell.rad64.NewModel -b /model/src/2_driver/sample1.pf -NS
```

`javac` produces working classes, but not the same bytes as the release, which was built
in Eclipse. To confirm that the source is exactly what was released, run:

```bash
tools/verify-jar.sh
```

It compiles with the Eclipse 3.6.2 compiler on Java 6 inside Docker and compares each
class with the one in the jar. Run it on the host, not inside the container.

### Unit tests

```bash
java -cp "/model/releases/radicalization-3.24.3.jar:/model/lib/*:junit-4.8.2.jar" \
  junit.textui.TestRunner edu.cornell.rad64.UnitTest

java -cp "/model/releases/radicalization-3.24.3.jar:/model/lib/*:junit-4.8.2.jar" \
  junit.textui.TestRunner edu.cornell.rad64.NewModelTest
```

`UnitTest` takes under a second. `NewModelTest` runs full simulations and took about
three minutes on Java 6 under emulation; because it asserts the direction of effects in
stochastic runs, an occasional failure does not mean the build is broken.

## What was tested

| Step | Environment | Result |
|---|---|---|
| Run the released jar in batch mode | Java 8 (arm64), Java 6 (amd64, emulated) | Works |
| Compile the source with `javac` and run it | Java 8 | Works |
| Rebuild with the Eclipse 3.6.2 compiler and compare to the jar | Java 6 | All 5 classes identical |
| `UnitTest` | Java 6 | 6 of 6 pass |
| `NewModelTest` | Java 6 | 18 of 19 pass; `testAvgPressurability` failed in the one run made |
| Python 3 driver, four-sample batch end to end | Docker image | Works |
| Python 3 driver, sample generation for 6,000 samples | uv on macOS | Matches the 2010 job files (see `PORTING.md`) |
| Python 3 analysis scripts | Docker image with `WITH_LATEX=1` | Match the Python 2 output |
| Interactive GUI | — | Not tested |
| A full 6,000-sample batch | — | Not run |
