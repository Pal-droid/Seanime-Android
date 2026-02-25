#!/bin/bash
set -e

echo "============================================"
echo "Building web frontend for mobile"
echo "============================================"

# Navigate to the frontend source in the sibling repo
# Structure: seanime-project/seanime/seanime-web
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

# We need to copy the 'out' folder to 'seanime/web' 
# so 'go:embed' can find it during the Go build process.
echo "Copying build to ../web/ (Seanime Go root)"
rm -rf ../web
cp -r out/ ../web/

# Also keeping a copy in the android folder just in case
echo "Copying build to ../../seanime-android/web/"
rm -rf ../../seanime-android/web
cp -r out/ ../../seanime-android/web/

echo "✓ Web frontend built successfully"
echo "Output: ../web/"
