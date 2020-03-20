# Machina Tools for Google Cloud Storage

The Machina Tools for Google Cloud Storage integration offers a simple way for developers building atop Google's Java SDK to invoke Ionic's protection and policy functionality as data moves to and from the cloud storage. This addresses use cases such as migration from on-prem storage solutions, protecting data across multi-region or multi-cloud environments, applying granular cryptographic control, and more.

# Requirements
Java 8 or later.
Maven 3.0.0 or later.

# Installation
This library can be installed to local maven cache with the command ```mvn install``` executed from the repository root.

# Setup
Using the library requires an Google Cloud Storage Account and an Ionic Enrollment. Consult the [Setup Guide](https://dev.ionic.com/integrations/idts-gcs-sdk/tasks/setup) for details.

# Documentation
An overview of the library can be found [here](https://dev.ionic.com/integrations/idts-gcs-sdk/) as well as breakdown of the [Sample Application](https://dev.ionic.com/integrations/idts-gcs-sdk/tasks/sample-cli).
Hosted Javadocs are available at https://dev.ionic.com/sdk_docs/ionic_gcs_sdk/java/version_1.1.0/index.html.
Alternatively they can be generated locally with the command ```mvn javadoc:javadoc``` and accessed from 'target/site/apidocs/index.html'.
