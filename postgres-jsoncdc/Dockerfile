ARG PG_VERSION

FROM postgres:${PG_VERSION}

ENV CARGO_HOME /cargo
ENV PATH $CARGO_HOME/bin:$PATH
ENV SRC_PATH /src

RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    ca-certificates curl git make gcc gcc-multilib postgresql-server-dev-$PG_MAJOR=$PG_VERSION \
    python-pip python-setuptools \
  && curl -sf https://static.rust-lang.org/rustup.sh -o rustup.sh \
  && bash rustup.sh --disable-sudo -y --verbose \
  && pip install pgxnclient \
  && cargo install rustfmt \
  && mkdir -p "$CARGO_HOME" \
  && mkdir -p "$SRC_PATH" \
  && git clone https://github.com/posix4e/jsoncdc.git "$SRC_PATH/jsoncdc" \
  && make -C "$SRC_PATH/jsoncdc" install \
  && bash rustup.sh --disable-sudo -y --verbose --uninstall \
  && rm -f rustup.sh \
  && apt-get purge -y \
    ca-certificates curl git make gcc gcc-multilib postgresql-server-dev-$PG_MAJOR=$PG_VERSION \
    python-pip python-setuptools \
  && apt autoremove -y \
  && rm -rf /var/lib/apt/lists/*

COPY conf/postgresql.conf /src/postgresql.conf
COPY scripts/configure.sh /docker-entrypoint-initdb.d/configure.sh

