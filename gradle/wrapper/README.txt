gradle-wrapper.jar
==================

The gradle-wrapper.jar binary is stored in this repository.

This lets GitHub Actions and fresh clones bootstrap Gradle without Android
Studio or a local Gradle install.

Alternatively, if you have Gradle installed locally, run:
    gradle wrapper --gradle-version 8.9

The configured Gradle distribution is:
    https://services.gradle.org/distributions/gradle-8.9-bin.zip
