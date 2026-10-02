# Genkin–Gutfraind Mobilization Model

An agent-based model of how radicalized individuals find each other and assemble into
cells, with the tools used to run it at scale and analyse the results. This repository is
a reconstruction of the research code behind:

> Michael Genkin and Alexander Gutfraind (2011). *How Do Terrorist Cells Self-Assemble:
> Insights from an Agent-Based Model of Radicalization.* SSRN working paper 1031521.
> <https://papers.ssrn.com/sol3/papers.cfm?abstract_id=1031521>

The model simulates a community of individuals who form and drop friendships under
homophily, peer pressure, attrition and shared venues ("magnets"). It measures how the
radical subpopulation organises: how isolated it is, how clustered, and how large its
connected cells become. The paper uses 6,000 Latin Hypercube samples of the parameter
space to rank which social factors drive each mobilization scenario.

## What is here

| Path | Contents |
|---|---|
| `releases/radicalization-3.24.3.jar` | The simulation as built on 7 July 2011 (version "REM 3.24.3"). The class files are the original ones; see "What was removed" below. |
| `lib/` | The six third-party jars it runs against (Repast 3, Colt and others). See `lib/NOTICE.md`. |
| `src/1_simulation/` | Java source of the simulation, unchanged. Parameters and metrics are documented in `edu/cornell/rad64/README.txt`; history in `releaseNotes.txt`. |
| `src/2_driver/` | Latin Hypercube Sampling driver: generates parameter samples, runs the simulations in parallel and collects one summary row per run. Python 3 port. |
| `src/3_analysis/` | Scripts that turn the summary table into the paper's statistics and figures. Python 3 port. |
| `src/*/original/` | The last Python 2 version of each driver and analysis file, kept for reference. |
| `results/` | Archived run configuration and summary tables (see below). |
| `Dockerfile`, `pyproject.toml`, `uv.lock` | Reproducible runtime: Java 8 plus a uv-managed Python 3 environment. |
| `tools/verify-jar.sh` | Rebuilds the source with the original compiler and compares it to the released jar. |

`SETUP.md` covers installing and running. `PORTING.md` lists every change made in the
Python 3 port.

## Quick start

With Docker installed:

```bash
docker build -t mobilization-model .
mkdir -p runs

# one simulation from a parameter file; output appears in runs/lhs/
docker run --rm -v "$PWD/runs:/runs" mobilization-model \
  java -cp "/model/releases/radicalization-3.24.3.jar:/model/lib/*" \
  edu.cornell.rad64.NewModel -b /model/src/2_driver/sample1.pf -NS

# a four-sample Latin Hypercube batch; output appears in runs/lhs_smoke/
docker run --rm -v "$PWD/runs:/runs" mobilization-model \
  python /model/src/2_driver/lhsDriver.py -m b -b /model/src/2_driver/lhs_smoke.ini
```

The batch ends by writing `batchDossier_t=<timestamp>.csv`: one row per simulation with
its parameter values, then the mean and standard deviation of every metric after burn-in.

## Provenance

The code was recovered from a Subversion working copy whose server no longer exists.

- **The source matches the released jar exactly.** Compiling `src/1_simulation` with the
  Eclipse 3.6 compiler for Java 6 reproduces all five classes in the jar byte for byte.
  `tools/verify-jar.sh` repeats that check.
- Source and jar were committed together as revision 705 on 7 July 2011, the date on the
  working paper. No later version of the simulation was found.
- The main analysis scripts (`scenario_analysis.py`, `count_avg_cell.py`,
  `plot_magnets.py`) are the latest copies found, from October–November 2011, a few
  months newer than their last commit.
- Older material was left out: earlier jars (2007–2010), the 2007 driver, a duplicate
  source tree under the model's earlier package name `hopfieldZeal`, and superseded
  batch files.

## What was removed

The archive was cleaned of details of the machines it was developed on:

- Version-control metadata and the Eclipse `.classpath`, from the source tree and from
  inside the jar. The jar's class files were not touched and still match the source
  byte for byte.
- The scripts that started and stopped Parallel Python servers on university clusters,
  and two 2009 test configurations that named those servers.
- Email addresses, cluster-specific Python paths, a shared Parallel Python password, and
  a commented-out line of empirical data.

## Results

| File | Contents |
|---|---|
| `results/lhs_runconfig_2010_08_03.zip` | The 6,000 generated parameter files, their job list and the `.ini` that defined the batch of 3 August 2010. Inputs only. Paths in the job list were made relative. |
| `results/batchDossier_t=2010_12_17.csv` | Summary table of a 6,000-run batch (December 2010): 6,000 rows of parameters and metric means and standard deviations. |
| `results/batchDossier_t=2010_12_24__thin_man.csv` | Summary table of a 3,000-run batch (December 2010); the name matches `lhs_thin_man.ini`. |

## Things to know before relying on it

- **Runs are not reproducible from the seed.** Two runs of the released jar with the same
  parameter file and the same `Rngseed` gave different metric values. Results are
  statistical; compare distributions, not individual runs.
- **The analysis scripts need inputs that are not in this repository.** As written they
  read `../batchDossier_t=2011_02_16__transitivity.csv` (the February 2011 batch),
  `../rank*.csv`, and the empirical cell list `terroristcelllist4.19.csv`. To use one of
  the tables in `results/`, change the file name at the bottom of the script.
- **One statistical unit test can fail by chance.** `UnitTest` passed 6 of 6.
  `NewModelTest` checks directional effects on stochastic runs; in the one full run made
  here 18 of 19 passed and `testAvgPressurability` failed.
- **The simulation needs Java 8 or older.** It was built for Java 6 and was run here on
  Java 6 and Java 8. Newer Java versions were not tried.
- **The interactive GUI was not tested.** Only batch mode (`-b`) was run.
- `abmdriver.py`, the older single-parameter driver, still runs the simulation, but its
  plots look for metric names that version 3.24.3 no longer writes.

## Licence

BSD for the simulation; see `LICENSE` for the full text and for the statements carried by
individual driver and analysis files. Third-party libraries in `lib/` keep their own
licences.
