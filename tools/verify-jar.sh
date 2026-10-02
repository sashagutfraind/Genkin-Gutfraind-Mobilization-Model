#!/usr/bin/env bash
# Rebuilds src/1_simulation with the compiler the release was built with (Eclipse 3.6, Java 6)
# and checks that every class is byte-for-byte identical to the one inside
# releases/radicalization-3.24.3.jar.  Needs Docker and network access; changes nothing in the repo.
#
#   tools/verify-jar.sh
set -euo pipefail

repo="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

curl -fsSL -o "$work/ecj.jar"   https://archive.eclipse.org/eclipse/downloads/drops/R-3.6.2-201102101200/ecj-3.6.2.jar
curl -fsSL -o "$work/junit.jar" https://repo1.maven.org/maven2/junit/junit/4.8.2/junit-4.8.2.jar
echo "fc6b64c8fb42d20079fdb3d4921bebec51c9b33c86b23612350289b3b711d413  $work/ecj.jar"   | shasum -a 256 -c -
echo "a2aa2c3bb2b72da76c3e6a71531f1eefdc350494819baf2b1d80d7146e020f9e  $work/junit.jar" | shasum -a 256 -c -

mkdir "$work/released" "$work/rebuilt"
unzip -q "$repo/releases/radicalization-3.24.3.jar" -d "$work/released"

docker run --rm --platform linux/amd64 -v "$repo":/model:ro -v "$work":/work azul/zulu-openjdk:6 \
  java -cp /work/ecj.jar org.eclipse.jdt.internal.compiler.batch.Main \
    -1.6 -g -preserveAllLocals -nowarn -encoding ISO-8859-1 \
    -cp '/model/lib/repast.jar:/model/lib/colt.jar:/model/lib/plot.jar:/model/lib/trove.jar:/model/lib/beanbowl.jar:/model/lib/violinstrings-1.0.2.jar:/work/junit.jar' \
    -d /work/rebuilt /model/src/1_simulation/edu/cornell/rad64/NewModel.java /model/src/1_simulation/edu/cornell/rad64/NewNode.java \
    /model/src/1_simulation/edu/cornell/rad64/NewEdge.java /model/src/1_simulation/edu/cornell/rad64/NewModelTest.java \
    /model/src/1_simulation/edu/cornell/rad64/UnitTest.java

status=0
for class in NewModel NewNode NewEdge NewModelTest UnitTest; do
  if cmp -s "$work/released/edu/cornell/rad64/$class.class" "$work/rebuilt/edu/cornell/rad64/$class.class"; then
    echo "identical  $class.class"
  else
    echo "DIFFERENT  $class.class"; status=1
  fi
done
exit $status
