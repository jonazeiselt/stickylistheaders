#!/bin/bash

buildGradle=stickylistheaders/build.gradle

bumpMajor() {
    bumpVersion 0
}

bumpMinor() {
    bumpVersion 1
}

bumpPatch() {
    bumpVersion 2
}

bumpVersion() {
    local index=$1
    # shellcheck disable=SC2155
    local versionCode=$(grep "versionCode" $buildGradle | awk '{print $2}')
    # shellcheck disable=SC2155
    local versionName=$(grep "versionName" $buildGradle | awk -F '"' '{print $2}')
    # shellcheck disable=SC2155
    local publishingVersion=$(grep "version '" $buildGradle | tr -d "'" | awk '{print $2}')

    echo ">> Version code: $versionCode"
    echo ">> Version name: $versionName"
    echo ">> Publishing version: $publishingVersion"

    IFS='.' read -ra semanticVersioning <<< "$versionName"

    if [[ ${#semanticVersioning[@]} != 3 ]]; then
        echo "Could not parse version name ($versionName)"
        exit 1
    fi

    local semanticDigit=${semanticVersioning[$index]}
    semanticVersioning[$index]=$((semanticDigit + 1))

    if [[ $index -eq 0 ]]; then
        semanticVersioning[1]=0
    fi

    if [[ $index -le 1 ]]; then
        semanticVersioning[2]=0
    fi

    local newVersionCode=$((versionCode + 1))
    local newVersionName="${semanticVersioning[0]}.${semanticVersioning[1]}.${semanticVersioning[2]}"

    echo ">> New version code: $newVersionCode"
    echo ">> New version name: $newVersionName"

    sed -i '' "s/versionCode $versionCode/versionCode $newVersionCode/" $buildGradle
    sed -i '' "s/versionName \"$versionName\"/versionName \"$newVersionName\"/" $buildGradle
    sed -i '' "s/version '$publishingVersion'/version '$newVersionName'/" $buildGradle

    echo ">> Updated gradle file.."

    git add $buildGradle
    git commit -m "chore: bump version"
    git push origin

    git tag "$newVersionName"
    git push origin "$newVersionName"
}

# Main logic
# Example: ./bump_version.sh major
case $1 in
    major)
        bumpMajor
        ;;
    minor)
        bumpMinor
        ;;
    patch)
        bumpPatch
        ;;
    *)
        echo "Invalid option: specify major, minor, or patch"
        ;;
esac
