# Python 3 port of the driver and analysis scripts

The Java simulation (`src/1_simulation`) is unchanged. The Python code was written for
Python 2.4–2.7 in 2007–2011 and no longer runs on a current interpreter, so
`src/2_driver` and `src/3_analysis` hold a Python 3 port. The last Python 2 version of
every file is kept next to it in `original/`.

The port keeps the code's structure, names and comments. To see exactly what changed in a
file:

```bash
diff --strip-trailing-cr src/2_driver/original/lhsDriver.py src/2_driver/lhsDriver.py
```

## What changed everywhere

- Python 3 syntax: `print()`, `except ... as`, `raise X(...)`, `range`, `input`,
  `configparser`, `functools.reduce`, and `list(...)` around dictionary views.
- Line endings normalised to LF (the originals have Windows line endings).
- Email addresses, cluster-specific `sys.path` lines and the Parallel Python password
  were removed, in the originals as well. Apart from that the files in `original/` are
  as found.

## Driver (`src/2_driver`)

| Change | Why |
|---|---|
| Parallel Python (`pp`, `ppserver`, SSH tunnels) replaced by `concurrent.futures.ProcessPoolExecutor` | `pp` 1.5.x, the only version with the API the driver used, is unobtainable from PyPI. Simulations now run in worker processes on the local machine. |
| New option `-w <numWorkers>` and optional `numWorkers` key in `[LHSconfig]`; `serverList` is ignored | Replaces the server list. Default is one worker per CPU. Old `.ini` files still load. |
| R's `pnorm`/`qnorm` through `rpy2` replaced by `scipy.stats.norm.cdf`/`ppf` | Removes the dependency on R. |
| `round()` replaced by `roundHalfAwayFromZero()` when writing integer parameters | Python 3 rounds ties to even; Python 2 rounded them away from zero, which changes values such as 12.5. |
| Default simulation command points at `releases/radicalization-3.24.3.jar` and `lib/*` inside the repository | The original expected `radicalization.jar` and `jars/` in the working directory. `-c` still overrides it. |
| `ConfigParser` created with `inline_comment_prefixes=(';',), strict=False` | Reproduces Python 2's handling of `; comments` after values. |
| Removed: `create_simulated_job_server`, `testpp.py`, the `ppLog_*` file | Only served Parallel Python. |

The remaining helper scripts (`writeTorqueFile.sh`, `testPBS.sh`, `runrepast.sh`,
`past32.sh`, `listMissing.sh`, `fixOutputDir.sh`) apply only to the 2010 Parallel Python
and Torque setup and live in `original/`. The scripts that started and stopped Parallel
Python servers on specific cluster machines are not published.

`abmdriver.py`, the older single-parameter scan driver, received the syntax port only.
Its regression step needs the long-obsolete `rpy` package and is skipped when that is
absent, exactly as before, and its plots look for metric names that version 3.24.3 of the
simulation no longer writes.

## Analysis (`src/3_analysis`)

| Change | Why |
|---|---|
| `pylab.hold(...)` calls removed | Removed from matplotlib 3; holding is the default. |
| `bar(left=...)` → `bar(x=...)`, `normed=True` → `density=True`, `rotation='90'` → `rotation=90` | Renamed or tightened matplotlib/NumPy arguments. |
| CSV files opened in text mode (`'r', newline=''`) | Required by the Python 3 `csv` module. |
| `np.oldnumeric.load` → `pickle.load` in `plot_elasticities.py` | `oldnumeric` no longer exists. |
| `sps.mannwhitneyu(a, b)` → `sps.mannwhitneyu(a, b, alternative='two-sided', method='asymptotic')[1]/2.` | SciPy used to return the one-sided asymptotic p-value; it now defaults to two-sided and may use an exact method. |
| `sps.ks_2samp(a, b)` → `method='asymp'` in `scenario_analysis.py`; `ks_2samp_legacy()` in `count_avg_cell.py` | SciPy changed how the Kolmogorov–Smirnov p-value is computed. For the small samples in `count_avg_cell.py` only the old formula reproduces the old numbers. |

## How the port was checked

- **Sampling.** `lhsDriver.py -m g` on `lhs_big_boy.ini` was compared with the 6,000 job
  files of 3 August 2010 in `results/lhs_runconfig_2010_08_03.zip`. For 33 of the 34
  parameters the port produces exactly the same 6,000 values. The exception is `Rngseed`,
  which is drawn by shuffling a repeated list and so depends on shuffle order. Which
  value lands in which sample differs in any case: the pairing comes from a random
  shuffle whose order depended on Python 2's dictionary ordering.
- **Running.** A four-sample batch (`lhs_smoke.ini`) runs end to end — generate, simulate
  in parallel, analyse — under both the original driver (Python 2.7, `pp` 1.5.7, R) and
  the port, each driving the released jar.
- **Analysis.** `scenario_analysis.py`, `count_avg_cell.py`, `plot_magnets.py` and
  `plot_elasticities2.py` were run under Python 2.7 and Python 3.12 on the same inputs.
  All printed statistics and the generated `radicalizationReport_*.csv` agree (largest
  relative difference 7e-16). `drop.py` runs and writes its figure.
- **Not checked.** `plot_elasticities.py` needs pickles from `abmdriver.py`'s regression
  step, which cannot be produced without `rpy`; it only passes a syntax check.
