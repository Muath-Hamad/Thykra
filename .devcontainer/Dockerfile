# Thykra dev container
# Base: Eclipse Temurin JDK 17 (compiles JVM 11 target, supports Gradle 8.14)
FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/home/dev/.gradle \
    NODE_VERSION=20.x \
    # Tell Claude Code we trust this isolated sandbox
    IS_SANDBOX=1

# --- System packages --------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
        git curl ca-certificates gnupg unzip zip xz-utils \
        build-essential python3 python3-pip \
        sudo less vim nano openssh-client \
    && rm -rf /var/lib/apt/lists/*

# --- Node 20 + npm ----------------------------------------------------------
RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION} | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/* \
    && npm install -g npm@latest

# --- Android SDK (commandline-tools + platform 34 + build-tools) ------------
ARG CMDLINE_TOOLS_VERSION=11076708
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o /tmp/cmdtools.zip \
    && unzip -q /tmp/cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdtools.zip

ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Accept licenses, then install each package in its own retry-wrapped layer.
# Android SDK downloads are flaky: a single corrupted zip used to nuke the
# entire `sdkmanager --install` call and lose 10+ minutes of progress. Per-package
# layers + 3-attempt retry mean only the affected package retries.
RUN yes | sdkmanager --licenses > /dev/null

RUN set -eux; \
    retry() { for i in 1 2 3; do "$@" && return 0 || { echo "attempt $i failed, retrying in 15s..."; sleep 15; }; done; return 1; }; \
    retry sdkmanager --install "platform-tools"

RUN set -eux; \
    retry() { for i in 1 2 3; do "$@" && return 0 || { echo "attempt $i failed, retrying in 15s..."; sleep 15; }; done; return 1; }; \
    retry sdkmanager --install "platforms;android-36"

RUN set -eux; \
    retry() { for i in 1 2 3; do "$@" && return 0 || { echo "attempt $i failed, retrying in 15s..."; sleep 15; }; done; return 1; }; \
    retry sdkmanager --install "build-tools;35.0.0"

RUN chmod -R a+rwX ${ANDROID_HOME}

# --- Claude Code CLI --------------------------------------------------------
RUN npm install -g @anthropic-ai/claude-code

# --- Non-root dev user ------------------------------------------------------
ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=1000
RUN groupadd --gid ${USER_GID} ${USERNAME} \
    && useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME} \
    && echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers.d/${USERNAME} \
    && chmod 0440 /etc/sudoers.d/${USERNAME} \
    && mkdir -p /home/${USERNAME}/.gradle /home/${USERNAME}/.claude /home/${USERNAME}/.android \
    && chown -R ${USER_UID}:${USER_GID} /home/${USERNAME} ${ANDROID_HOME}

USER ${USERNAME}
WORKDIR /workspace

# Wrapper that always launches Claude Code with safety prompts disabled.
# Safe BECAUSE the entire dev environment is an isolated container.
RUN printf '#!/usr/bin/env bash\nexec /usr/bin/claude --dangerously-skip-permissions "$@"\n' \
        | sudo tee /usr/local/bin/cc > /dev/null \
    && sudo chmod +x /usr/local/bin/cc

EXPOSE 8080 8081 5005

CMD ["bash"]
