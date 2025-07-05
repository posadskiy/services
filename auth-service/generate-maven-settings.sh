#!/bin/bash

# Generate Maven settings.xml with GitHub authentication
# Usage: ./generate-maven-settings.sh <github_username> <github_token>

GITHUB_USERNAME=$1
GITHUB_TOKEN=$2

if [ -z "$GITHUB_USERNAME" ] || [ -z "$GITHUB_TOKEN" ]; then
    echo "Usage: $0 <github_username> <github_token>"
    exit 1
fi

mkdir -p /root/.m2

cat > /root/.m2/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                            http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <name>GitHub posadskiy Apache Maven Packages</name>
          <url>https://maven.pkg.github.com/posadskiy/user-service</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>
</settings>
EOF

echo "Maven settings.xml generated successfully in /root/.m2/settings.xml" 