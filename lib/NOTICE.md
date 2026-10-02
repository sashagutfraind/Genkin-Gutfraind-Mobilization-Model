# Third-party libraries

These six jars are the unmodified libraries the released simulation jar was built and run
against in 2010–2011. They shipped with the Repast 3 distribution (files dated 16 Aug 2005)
and are kept here because Repast 3 is no longer easy to obtain. They are not part of the
model's own code and remain under their authors' licences.

| File | What it is | SHA-256 |
|---|---|---|
| `repast.jar` | Repast J 3 agent-based modelling toolkit (University of Chicago / Argonne; ROAD) | `54b71312cd884da252378ce077611c8cbbe3ee413eab5e83298f24fff8e5f257` |
| `colt.jar` | Colt scientific computing library (CERN) | `e1fcbfbdd0d0caedadfb59febace5a62812db3b9425f3a03ef4c4cbba3ed0ee3` |
| `plot.jar` | Ptolemy `ptplot` plotting package (UC Berkeley) | `4ba9b048c83af150d491b70db0a72ff714596fa3a5037b4bb2b3044186c8202f` |
| `trove.jar` | GNU Trove primitive collections | `e2bec952bf901e08fe867f3daf4d26565bbdd79185ba40704ea029dacdfaa858` |
| `beanbowl.jar` | Bean Bowl 1.3.1 (Netbreeze), used by Repast's GUI | `63980c7775924d2218a246ccfbf1a73dc928ff79868951cae6be5b74448ec512` |
| `violinstrings-1.0.2.jar` | ViolinStrings string utilities | `dc20a95ea8bb5d48c62a2852b5cbe5a31e89823de8b6c39dd189ad1d7333a842` |

The jars themselves do not embed licence files. Consult each upstream project for the
authoritative terms before redistributing them further.

The model's source imports only `uchicago.src.*` (Repast) and `cern.*` (Colt) directly;
the other four are needed by Repast at run time.

The unit tests additionally need JUnit (the `junit.framework` API). JUnit is not included;
`SETUP.md` shows how to fetch JUnit 4.8.2.
