# Getting Started

Ensure that you have access to the required team resources beforehand. If you
encounter a permission error, it is likely because you are missing appropriate
access. Join the `#github` Slack channel, click the lightning bolt in the
channel header, and select `Join DataBiosphere`. Ask for access to Google Groups
including `dsde-engineering`. A colleague on the team will also need to create a
Helm `datarepo` definition for you.

These instructions assume you use MacOS, and that you are on the internal Broad
network or the VPN. If the VPN is not installed, follow the instructions
[at this link](https://broad.io/vpn).

> During this process, you will need your GitHub and Docker Hub username,
password, and personal access token for multiple steps, so make sure to have
those handy. If you don't have those yet, see the section below, otherwise you
can skip to [Connect Accounts](#2-connect-accounts)

## 1. Create a GitHub and Docker Hub account

GitHub is where the Broad stores our code and projects. Docker Hub allows the
development team to easily deploy software without having to install lots of
dependencies.

Sign up to these services with your **personal** email:
  * https://github.com/join
  * https://hub.docker.com/signup

Create a [personal access token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line)
so you can interact with GitHub on the command line.

## 2. Connect accounts

> Make sure 2-factor authentication (2FA) is activated on your
[Broad](https://broad.io/2fa) and [GitHub](https://github.com/settings/security)
account before starting this process!

Connect your GitHub account to your Broad profile:

1. Go to [Broad people](https://people.broadinstitute.org/me) and select the
**My Profile** tab.
2. Link your profile to GitHub by clicking under **Other Profiles**.
3. [Check if the account is successfully linked](https://github.broadinstitute.org/).
4. Open each of the following GitHub groups and **Request to join** by going to 
the Members tab: [Broad Institute Read](https://github.com/orgs/broadinstitute/teams/broad-institute-read),
[Prometheus](https://github.com/orgs/broadinstitute/teams/prometheus),
[DSDE Engineering](https://github.com/orgs/broadinstitute/teams/dsde-engineering)
5. To avoid being overwhelmed with notifications, [add your Broad email address](https://github.com/settings/emails),
[route the notifications](https://github.com/settings/notifications) to that
email, and [unfollow projects](https://github.com/watching) that are not
relevant to your team.

Connect your Docker Hub account to your Broad profile by contacting the DevOps
team.

## 3. Install Homebrew

[Homebrew](https://brew.sh/) is a [package manager](https://en.wikipedia.org/wiki/Package_manager)
which enables the installation of software using a single, convenient command
line interface:

```
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

Once Homebrew is installed, there are a number of useful development tools that
should be installed.

1. [Git](https://git-scm.com/) is a version control tool for tracking changes in
projects and code. To ensure that secrets and passwords cannot accidentally be
committed to repositories, [git-secrets](https://github.com/awslabs/git-secrets)
is also installed:

```
brew install git
brew install git-secrets
git clone https://github.com/broadinstitute/dsp-appsec-gitsecrets-client.git gitsecrets
sh gitsecrets/gitsecrets.sh
```

2. [jq](https://stedolan.github.io/jq/) is a command line JSON processing tool:

```
brew install jq
```

3. [Docker](https://www.docker.com/) is a tool to deliver software in packages
called containers. Docker for MacOS also includes [Kubernetes](https://kubernetes.io/),
which deploys groups of containers together in clusters. Once installed, you'll
need to run Docker once from your list of Applications:

```
brew cask install docker
open -a Docker
```

4. [Helm](https://helm.sh/) streamlines the process of defining, installing, and
upgrading Kubernetes deployments, which are otherwise challenging to manage:

```
brew install helm
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo add datarepo-helm https://broadinstitute.github.io/datarepo-helm
helm plugin install https://github.com/thomastaylor312/helm-namespace
helm repo update
```

5. [Skaffold](https://github.com/GoogleContainerTools/skaffold) facilitates the
continuous development of Kubernetes resources. Newer versions are incompatible
with our development environments, so version 1.3.1 is installed instead.

```
curl https://raw.githubusercontent.com/Homebrew/homebrew-core/5db9ede616f5d681fa9873b150416d6795e0e0e9/Formula/skaffold.rb --output skaffold.rb
brew install skaffold.rb
brew pin skaffold
```

6. [Vault](https://www.vaultproject.io/) is an encrypted database used to store
many of the team's secrets such as keys and passwords:

```
brew install vault
export VAULT_ADDR=https://clotho.broadinstitute.org:8200
```

7. Much of the code written at the Broad is in [Java](https://en.wikipedia.org/wiki/Java_(programming_language)).
Install the OpenJDK 8 runtime from the [AdoptOpenJDK](https://adoptopenjdk.net/)
project to develop and run Java code:

```
brew tap AdoptOpenJDK/openjdk
brew cask install adoptopenjdk8
```

8. [Google Cloud SDK](https://cloud.google.com/sdk) is a command-line interface
to Google Cloud services. Once it is installed, you'll need to allow auth access
and configure Docker to connect to the appropriate Google Cloud endpoint when
necessary:

```
brew cask install google-cloud-sdk
gcloud auth login
gcloud auth application-default login
gcloud auth configure-docker
```

9. [IntelliJ IDEA](https://www.jetbrains.com/idea/) is an integrated development
environment (IDE) for Java. There are two versions available: **Ultimate** (paid)
and **Community** (open-source). The **Community** edition has all the features
needed for development:

```
brew cask install intellij-idea-ce
open -a "IntelliJ IDEA CE"
```

10. Once it is launched, go to IntelliJ IDEA -> Preferences -> Plugins,
then click in the search box and install **Cloud Code**, which integrates
Google Cloud features with IntelliJ IDEA.

## 4. Create GitHub token

The GitHub token verifies team permissions. This token is necessary for the next
step, [Login to Vault](#6-login-to-vault). To create a token:

1. Go to the [GitHub Personal Access Token](https://github.com/settings/tokens)
page and click **Generate new token**.
2. Give the token a descriptive name, **only** give it the `read:org` scope
under `admin:org`, and click **Generate token**.
3. Store this token in a file:

```
GH_VAULT_TOKEN=<<GITHUB TOKEN VALUE>>
echo $GH_VAULT_TOKEN > ~/.gh_token
```

## 5. Login to Vault

Vault access tokens can be obtained using the GitHub token from earlier as
follows:

```
vault login -method=github token=$(cat ~/.gh_token)
```

## 6. Code Checkout

> It may be useful to create a folder for Broad projects in your home directory.

Download the team's projects:

```
git clone https://github.com/DataBiosphere/jade-data-repo
git clone https://github.com/DataBiosphere/jade-data-repo-ui
git clone https://github.com/DataBiosphere/jade-data-repo-cli
git clone https://github.com/broadinstitute/terraform-jade
git clone https://github.com/broadinstitute/datarepo-helm
git clone https://github.com/broadinstitute/datarepo-helm-definitions
```

## 7. Google Cloud Platform setup

1. Log in to [Google Cloud Platform](https://console.cloud.google.com). In the
top-left corner, select the **BROADINSTITUTE.ORG** organization. Select
**broad-jade-dev** from the list of projects.
2. From the left hand sidebar, select **Kubernetes Engine -> Clusters** under
**COMPUTE**.
3. Click **Connect** on the **dev-master** cluster. This gives you a `kubectl`
command to copy and paste into the terminal:

```
gcloud container clusters get-credentials dev-master --region us-central1 --project broad-jade-dev
```

4. Starting from your [project directory](#7-code-checkout), bring up Helm services:

```
# replace all instances of `zzz` with your initials
cd datarepo-helm-definitions/dev/zzz
helm namespace upgrade zzz-secrets datarepo-helm/create-secret-manager-secret --version=0.0.4 --install --namespace zzz -f zzzSecrets.yaml
helm namespace upgrade zzz-jade datarepo-helm/datarepo --version=0.1.0 --install --namespace zzz -f zzzDeployment.yaml
```

## 8. Install Postgres 12

[Postgres](https://www.postgresql.org/) is an advanced open-source database.
**Postgres.app** is used to manage a local installation of Postgres. The latest
release can be found on the [GitHub releases](https://github.com/PostgresApp/PostgresApp/releases)
page. For compatibility, make sure to select a version which supports all the
older versions of Postgres including 9.6. After launching the application,
create a new version 12 database as follows:

1. Click the sidebar icon (bottom left-hand corner) and then click the plus sign
2. Name the new server, making sure to select version **12**, and then
**Initialize** it
3. Add `/Applications/Postgres.app/Contents/Versions/latest/bin` to your path
(there are multiple ways to achieve this)
4. Switch to the `jade-data-repo` repository, and create the data repo database
and user following the [database readme](https://github.com/DataBiosphere/jade-data-repo/blob/develop/DATABASE.md):

```
psql -f db/create-data-repo-db
# verify that the `datarepo` and `stairway` databases exist
psql --list
```

## 9. Repository Setup

### 1. Build `jade-data-repo`

Follow the [Build and Run Locally](https://github.com/DataBiosphere/jade-data-repo#build-and-run-locally)
section in the [main readme](https://github.com/DataBiosphere/jade-data-repo#jade-data-repository---)
to build `jade-data-repo`.

* You will need to run `render-configs.sh` before running integration tests.

* Certain environment variables need to be set beforehand. Instances of `zzz`
should be replaced by your initials or the environment (i.e. `dev`):

```
export VAULT_ADDR=https://clotho.broadinstitute.org:8200
export PROXY_URL=https://jade-zzz.datarepo-dev.broadinstitute.org
export GOOGLE_CLOUD_PROJECT=broad-jade-zzz
export GOOGLE_APPLICATION_CREDENTIALS=/tmp/jade-dev-account.json
export GOOGLE_SA_CERT=/tmp/jade-dev-account.pem
```

* If you're not on a Broad computer, you may need to set the host to `localhost`
instead of `http://local.broadinstitute.org`:

```
export HOST=localhost
```

* Build the code and run the tests:

```
./gradlew bootRun         # build jade-data-repo with Spring Boot features
./gradlew check           # linters and unit tests
./gradlew testConnected   # connected tests
./gradlew testIntegrated  # integration tests
```

* The first run of the integration tests should create a corresponding Google
Cloud Project with the name `broad-jade-zzz-data`, where `zzz` is replaced by
your initials. After this is created, Firestore needs to be enabled:
  1. Go to the [Google Cloud Console](http://console.cloud.google.com/).
  2. From the `DATA.TEST-TERRA.BIO` organization, select your newly created GCP
  project: `broad-jade-zzz-data`.
  3. Go to Firestore and enable firestore in **Native** mode.

### 2. Build `jade-data-repo-ui`

Follow the [setup instructions](https://github.com/DataBiosphere/jade-data-repo-ui#jade-data-repository-ui)
to build the `jade-data-repo-ui` repository.

### 3. Initialize `terraform-jade`

Follow the [setup instructions](https://github.com/broadinstitute/terraform-jade#terraform-jade)
to initialize the `terraform-jade` repository.

## Common Issues

Ensure that:

1. You are on the Broad Non-split VPN. See earlier [instructions](#-getting-started).
2. Docker is running.
3. Postgres database is started.
4. Environment variables are set. Instances of `zzz` should be replaced by your
initials or the environment (i.e. `dev`). You may need to set some or all of the
environment variables below depending on what you are trying to run:

```
export NVM_DIR="$HOME/.nvm"
export VAULT_ADDR=https://clotho.broadinstitute.org:8200
export ENVIRONMENT=dev
export GOOGLE_CLOUD_PROJECT=broad-jade-zzz
export SUFFIX=zzz
export STEWARD_ACCT={YOUREMAIL}@{whatever}.com
export PROXY_URL=https://jade-zzz.datarepo-dev.broadinstitute.org
export CYPRESS_BASE_URL=http://local.broadinstitute.org:3000
# Only need to do this if you're not using a Broad computer - otherwise host
# would be http://local.broadinstitute.org
export HOST=localhost
```
