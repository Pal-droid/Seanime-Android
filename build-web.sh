#!/bin/bash
set -e
echo "============================================"
echo "Building web frontend for mobile"
echo "============================================"
cd ../seanime/seanime-web
# Use mobile configuration if it exists
if [ -f ".env.mobile" ]; then
    cp .env.mobile .env.local
    echo "Using .env.mobile configuration"
else
    echo "Warning: .env.mobile not found, using default configuration"
fi
# Install dependencies
echo "Installing dependencies..."
npm install
# Build
echo "Building frontend..."
npm run build
# Copy output to root web directory for go:embed
echo "Copying build to ../../seanime-android/web/"
rm -rf ../../seanime-android/web
cp -r out/ ../../seanime-android/web/
echo "✓ Web frontend built successfully"
echo "Output: ../../seanime-android/web/"
