#!/bin/bash

echo "🔧 Starting lightweight web build..."

./gradlew --stop

./gradlew :shared:compileKotlinJs --no-daemon --max-workers=1 || exit 1

echo "✅ Web build completed successfully!"
echo "💡 Use './gradlew :shared:jsBrowserDevelopmentRun --no-daemon' to run"