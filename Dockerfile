# Runtime for the Genkin-Gutfraind mobilization model:
#   * a JDK for the Repast 3 simulation jar (releases/) and its libraries (lib/)
#   * a uv-managed Python 3 environment for the driver (src/2_driver) and analysis (src/3_analysis)
#
#   docker build -t mobilization-model .
#   docker run --rm -it -v "$PWD/runs:/runs" mobilization-model
#
# Java 8 is the default.  The jar was built for Java 6; to use that instead (amd64 only):
#   docker build --platform linux/amd64 --build-arg JAVA_IMAGE=azul/zulu-openjdk:6 -t mobilization-model:java6 .
ARG JAVA_IMAGE=eclipse-temurin:8-jdk
FROM ${JAVA_IMAGE}

COPY --from=ghcr.io/astral-sh/uv:0.9 /uv /uvx /usr/local/bin/

# Set WITH_LATEX=1 to add the LaTeX toolchain that the plotting scripts in src/3_analysis need (about 500 MB).
ARG WITH_LATEX=0
RUN if [ "$WITH_LATEX" = "1" ]; then \
      apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        texlive-latex-base texlive-latex-extra texlive-fonts-recommended dvipng cm-super ghostscript imagemagick \
      && rm -rf /var/lib/apt/lists/*; \
    fi

ENV UV_PROJECT_ENVIRONMENT=/opt/venv \
    UV_PYTHON_INSTALL_DIR=/opt/python \
    UV_LINK_MODE=copy \
    MPLBACKEND=Agg
WORKDIR /model
COPY pyproject.toml uv.lock .python-version ./
RUN uv sync --frozen
ENV PATH=/opt/venv/bin:$PATH

COPY . .

# simulation output is written relative to the working directory
WORKDIR /runs
CMD ["bash"]
