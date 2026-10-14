# HTTP network timeout tests

This laboratory runs `ConnectTimeoutNetworkIT` against the real Apache HTTP client and the Linux TCP stack. It checks TCP connection timeouts, cancellation by the overall HTTP request deadline, and recovery using the same client after each failure.

## Execution model

`ConnectTimeoutNetworkIT` is compiled with the other Java test sources, but its `IT` suffix excludes it from the current default Surefire test selection. It therefore does not run during ordinary `mvn test` or `mvn clean verify` builds.

Execution requires both:

- Explicit selection with `-Dtest=ConnectTimeoutNetworkIT`.
- The test-only environment variable `PDF_ENGINE_NETWORK_TESTS=1`.

The Compose service supplies both. Selecting the class without the environment variable skips it; a successful build with skipped tests does not validate the network behavior. The variable is not an application configuration setting.

## Prerequisites

- A running Docker Engine on Linux, or Docker Desktop on Windows/macOS, with Docker Compose v2 and Linux containers.
- Permission to start a container with the `NET_ADMIN` capability, needed for its local `iptables` rules.
- JDK 17 and Maven on the host for the initial standard build.
- Dependencies and Maven plugins downloaded by a successful host build into the Maven repository mounted at `/maven-cache`.
- Network access while building the image, to obtain the Maven base image and install `iptables`.

The test container runs Maven offline (`-o`). Its runtime network contains only loopback, so missing dependencies cannot be downloaded during the test run.

### Maven cache path

The current `compose.yaml` uses `${USERPROFILE}/.m2/repository` as the host cache path. Windows normally defines `USERPROFILE`; the Linux/macOS commands below supply it only to each Compose invocation, using the existing `$HOME` value.

| Host | Default local Maven repository |
|---|---|
| Windows | `%USERPROFILE%\.m2\repository` |
| Linux / macOS | `$HOME/.m2/repository` |

This allows the same Compose file to be used on all three operating systems. If Maven uses a custom local repository, adjust the `/maven-cache` source in `compose.yaml` to its actual absolute path before running the laboratory.

## Run from Windows CMD

Run all commands from the repository root. Each command is a complete line.

First, populate the Maven cache and verify the ordinary Java tests:

```cmd
mvn test
```

Inspect the resolved mounts and build the laboratory image:

```cmd
docker compose -f docker/network-tests/compose.yaml config
docker compose -f docker/network-tests/compose.yaml build
```

Run the network tests:

```cmd
docker compose -f docker/network-tests/compose.yaml run --rm -T tests
```

## Run from Linux or macOS (Bash / Zsh)

Run all commands from the repository root. On macOS, start Docker Desktop first; the tests execute inside its Linux container, so `iptables` does not need to be installed on macOS. On Linux, ensure that the Docker daemon is running and accessible to your current user.

First, populate the Maven cache and verify the ordinary Java tests:

```bash
mvn test
```

Inspect the resolved mounts and build the laboratory image:

```bash
env USERPROFILE="$HOME" docker compose -f docker/network-tests/compose.yaml config
env USERPROFILE="$HOME" docker compose -f docker/network-tests/compose.yaml build
```

Run the network tests:

```bash
env USERPROFILE="$HOME" docker compose -f docker/network-tests/compose.yaml run --rm -T tests
```

The `env USERPROFILE="$HOME"` prefix supplies the cache-path variable only to that command. It does not change `$HOME` or persistently modify your shell environment. Compose reads it when resolving the host mount; it is not an HTTP client setting.

## Subsequent runs

Repeat the run command for your operating system, including the `env USERPROFILE="$HOME"` prefix on Linux/macOS. The service copies the current `pom.xml` and `src/` into the container on every execution, so Java source changes do not require rebuilding the image. Rebuild when the Dockerfile or image dependencies change.

## How the fault is produced

Each test starts a small HTTP server on `127.0.0.1`, using an automatically assigned port. Before the failing call, it installs an `iptables` rule that drops incoming TCP SYN packets on loopback for that port. The server consequently cannot complete the TCP handshake, and the client must wait for a timeout or cancellation.

Dropping packets produces a pending connection attempt. Connecting to a closed port would instead normally produce an immediate connection-refused error and would not exercise the same timeout behavior.

The rule is removed in a `finally` block before the recovery call. The tests then send another request using the same HTTP client and check the returned PDF bytes.

The Compose service uses `network_mode: none`: filtering applies inside the container's own network namespace. It publishes no ports. The source repository is mounted read-only, and compilation happens in a container-local copy. The Maven cache is mounted writable so Maven can maintain its repository metadata.

## Coverage

| Test | Configuration | Assertions |
|---|---|---|
| `connectTimeoutStopsBlockedTcpAndClientRecovers` | Connect timeout of `250` ms, then `500` ms; request deadline of `5000` ms. | The blocked connection returns `PDFE_902` with a connection-timeout diagnostic, no POST reaches the server, and no PDF paths are returned. Once filtering is removed, the same client successfully downloads the expected PDF bytes. |
| `requestDeadlineStopsBlockedTcpBeforeConnectTimeout` | Connect timeout of `10000` ms; request deadline of `700` ms. | The overall deadline cancels the blocked connection before the connect timeout, reports expiry of the `700` ms budget, and returns no PDF paths. Once filtering is removed, the same client successfully downloads the expected PDF bytes. |

The first test covers two timeout values, so the class has two test methods and three failure/recovery scenarios.

Timing assertions allow scheduling and setup overhead: connect cases accept durations from the configured timeout minus `50` ms up to, but excluding, the configured timeout plus `1000` ms; the deadline case accepts `600` ms up to, but excluding, `2000` ms.

## Results and reports

A complete successful run reports:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The log also includes timing lines such as:

```text
Connect timeout=250 ms; generatePDF duration=455 ms
Connect timeout=500 ms; generatePDF duration=502 ms
Request deadline=700 ms; connect timeout=10000 ms; generatePDF duration=702 ms
```

These durations measure the entire `generatePDF()` invocation, including request preparation. They exclude firewall rule installation and removal. They are not isolated measurements of the TCP handshake and can vary between runs.

Surefire XML and text reports are written to `target/network-test-reports/` on the host and remain available after `--rm` removes the container. Ordinary Java test reports remain under `target/surefire-reports/`. A later `mvn clean` removes both directories.

## Troubleshooting

| Symptom | Action |
|---|---|
| Compose cannot resolve the cache mount or the mounted cache is empty. | Check the resolved configuration and actual host Maven repository path. On Linux/macOS, use the documented `env USERPROFILE="$HOME"` prefix for every Compose command. |
| Maven reports missing dependencies or plugins in offline mode. | Run a successful `mvn test` on the host using the repository mounted at `/maven-cache`, then rerun the laboratory. |
| `iptables` reports permission errors. | Check that Docker runs Linux containers and that the environment permits `NET_ADMIN` for the test container. `iptables` is installed in the image, so it does not need to be installed on the host. |
| The selected class is skipped. | Run through the supplied Compose service, which sets `PDF_ENGINE_NETWORK_TESTS=1`. |
| A timing assertion fails. | Inspect the reported duration and host/container resource contention before changing the timing thresholds. |

## Scope

These tests verify timeout and recovery mechanisms under a controlled TCP fault. They use a local HTTP fixture and a small synthetic PDF response; they do not run the Node renderer or PDF/A conversion.


## Docker references

- [Docker Engine on Linux](https://docs.docker.com/engine/install/)
- [Docker Desktop on macOS](https://docs.docker.com/desktop/setup/install/mac-install/)
- [Compose environment-variable interpolation](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)
